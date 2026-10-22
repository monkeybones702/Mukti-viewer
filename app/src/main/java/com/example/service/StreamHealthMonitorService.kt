package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.R
import com.example.data.db.AppDatabase
import com.example.data.model.FeedHealthStatus
import com.example.data.repository.CloudSyncRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

data class FeedHeartbeatSnapshot(
    val windowId: String,
    val windowTitle: String,
    val streamUrl: String,
    val lastBeatEpochMs: Long,
    val lastFrameProgressSignature: String,
    val isNetworkConnected: Boolean,
    val status: FeedHealthStatus,
    val softRefreshAttempts: Int = 0,
    val lastLatencyMs: Int = 42,
    val lastDiagnosis: String = "Active stream heartbeat OK"
)

data class SoftRefreshCommand(
    val windowId: String,
    val windowTitle: String,
    val streamUrl: String,
    val reason: String,
    val attemptNumber: Int,
    val timestampMs: Long = System.currentTimeMillis()
)

data class HealthMonitorSummary(
    val isServiceRunning: Boolean = true,
    val isAutoSoftRefreshEnabled: Boolean = true,
    val freezeThresholdSec: Int = 12,
    val checkIntervalSec: Int = 4,
    val healthyCount: Int = 0,
    val frozenOrDroppedCount: Int = 0,
    val totalAutoRecoveries: Int = 0,
    val lastScanTimestampMs: Long = 0L,
    val feedSnapshots: Map<String, FeedHeartbeatSnapshot> = emptyMap()
)

object StreamHealthMonitorBus {
    private val heartbeats = ConcurrentHashMap<String, FeedHeartbeatSnapshot>()

    private val _summary = MutableStateFlow(HealthMonitorSummary())
    val summary: StateFlow<HealthMonitorSummary> = _summary.asStateFlow()

    private val _softRefreshCommands = MutableSharedFlow<SoftRefreshCommand>(extraBufferCapacity = 32)
    val softRefreshCommands: SharedFlow<SoftRefreshCommand> = _softRefreshCommands.asSharedFlow()

    fun reportHeartbeat(
        windowId: String,
        windowTitle: String,
        streamUrl: String,
        frameSignature: String = "frame_ok",
        isConnected: Boolean = true,
        latencyMs: Int = 38
    ) {
        val now = System.currentTimeMillis()
        val existing = heartbeats[windowId]
        val nextStatus = if (!isConnected) {
            FeedHealthStatus.DROPPED
        } else if (existing?.status == FeedHealthStatus.RECOVERING) {
            FeedHealthStatus.RECOVERED
        } else {
            FeedHealthStatus.HEALTHY
        }
        val snapshot = FeedHeartbeatSnapshot(
            windowId = windowId,
            windowTitle = windowTitle,
            streamUrl = streamUrl,
            lastBeatEpochMs = now,
            lastFrameProgressSignature = frameSignature,
            isNetworkConnected = isConnected,
            status = nextStatus,
            softRefreshAttempts = existing?.softRefreshAttempts ?: 0,
            lastLatencyMs = latencyMs.coerceIn(5, 60000),
            lastDiagnosis = if (isConnected) "Stream DOM & video frame heartbeat active" else "Connection dropped"
        )
        heartbeats[windowId] = snapshot
        publishSummary()
    }

    fun reportStreamFreezeOrDrop(
        windowId: String,
        windowTitle: String,
        streamUrl: String,
        isDropped: Boolean,
        reason: String
    ) {
        val existing = heartbeats[windowId]
        val staleEpoch = System.currentTimeMillis() - (_summary.value.freezeThresholdSec * 1000L + 3000L)
        val status = if (isDropped) FeedHealthStatus.DROPPED else FeedHealthStatus.FROZEN
        heartbeats[windowId] = FeedHeartbeatSnapshot(
            windowId = windowId,
            windowTitle = windowTitle,
            streamUrl = streamUrl,
            lastBeatEpochMs = staleEpoch,
            lastFrameProgressSignature = "stalled",
            isNetworkConnected = !isDropped,
            status = status,
            softRefreshAttempts = existing?.softRefreshAttempts ?: 0,
            lastLatencyMs = if (isDropped) 9999 else 4500,
            lastDiagnosis = reason
        )
        publishSummary()
    }

    fun unregisterWindow(windowId: String) {
        heartbeats.remove(windowId)
        publishSummary()
    }

