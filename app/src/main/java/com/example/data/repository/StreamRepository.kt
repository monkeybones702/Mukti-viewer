package com.example.data.repository

import com.example.data.db.GridPresetDao
import com.example.data.db.StreamWindowDao
import com.example.data.model.FeedType
import com.example.data.model.GridPreset
import com.example.data.model.IntelRegistry
import com.example.data.model.LoginState
import com.example.data.model.PresetJsonSerializer
import com.example.data.model.StreamWindow
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class StreamRepository(
    private val dao: StreamWindowDao,
    private val presetDao: GridPresetDao? = null
) {
    val allWindows: Flow<List<StreamWindow>> = dao.getAllWindows()
    val allPresets: Flow<List<GridPreset>>? = presetDao?.getAllPresets()

    suspend fun ensureDefaultsInitialized() {
        if (dao.getWindowCount() == 0) {
            dao.insertWindows(IntelRegistry.getDefaultWindows())
        }
        if (presetDao != null && presetDao.getPresetCount() == 0) {
            presetDao.insertPresets(PresetJsonSerializer.getBuiltInPresets())
        }
    }

    suspend fun getAllWindowsSync(): List<StreamWindow> = dao.getAllWindowsSync()

    suspend fun addWindow(
        title: String,
        url: String,
        feedType: FeedType,
        scrollIntervalSec: Int,
        zoomCols: Int = 1,
        zoomRows: Int = 1,
        isMuted: Boolean = true
    ): StreamWindow {
        val currentCount = dao.getWindowCount()
        val formattedUrl = if (url.startsWith("http://") || url.startsWith("https://")) {
            url
        } else {
            "https://$url"
        }
        val window = StreamWindow(
            id = "win_${UUID.randomUUID().toString().replace("-", "").take(10)}",
            title = title.ifBlank { "Stream ${currentCount + 1}" },
            url = formattedUrl,
            feedType = feedType,
            isAutoScrollEnabled = false,
            scrollIntervalSec = scrollIntervalSec.coerceIn(2, 60),
            isMuted = isMuted,
            loginState = LoginState.LOGGED_OUT,
            zoomWidthCols = zoomCols.coerceIn(1, 5),
            zoomHeightRows = zoomRows.coerceIn(1, 10),
            fitEntirePhone = true,
            proportionalZoom = false,
            orderIndex = currentCount,
            isMainChoice = currentCount == 0
        )
        dao.insertWindow(window)
        return window
    }

    suspend fun updateWindow(window: StreamWindow) {
        dao.updateWindow(window)
    }

    suspend fun removeWindow(window: StreamWindow) {
        dao.deleteWindow(window)
    }

    suspend fun toggleMute(id: String, currentMute: Boolean) {
        dao.updateMuteState(id, !currentMute)
    }

    suspend fun setAllMuted(isMuted: Boolean) {
        dao.setAllMuted(isMuted)
    }

    suspend fun toggleAutoScroll(id: String, currentEnabled: Boolean) {
        dao.updateAutoScroll(id, !currentEnabled)
    }

    suspend fun setAllAutoScroll(enabled: Boolean) {
        dao.setAllAutoScroll(enabled)
    }

    suspend fun setLoginStatus(id: String, state: LoginState) {
        dao.updateLoginState(id, state)
    }

    suspend fun setMainChoice(id: String) {
        dao.setMainChoiceWindow(id)
    }

    suspend fun updateWindowZoom(
        id: String,
        cols: Int,
        rows: Int,
        fitPhone: Boolean,
        proportional: Boolean
    ) {
        dao.updateWindowZoom(
            id = id,
            cols = cols.coerceIn(1, 5),
            rows = rows.coerceIn(1, 10),
            fitPhone = fitPhone,
            proportional = proportional
        )
    }

    suspend fun setAllWindowsZoom(
        cols: Int,
        rows: Int,
        fitPhone: Boolean,
        proportional: Boolean
    ) {
        dao.setAllWindowsZoom(
            cols = cols.coerceIn(1, 5),
            rows = rows.coerceIn(1, 10),
            fitPhone = fitPhone,
            proportional = proportional
        )
    }

    suspend fun updateHealthState(
        id: String,
        status: String,
        heartbeatMs: Long,
        reason: String
    ) {
        dao.updateHealthState(id, status, heartbeatMs, reason)
    }

    suspend fun recordSoftRefresh(
        id: String,
        status: String,
        heartbeatMs: Long,
        reason: String
    ) {
        dao.recordSoftRefresh(id, status, heartbeatMs, reason)
    }

    suspend fun saveGridPreset(
        name: String,
        description: String,
        gridColumns: Int,
        gridRows: Int,
        fitEntirePhone: Boolean,
        proportionalZoom: Boolean,
        masterAudioMuted: Boolean,
        windows: List<StreamWindow>
    ): GridPreset {
        val preset = GridPreset(
            id = "preset_${UUID.randomUUID().toString().replace("-", "").take(10)}",
            name = name.ifBlank { "Custom ${windows.size}-Window Grid" },
            description = description.ifBlank {
                "${gridColumns}x${gridRows} grid with ${windows.size} active stream windows"
            },
            gridColumns = gridColumns.coerceIn(1, 5),
            gridRows = gridRows.coerceIn(1, 10),
            fitEntirePhone = fitEntirePhone,
            proportionalZoom = proportionalZoom,
            masterAudioMuted = masterAudioMuted,
            windowsJson = PresetJsonSerializer.encodeWindows(windows),
            isBuiltIn = false,
            createdAtMs = System.currentTimeMillis()
        )
        presetDao?.insertPreset(preset)
        return preset
    }

    suspend fun upsertPreset(preset: GridPreset) {
        presetDao?.insertPreset(preset)
    }

    suspend fun applyPresetWindows(windows: List<StreamWindow>) {
        if (windows.isEmpty()) return
        dao.deleteAllWindows()
        dao.insertWindows(windows)
    }

    suspend fun deleteGridPreset(preset: GridPreset) {
        presetDao?.deletePreset(preset)
    }

    suspend fun restoreBuiltInPresets() {
        presetDao?.insertPresets(PresetJsonSerializer.getBuiltInPresets())
    }
}
