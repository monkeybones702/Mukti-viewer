package com.example.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.ai.ChatbotRolePersona
import com.example.ai.GeminiCommandService
import com.example.ai.GeminiTaskTier
import com.example.data.db.AppDatabase
import com.example.data.model.CloudChatMessage
import com.example.data.model.FeedHealthStatus
import com.example.data.model.FeedType
import com.example.data.model.GridPreset
import com.example.data.model.LoginState
import com.example.data.model.PresetJsonSerializer
import com.example.data.model.StreamHealthIncident
import com.example.data.model.StreamWindow
import com.example.data.repository.CloudSyncRepository
import com.example.data.repository.StreamRepository
import com.example.service.HealthMonitorSummary
import com.example.service.StreamHealthMonitorBus
import com.example.service.StreamHealthMonitorService
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface StreamUiEvent {
    data class RefreshWindow(val windowId: String, val isSoftRecovery: Boolean = false) : StreamUiEvent
    data object RefreshAllWindows : StreamUiEvent
    data class ApplyWindowZoom(
        val windowId: String,
        val cols: Int,
        val rows: Int,
        val fitPhone: Boolean,
        val proportional: Boolean
    ) : StreamUiEvent
    data class ApplyAllWindowsZoom(
        val cols: Int,
        val rows: Int,
        val fitPhone: Boolean,
        val proportional: Boolean
    ) : StreamUiEvent
    data class ApplyWindowAudioMute(val windowId: String, val isMuted: Boolean) : StreamUiEvent
    data class ApplyAllAudioMute(val isMuted: Boolean) : StreamUiEvent
    data class ShowSnackbar(val message: String) : StreamUiEvent
}