    fun setServiceRunning(running: Boolean) {
        _summary.value = _summary.value.copy(isServiceRunning = running)
    }

    fun setAutoSoftRefreshEnabled(enabled: Boolean) {
        _summary.value = _summary.value.copy(isAutoSoftRefreshEnabled = enabled)
    }

    fun setFreezeThresholdSec(seconds: Int) {
        _summary.value = _summary.value.copy(freezeThresholdSec = seconds.coerceIn(5, 60))
    }

    fun evaluateFeedHealthAndTriggerSoftRefresh(
        nowMs: Long = System.currentTimeMillis()
    ): List<SoftRefreshCommand> {
        val currentSummary = _summary.value
        if (!currentSummary.isServiceRunning) return emptyList()

        val thresholdMs = currentSummary.freezeThresholdSec * 1000L
        val triggeredCommands = mutableListOf<SoftRefreshCommand>()

        for ((windowId, snap) in heartbeats.entries) {
            val elapsedMs = nowMs - snap.lastBeatEpochMs
            val isFrozenOrDropped = !snap.isNetworkConnected ||
                snap.status == FeedHealthStatus.FROZEN ||
                snap.status == FeedHealthStatus.DROPPED ||
                elapsedMs > thresholdMs

            if (isFrozenOrDropped) {
                val diagnosis = when {
                    !snap.isNetworkConnected || snap.status == FeedHealthStatus.DROPPED ->
                        "Stream dropped connection or HTTP/WebRTC socket stalled"
                    snap.lastDiagnosis.isNotBlank() && snap.status == FeedHealthStatus.FROZEN ->
                        snap.lastDiagnosis
                    else ->
                        "Emulator feed heartbeat frozen for ${elapsedMs / 1000}s (> ${currentSummary.freezeThresholdSec}s threshold)"
                }

                if (currentSummary.isAutoSoftRefreshEnabled) {
                    val nextAttempt = snap.softRefreshAttempts + 1
                    val recoveringSnap = snap.copy(
                        lastBeatEpochMs = nowMs,
                        status = FeedHealthStatus.RECOVERING,
                        softRefreshAttempts = nextAttempt,
                        lastDiagnosis = "Auto soft-refresh #${nextAttempt}: $diagnosis"
                    )
                    heartbeats[windowId] = recoveringSnap
                    val cmd = SoftRefreshCommand(
                        windowId = windowId,
                        windowTitle = snap.windowTitle,
                        streamUrl = snap.streamUrl,
                        reason = diagnosis,
                        attemptNumber = nextAttempt,
                        timestampMs = nowMs
                    )
                    triggeredCommands.add(cmd)
                    _softRefreshCommands.tryEmit(cmd)
                } else {
                    heartbeats[windowId] = snap.copy(
                        status = if (!snap.isNetworkConnected) FeedHealthStatus.DROPPED else FeedHealthStatus.FROZEN,
                        lastDiagnosis = diagnosis
                    )
                }
            }
        }

        val addedRecoveries = triggeredCommands.size
        publishSummary(nowMs, addedRecoveries)
        return triggeredCommands
    }

    fun clearForTesting() {
        heartbeats.clear()
        _summary.value = HealthMonitorSummary()
    }

    private fun publishSummary(
        scanTimestampMs: Long = _summary.value.lastScanTimestampMs,
        addedRecoveries: Int = 0
    ) {
        val snapshotsMap = heartbeats.toMap()
        val healthy = snapshotsMap.values.count {
            it.status == FeedHealthStatus.HEALTHY || it.status == FeedHealthStatus.RECOVERED
        }
        val unhealthy = snapshotsMap.values.count {
            it.status == FeedHealthStatus.FROZEN ||
                it.status == FeedHealthStatus.DROPPED ||
                it.status == FeedHealthStatus.RECOVERING
        }
        _summary.value = _summary.value.copy(
            healthyCount = healthy,
            frozenOrDroppedCount = unhealthy,
            totalAutoRecoveries = _summary.value.totalAutoRecoveries + addedRecoveries,
            lastScanTimestampMs = if (scanTimestampMs > 0L) scanTimestampMs else System.currentTimeMillis(),
            feedSnapshots = snapshotsMap
        )
    }
}

class StreamHealthMonitorService : Service() {

    inner class LocalBinder : Binder() {
        fun getService(): StreamHealthMonitorService = this@StreamHealthMonitorService
    }

    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var monitorJob: Job? = null

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForegroundSafely()
        StreamHealthMonitorBus.setServiceRunning(true)
        startWatchdogLoop()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_MONITOR -> {
                StreamHealthMonitorBus.setServiceRunning(false)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_FORCE_SCAN -> {
                serviceScope.launch {
                    runSingleHealthCheckPass()
                }
            }
            else -> {
                StreamHealthMonitorBus.setServiceRunning(true)
                if (monitorJob?.isActive != true) {
                    startWatchdogLoop()
                }
            }
        }
        return START_STICKY
    }

    private fun startWatchdogLoop() {
        monitorJob?.cancel()
        monitorJob = serviceScope.launch {
            val db = AppDatabase.getDatabase(applicationContext)
            val dao = db.streamWindowDao()

            // Seed initial windows into the health bus if not yet registered
            runCatching {
                val windows = dao.getAllWindowsSync()
                val currentMap = StreamHealthMonitorBus.summary.value.feedSnapshots
                windows.forEach { win ->
                    if (!currentMap.containsKey(win.id)) {
                        StreamHealthMonitorBus.reportHeartbeat(
                            windowId = win.id,
                            windowTitle = win.title,
                            streamUrl = win.url,
                            frameSignature = "init_seed",
                            isConnected = true
                        )
                    }
                }
            }

            while (isActive && StreamHealthMonitorBus.summary.value.isServiceRunning) {
                val intervalMs = (StreamHealthMonitorBus.summary.value.checkIntervalSec.coerceIn(2, 30)) * 1000L
                delay(intervalMs)
                runSingleHealthCheckPass()
            }
        }
    }

    private suspend fun runSingleHealthCheckPass() {
        val now = System.currentTimeMillis()
        val commands = StreamHealthMonitorBus.evaluateFeedHealthAndTriggerSoftRefresh(now)
        if (commands.isEmpty()) return

        val roomDao = AppDatabase.getDatabase(applicationContext).streamWindowDao()
        val databaseId = runCatching {
            applicationContext.getString(R.string.firestore_database_id)
        }.getOrNull()

        val cloudRepo = if (!databaseId.isNullOrBlank() && Firebase.auth.currentUser != null) {
            runCatching {
                CloudSyncRepository(FirebaseFirestore.getInstance(databaseId))
            }.getOrNull()
        } else {
            null
        }

        for (cmd in commands) {
            runCatching {
                roomDao.recordSoftRefresh(
                    id = cmd.windowId,
                    status = FeedHealthStatus.RECOVERING.name,
                    heartbeatMs = now,
                    reason = cmd.reason
                )
            }
            cloudRepo?.logHealthIncident(
                windowId = cmd.windowId,
                windowTitle = cmd.windowTitle,
                streamUrl = cmd.streamUrl,
                status = FeedHealthStatus.RECOVERED.name,
                reason = cmd.reason,
                recoveryAction = "Automatic WebView Soft-Refresh (Attempt #${cmd.attemptNumber})",
                attemptCount = cmd.attemptNumber,
                latencyMs = 120
            )
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.health_monitor_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.health_monitor_channel_desc)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun startForegroundSafely() {
        runCatching {
            val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(getString(R.string.health_monitor_channel_name))
                .setContentText(getString(R.string.health_monitor_channel_desc))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .build()
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    override fun onDestroy() {
        monitorJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_ID = "streamsy_health_monitor_channel"
        const val NOTIFICATION_ID = 4401
        const val ACTION_START_MONITOR = "com.example.service.action.START_MONITOR"
        const val ACTION_STOP_MONITOR = "com.example.service.action.STOP_MONITOR"
        const val ACTION_FORCE_SCAN = "com.example.service.action.FORCE_SCAN"

        fun startService(context: Context) {
            runCatching {
                val intent = Intent(context, StreamHealthMonitorService::class.java).apply {
                    action = ACTION_START_MONITOR
                }
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            runCatching {
                val intent = Intent(context, StreamHealthMonitorService::class.java).apply {
                    action = ACTION_STOP_MONITOR
                }
                context.stopService(intent)
            }
        }

        fun triggerImmediateScan(context: Context) {
            runCatching {
                val intent = Intent(context, StreamHealthMonitorService::class.java).apply {
                    action = ACTION_FORCE_SCAN
                }
                context.startService(intent)
            }
        }
    }
}