class MultiStreamViewModel(
    application: Application,
    private val currentUserId: String
) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = StreamRepository(
        dao = database.streamWindowDao(),
        presetDao = database.gridPresetDao()
    )

    // Mandatory custom database ID from firebase_applet_config.xml
    private val databaseId: String = application.getString(R.string.firestore_database_id)
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(databaseId)
    private val cloudRepository = CloudSyncRepository(firestore)

    val windows: StateFlow<List<StreamWindow>> = repository.allWindows
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    val presets: StateFlow<List<GridPreset>> = (repository.allPresets ?: MutableStateFlow(emptyList()))
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = PresetJsonSerializer.getBuiltInPresets()
        )

    val healthMonitorSummary: StateFlow<HealthMonitorSummary> = StreamHealthMonitorBus.summary

    val cloudHealthIncidents: StateFlow<List<StreamHealthIncident>> =
        cloudRepository.observeHealthIncidents(currentUserId)
            .catch { error ->
                Log.w(TAG, "Error observing health incidents", error)
                emit(emptyList())
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = emptyList()
            )

    val cloudChatMessages: StateFlow<List<CloudChatMessage>> =
        cloudRepository.observeChatMessages(currentUserId)
            .catch { error ->
                Log.w(TAG, "Error observing cloud chat messages", error)
                emit(emptyList())
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = emptyList()
            )

    private val _gridMode = MutableStateFlow(2)
    val gridMode: StateFlow<Int> = _gridMode.asStateFlow()

    private val _gridRows = MutableStateFlow(2)
    val gridRows: StateFlow<Int> = _gridRows.asStateFlow()

    private val _fitEntirePhoneScreen = MutableStateFlow(true)
    val fitEntirePhoneScreen: StateFlow<Boolean> = _fitEntirePhoneScreen.asStateFlow()

    private val _proportionalZoom = MutableStateFlow(false)
    val proportionalZoom: StateFlow<Boolean> = _proportionalZoom.asStateFlow()

    private val _masterAudioMuted = MutableStateFlow(true)
    val masterAudioMuted: StateFlow<Boolean> = _masterAudioMuted.asStateFlow()

    private val _activePresetId = MutableStateFlow<String?>("builtin_4_window_focus")
    val activePresetId: StateFlow<String?> = _activePresetId.asStateFlow()

    private val _maximizedWindowId = MutableStateFlow<String?>(null)
    val maximizedWindowId: StateFlow<String?> = _maximizedWindowId.asStateFlow()

    private val _showAddDialog = MutableStateFlow(false)
    val showAddDialog: StateFlow<Boolean> = _showAddDialog.asStateFlow()

    private val _showPresetManagerDialog = MutableStateFlow(false)
    val showPresetManagerDialog: StateFlow<Boolean> = _showPresetManagerDialog.asStateFlow()

    private val _showHealthMonitorDialog = MutableStateFlow(false)
    val showHealthMonitorDialog: StateFlow<Boolean> = _showHealthMonitorDialog.asStateFlow()

    private val _showAiAssistantDialog = MutableStateFlow(false)
    val showAiAssistantDialog: StateFlow<Boolean> = _showAiAssistantDialog.asStateFlow()

    private val _zoomDialogTargetWindow = MutableStateFlow<StreamWindow?>(null)
    val zoomDialogTargetWindow: StateFlow<StreamWindow?> = _zoomDialogTargetWindow.asStateFlow()

    private val _showGlobalZoomDialog = MutableStateFlow(false)
    val showGlobalZoomDialog: StateFlow<Boolean> = _showGlobalZoomDialog.asStateFlow()

    private val _selectedGeminiTier = MutableStateFlow(GeminiTaskTier.GENERAL_GROUNDED)
    val selectedGeminiTier: StateFlow<GeminiTaskTier> = _selectedGeminiTier.asStateFlow()

    private val _selectedPersona = MutableStateFlow(ChatbotRolePersona.STREAM_OPS_COMMANDER)
    val selectedPersona: StateFlow<ChatbotRolePersona> = _selectedPersona.asStateFlow()

    private val _enableSearchGrounding = MutableStateFlow(true)
    val enableSearchGrounding: StateFlow<Boolean> = _enableSearchGrounding.asStateFlow()

    private val _isAiGenerating = MutableStateFlow(false)
    val isAiGenerating: StateFlow<Boolean> = _isAiGenerating.asStateFlow()

    private val _uiEvents = MutableSharedFlow<StreamUiEvent>(extraBufferCapacity = 32)
    val uiEvents: SharedFlow<StreamUiEvent> = _uiEvents.asSharedFlow()

    init {
        viewModelScope.launch {
            repository.ensureDefaultsInitialized()
            StreamHealthMonitorService.startService(application)
        }

        // Sync cloud presets down into local Room database so user presets persist across devices
        viewModelScope.launch {
            if (Firebase.auth.currentUser != null) {
                cloudRepository.observeCloudPresets(currentUserId)
                    .catch { e -> Log.w(TAG, "Cloud preset sync error", e) }
                    .collect { cloudList ->
                        cloudList.forEach { cloudPreset ->
                            repository.upsertPreset(
                                GridPreset(
                                    id = cloudPreset.id,
                                    name = cloudPreset.name,
                                    description = cloudPreset.description,
                                    gridColumns = cloudPreset.gridColumns,
                                    gridRows = cloudPreset.gridRows,
                                    fitEntirePhone = cloudPreset.fitEntirePhone,
                                    proportionalZoom = cloudPreset.proportionalZoom,
                                    masterAudioMuted = cloudPreset.masterAudioMuted,
                                    windowsJson = cloudPreset.windowsJson,
                                    isBuiltIn = false,
                                    createdAtMs = cloudPreset.createdAt?.toDate()?.time
                                        ?: System.currentTimeMillis()
                                )
                            )
                        }
                    }
            }
        }

        // Listen for background StreamHealthMonitorService auto-soft-refresh events
        viewModelScope.launch {
            StreamHealthMonitorBus.softRefreshCommands.collect { cmd ->
                _uiEvents.emit(StreamUiEvent.RefreshWindow(cmd.windowId, isSoftRecovery = true))
                _uiEvents.emit(
                    StreamUiEvent.ShowSnackbar(
                        "Auto Soft-Refresh: ${cmd.windowTitle} (${cmd.reason})"
                    )
                )
            }
        }
    }

    // --- Grid Layout & Zoom Controls ---

    fun setGridDimensions(cols: Int, rows: Int) {
        _gridColumns.value = cols.coerceIn(1, 5)
        _gridRows.value = rows.coerceIn(1, 10)
    }

    fun toggleFitEntirePhone() {
        val next = !_fitEntirePhone.value
        _fitEntirePhone.value = next
        viewModelScope.launch {
            repository.setAllWindowsZoom(
                cols = _gridColumns.value,
                rows = _gridRows.value,
                fitPhone = next,
                proportional = _proportionalZoom.value
            )
            _uiEvents.emit(
                StreamUiEvent.ApplyAllWindowsZoom(
                    cols = _gridColumns.value,
                    rows = _gridRows.value,
                    fitPhone = next,
                    proportional = _proportionalZoom.value
                )
            )
        }
    }

    fun toggleMaximizeWindow(windowId: String) {
        _maximizedWindowId.value = if (_maximizedWindowId.value == windowId) null else windowId
    }

    fun openWindowZoomDialog(window: StreamWindow) {
        _zoomDialogTargetWindow.value = window
    }

    fun openGlobalZoomDialog() {
        _showGlobalZoomDialog.value = true
    }

    fun closeZoomDialog() {
        _zoomDialogTargetWindow.value = null
        _showGlobalZoomDialog.value = false
    }

    fun applyZoomDimensions(
        targetWindow: StreamWindow?,
        cols: Int,
        rows: Int,
        fitPhone: Boolean,
        proportional: Boolean,
        applyToAll: Boolean
    ) {
        val safeCols = cols.coerceIn(1, 5)
        val safeRows = rows.coerceIn(1, 10)
        _fitEntirePhone.value = fitPhone
        _proportionalZoom.value = proportional
        viewModelScope.launch {
            if (applyToAll || targetWindow == null) {
                _gridColumns.value = safeCols
                _gridRows.value = safeRows
                repository.setAllWindowsZoom(safeCols, safeRows, fitPhone, proportional)
                _uiEvents.emit(
                    StreamUiEvent.ApplyAllWindowsZoom(safeCols, safeRows, fitPhone, proportional)
                )
            } else {
                repository.updateWindowZoom(targetWindow.id, safeCols, safeRows, fitPhone, proportional)
                _uiEvents.emit(
                    StreamUiEvent.ApplyWindowZoom(targetWindow.id, safeCols, safeRows, fitPhone, proportional)
                )
            }
            closeZoomDialog()
        }
    }

    // --- Master Audio & Per-Window Audio Control ---

    fun toggleMute(window: StreamWindow) {
        val nextMuted = !window.isMuted
        viewModelScope.launch {
            repository.toggleMute(window.id, window.isMuted)
            _uiEvents.emit(StreamUiEvent.ApplyWindowAudioMute(window.id, nextMuted))
            val updatedList = repository.getAllWindowsSync()
            _masterAudioMuted.value = updatedList.isNotEmpty() && updatedList.all { it.isMuted }
        }
    }

    fun toggleMasterAudio() {
        val currentWindows = windows.value
        val allCurrentlyMuted = currentWindows.isEmpty() || currentWindows.all { it.isMuted }
        val targetMuted = !allCurrentlyMuted
        setMasterAudioMuted(targetMuted)
    }

    fun setMasterAudioMuted(isMuted: Boolean) {
        _masterAudioMuted.value = isMuted
        viewModelScope.launch {
            repository.setAllMuted(isMuted)
            _uiEvents.emit(StreamUiEvent.ApplyAllAudioMute(isMuted))
            _uiEvents.emit(
                StreamUiEvent.ShowSnackbar(
                    if (isMuted) "Master Audio Muted across all active emulators"
                    else "Master Audio Enabled across all active emulators"
                )
            )
        }
    }

    // --- Preset Manager ---

    fun openPresetManagerDialog() {
        _showPresetManagerDialog.value = true
    }

    fun closePresetManagerDialog() {
        _showPresetManagerDialog.value = false
    }

    fun saveCurrentGridAsPreset(name: String, description: String) {
        val currentWindows = windows.value
        if (currentWindows.isEmpty()) return
        viewModelScope.launch {
            val saved = repository.saveGridPreset(
                name = name,
                description = description,
                gridColumns = _gridColumns.value,
                gridRows = _gridRows.value,
                fitEntirePhone = _fitEntirePhone.value,
                proportionalZoom = _proportionalZoom.value,
                masterAudioMuted = _masterAudioMuted.value,
                windows = currentWindows
            )
            _activePresetId.value = saved.id
            if (Firebase.auth.currentUser != null) {
                cloudRepository.saveCloudPreset(saved)
            }
            _uiEvents.emit(StreamUiEvent.ShowSnackbar("Saved preset '${saved.name}' to local & cloud"))
        }
    }

    fun loadGridPreset(preset: GridPreset) {
        val decoded = PresetJsonSerializer.decodeWindows(preset.windowsJson)
        if (decoded.isEmpty()) return
        _activePresetId.value = preset.id
        _gridColumns.value = preset.gridColumns.coerceIn(1, 5)
        _gridRows.value = preset.gridRows.coerceIn(1, 10)
        _fitEntirePhone.value = preset.fitEntirePhone
        _proportionalZoom.value = preset.proportionalZoom
        _masterAudioMuted.value = preset.masterAudioMuted
        _maximizedWindowId.value = null

        viewModelScope.launch {
            val syncedWindows = decoded.map { it.copy(isMuted = preset.masterAudioMuted) }
            repository.applyPresetWindows(syncedWindows)
            _uiEvents.emit(StreamUiEvent.ApplyAllAudioMute(preset.masterAudioMuted))
            _uiEvents.emit(
                StreamUiEvent.ApplyAllWindowsZoom(
                    cols = preset.gridColumns,
                    rows = preset.gridRows,
                    fitPhone = preset.fitEntirePhone,
                    proportional = preset.proportionalZoom
                )
            )
            _uiEvents.emit(
                StreamUiEvent.ShowSnackbar(
                    "Loaded preset '${preset.name}' (${syncedWindows.size} windows, ${preset.gridColumns}x${preset.gridRows})"
                )
            )
        }
    }

    fun deleteGridPreset(preset: GridPreset) {
        if (preset.isBuiltIn) return
        viewModelScope.launch {
            repository.deleteGridPreset(preset)
            if (Firebase.auth.currentUser != null) {
                cloudRepository.deleteCloudPreset(preset.id)
            }
            if (_activePresetId.value == preset.id) {
                _activePresetId.value = null
            }
        }
    }

    fun restoreBuiltInPresets() {
        viewModelScope.launch {
            repository.restoreBuiltInPresets()
            _uiEvents.emit(StreamUiEvent.ShowSnackbar("Restored built-in presets"))
        }
    }

    // --- Background Feed Health Monitor & Auto Soft-Refresh ---

    fun openHealthMonitorDialog() {
        _showHealthMonitorDialog.value = true
    }

    fun closeHealthMonitorDialog() {
        _showHealthMonitorDialog.value = false
    }

    fun reportWindowHeartbeat(
        window: StreamWindow,
        frameSignature: String = "dom_ok",
        isConnected: Boolean = true
    ) {
        StreamHealthMonitorBus.reportHeartbeat(
            windowId = window.id,
            windowTitle = window.title,
            streamUrl = window.url,
            frameSignature = frameSignature,
            isConnected = isConnected
        )
    }

    fun reportWindowFreezeOrDrop(
        window: StreamWindow,
        isDropped: Boolean,
        reason: String
    ) {
        StreamHealthMonitorBus.reportStreamFreezeOrDrop(
            windowId = window.id,
            windowTitle = window.title,
            streamUrl = window.url,
            isDropped = isDropped,
            reason = reason
        )
        StreamHealthMonitorService.triggerImmediateScan(getApplication())
    }

    fun toggleHealthMonitorService(enabled: Boolean) {
        StreamHealthMonitorBus.setServiceRunning(enabled)
        if (enabled) {
            StreamHealthMonitorService.startService(getApplication())
        } else {
            StreamHealthMonitorService.stopService(getApplication())
        }
    }

    fun toggleAutoSoftRefresh(enabled: Boolean) {
        StreamHealthMonitorBus.setAutoSoftRefreshEnabled(enabled)
    }

    fun setFreezeThresholdSeconds(seconds: Int) {
        StreamHealthMonitorBus.setFreezeThresholdSec(seconds)
    }

    fun triggerImmediateHealthScan() {
        viewModelScope.launch {
            val commands = StreamHealthMonitorBus.evaluateFeedHealthAndTriggerSoftRefresh()
            if (commands.isEmpty()) {
                _uiEvents.emit(
                    StreamUiEvent.ShowSnackbar("Health Scan Complete: All active emulator feeds are healthy")
                )
            }
        }
    }

    fun simulateStreamFreezeForTesting(window: StreamWindow) {
        reportWindowFreezeOrDrop(
            window = window,
            isDropped = false,
            reason = "Diagnostic freeze test triggered on ${window.title}"
        )
        viewModelScope.launch {
            val commands = StreamHealthMonitorBus.evaluateFeedHealthAndTriggerSoftRefresh()
            for (cmd in commands) {
                repository.recordSoftRefresh(
                    id = cmd.windowId,
                    status = FeedHealthStatus.RECOVERED.name,
                    heartbeatMs = System.currentTimeMillis(),
                    reason = cmd.reason
                )
                if (Firebase.auth.currentUser != null) {
                    cloudRepository.logHealthIncident(
                        windowId = cmd.windowId,
                        windowTitle = cmd.windowTitle,
                        streamUrl = cmd.streamUrl,
                        status = FeedHealthStatus.RECOVERED.name,
                        reason = cmd.reason,
                        recoveryAction = "Automatic WebView Soft-Refresh (Attempt #${cmd.attemptNumber})",
                        attemptCount = cmd.attemptNumber,
                        latencyMs = 115
                    )
                }
            }
        }
    }

    // --- Window CRUD & Actions ---

    fun openAddWindowDialog() {
        _showAddDialog.value = true
    }

    fun closeAddWindowDialog() {
        _showAddDialog.value = false
    }

    fun addStreamWindow(
        title: String,
        url: String,
        feedType: FeedType,
        scrollIntervalSec: Int,
        zoomCols: Int,
        zoomRows: Int
    ) {
        viewModelScope.launch {
            val created = repository.addWindow(
                title = title,
                url = url,
                feedType = feedType,
                scrollIntervalSec = scrollIntervalSec,
                zoomCols = zoomCols,
                zoomRows = zoomRows,
                isMuted = _masterAudioMuted.value
            )
            StreamHealthMonitorBus.reportHeartbeat(
                windowId = created.id,
                windowTitle = created.title,
                streamUrl = created.url
            )
            closeAddWindowDialog()
        }
    }

    fun removeWindow(window: StreamWindow) {
        viewModelScope.launch {
            StreamHealthMonitorBus.unregisterWindow(window.id)
            repository.removeWindow(window)
            if (_maximizedWindowId.value == window.id) {
                _maximizedWindowId.value = null
            }
        }
    }

    fun toggleAutoScroll(window: StreamWindow) {
        viewModelScope.launch {
            repository.toggleAutoScroll(window.id, window.isAutoScrollEnabled)
        }
    }

    fun setAllAutoScroll(enabled: Boolean) {
        viewModelScope.launch {
            repository.setAllAutoScroll(enabled)
        }
    }

    fun cycleLoginState(window: StreamWindow) {
        val next = when (window.loginState) {
            LoginState.LOGGED_OUT -> LoginState.LOGGING_IN
            LoginState.LOGGING_IN -> LoginState.LOGGED_IN
            LoginState.LOGGED_IN -> LoginState.LOGGED_OUT
        }
        viewModelScope.launch {
            repository.setLoginStatus(window.id, next)
        }
    }

    fun promoteToMainChoice(window: StreamWindow) {
        viewModelScope.launch {
            repository.setMainChoice(window.id)
        }
    }

    fun refreshWindow(windowId: String) {
        viewModelScope.launch {
            _uiEvents.emit(StreamUiEvent.RefreshWindow(windowId, isSoftRecovery = false))
        }
    }

    fun refreshAllWindows() {
        viewModelScope.launch {
            _uiEvents.emit(StreamUiEvent.RefreshAllWindows)
        }
    }

    // --- Gemini AI Command Assistant ---

    fun openAiAssistantDialog() {
        _showAiAssistantDialog.value = true
    }

    fun closeAiAssistantDialog() {
        _showAiAssistantDialog.value = false
    }

    fun selectGeminiTier(tier: GeminiTaskTier) {
        _selectedGeminiTier.value = tier
    }

    fun selectChatbotPersona(persona: ChatbotRolePersona) {
        _selectedPersona.value = persona
    }

    fun toggleSearchGrounding(enabled: Boolean) {
        _enableSearchGrounding.value = enabled
    }

    fun sendAiPrompt(promptText: String) {
        val cleanPrompt = promptText.trim()
        if (cleanPrompt.isEmpty() || _isAiGenerating.value) return

        val tier = _selectedGeminiTier.value
        val persona = _selectedPersona.value
        val grounded = _enableSearchGrounding.value

        val contextSummary = buildString {
            val activeWins = windows.value
            val health = healthMonitorSummary.value
            append("Grid: ${_gridColumns.value}x${_gridRows.value}, ")
            append("Active Windows: ${activeWins.size} (${activeWins.joinToString { it.title }}), ")
            append("Master Audio: ${if (_masterAudioMuted.value) "MUTED" else "ON"}, ")
            append("Health Monitor: ${health.healthyCount} healthy, ${health.frozenOrDroppedCount} frozen/dropped, ")
            append("Auto Recoveries: ${health.totalAutoRecoveries}")
        }

        viewModelScope.launch {
            _isAiGenerating.value = true
            try {
                val historyPairs = cloudChatMessages.value
                    .sortedBy { it.createdAt?.seconds ?: 0L }
                    .map { it.role to it.content }

                if (Firebase.auth.currentUser != null) {
                    cloudRepository.saveChatMessage(
                        role = "user",
                        content = cleanPrompt,
                        modelId = tier.modelId,
                        agentPersona = persona.title
                    )
                }

                val reply = GeminiCommandService.sendConversationTurn(
                    prompt = cleanPrompt,
                    history = historyPairs,
                    tier = tier,
                    persona = persona,
                    enableSearchGrounding = grounded,
                    gridContextSummary = contextSummary
                )

                val sourcesSummary = reply.groundingSources.joinToString("\n") { "• ${it.title}: ${it.uri}" }
                if (Firebase.auth.currentUser != null) {
                    cloudRepository.saveChatMessage(
                        role = "model",
                        content = reply.text,
                        modelId = reply.modelId,
                        agentPersona = reply.persona,
                        sourcesSummary = sourcesSummary
                    )
                }
            } finally {
                _isAiGenerating.value = false
            }
        }
    }

    private companion object {
        const val TAG = "MultiStreamVM"
    }
}
