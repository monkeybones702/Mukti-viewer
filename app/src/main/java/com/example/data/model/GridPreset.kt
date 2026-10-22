package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "grid_presets")
data class GridPreset(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val gridColumns: Int = 2,
    val gridRows: Int = 2,
    val fitEntirePhone: Boolean = true,
    val proportionalZoom: Boolean = false,
    val masterAudioMuted: Boolean = true,
    val windowsJson: String,
    val isBuiltIn: Boolean = false,
    val createdAtMs: Long = System.currentTimeMillis()
)

data class PresetWindowSnapshot(
    val id: String,
    val title: String,
    val url: String,
    val feedType: FeedType,
    val isAutoScrollEnabled: Boolean,
    val scrollIntervalSec: Int,
    val isMuted: Boolean,
    val zoomWidthCols: Int,
    val zoomHeightRows: Int,
    val fitEntirePhone: Boolean,
    val proportionalZoom: Boolean,
    val orderIndex: Int,
    val isMainChoice: Boolean
)

object PresetJsonSerializer {
    private const val RECORD_SEP = "\u001E"
    private const val FIELD_SEP = "\u001F"

    private fun escapeField(raw: String): String {
        return raw
            .replace("\\", "\\\\")
            .replace(RECORD_SEP, "")
            .replace(FIELD_SEP, "")
            .replace("\n", "\\n")
            .replace("|", "\\p")
    }

    private fun unescapeField(escaped: String): String {
        return escaped
            .replace("\\p", "|")
            .replace("\\n", "\n")
            .replace("\\\\", "\\")
    }

    fun encodeWindows(windows: List<StreamWindow>): String {
        if (windows.isEmpty()) return "[]"
        return windows.joinToString(separator = RECORD_SEP) { win ->
            listOf(
                escapeField(win.id),
                escapeField(win.title),
                escapeField(win.url),
                win.feedType.name,
                win.isAutoScrollEnabled.toString(),
                win.scrollIntervalSec.coerceIn(2, 60).toString(),
                win.isMuted.toString(),
                win.zoomWidthCols.coerceIn(1, 5).toString(),
                win.zoomHeightRows.coerceIn(1, 10).toString(),
                win.fitEntirePhone.toString(),
                win.proportionalZoom.toString(),
                win.orderIndex.toString(),
                win.isMainChoice.toString()
            ).joinToString(separator = FIELD_SEP)
        }
    }

    fun decodeWindows(encoded: String): List<StreamWindow> {
        val trimmed = encoded.trim()
        if (trimmed.isEmpty() || trimmed == "[]") return emptyList()
        return trimmed.split(RECORD_SEP).mapIndexedNotNull { idx, record ->
            val parts = record.split(FIELD_SEP)
            if (parts.size < 13) return@mapIndexedNotNull null
            val feedType = runCatching { FeedType.valueOf(parts[3]) }.getOrDefault(FeedType.CUSTOM_WEB)
            StreamWindow(
                id = unescapeField(parts[0]).ifBlank { "win_preset_${idx + 1}" },
                title = unescapeField(parts[1]).ifBlank { "Window ${idx + 1}" },
                url = unescapeField(parts[2]).ifBlank { IntelRegistry.UPTOPLAY_URL },
                feedType = feedType,
                isAutoScrollEnabled = parts[4].toBooleanStrictOrNull() ?: false,
                scrollIntervalSec = parts[5].toIntOrNull()?.coerceIn(2, 60) ?: 6,
                isMuted = parts[6].toBooleanStrictOrNull() ?: true,
                loginState = LoginState.LOGGED_OUT,
                zoomWidthCols = parts[7].toIntOrNull()?.coerceIn(1, 5) ?: 1,
                zoomHeightRows = parts[8].toIntOrNull()?.coerceIn(1, 10) ?: 1,
                fitEntirePhone = parts[9].toBooleanStrictOrNull() ?: true,
                proportionalZoom = parts[10].toBooleanStrictOrNull() ?: false,
                orderIndex = parts[11].toIntOrNull() ?: idx,
                isMainChoice = parts[12].toBooleanStrictOrNull() ?: (idx == 0)
            )
        }
    }

    fun getBuiltInPresets(): List<GridPreset> {
        val focus4Windows = IntelRegistry.curatedFeeds.take(4).mapIndexed { index, feed ->
            StreamWindow(
                id = "preset_4focus_win_${index + 1}",
                title = feed.title,
                url = feed.url,
                feedType = feed.feedType,
                isAutoScrollEnabled = index >= 2,
                scrollIntervalSec = 6,
                isMuted = true,
                zoomWidthCols = feed.defaultZoomCols,
                zoomHeightRows = feed.defaultZoomRows,
                fitEntirePhone = true,
                proportionalZoom = false,
                orderIndex = index,
                isMainChoice = index == 0
            )
        }

        val overview10Windows = IntelRegistry.curatedFeeds.take(10).mapIndexed { index, feed ->
            StreamWindow(
                id = "preset_10overview_win_${index + 1}",
                title = feed.title,
                url = feed.url,
                feedType = feed.feedType,
                isAutoScrollEnabled = index % 2 == 1,
                scrollIntervalSec = 8,
                isMuted = true,
                zoomWidthCols = 2,
                zoomHeightRows = 3,
                fitEntirePhone = true,
                proportionalZoom = false,
                orderIndex = index,
                isMainChoice = index == 0
            )
        }

        val singleFocusWindow = listOf(
            StreamWindow(
                id = "preset_1focus_win_1",
                title = "UptoPlay Android Hub",
                url = IntelRegistry.UPTOPLAY_URL,
                feedType = FeedType.GAME_CLOUD,
                isAutoScrollEnabled = false,
                scrollIntervalSec = 6,
                isMuted = false,
                zoomWidthCols = 1,
                zoomHeightRows = 1,
                fitEntirePhone = true,
                proportionalZoom = false,
                orderIndex = 0,
                isMainChoice = true
            )
        )

        return listOf(
            GridPreset(
                id = "builtin_4_window_focus",
                name = "4-Window Focus",
                description = "Balanced 2x2 quad emulator & telemetry layout with UptoPlay primary focus.",
                gridColumns = 2,
                gridRows = 2,
                fitEntirePhone = true,
                proportionalZoom = false,
                masterAudioMuted = true,
                windowsJson = encodeWindows(focus4Windows),
                isBuiltIn = true,
                createdAtMs = 1700000001000L
            ),
            GridPreset(
                id = "builtin_10_window_overview",
                name = "10-Window Overview",
                description = "High-density 2x5 10-feed command matrix for simultaneous multi-stream monitoring.",
                gridColumns = 2,
                gridRows = 5,
                fitEntirePhone = true,
                proportionalZoom = false,
                masterAudioMuted = true,
                windowsJson = encodeWindows(overview10Windows),
                isBuiltIn = true,
                createdAtMs = 1700000002000L
            ),
            GridPreset(
                id = "builtin_1_window_full",
                name = "1x1 Full Phone Focus",
                description = "Dedicated full-screen 1x1 phone fit for low-latency primary emulator interaction.",
                gridColumns = 1,
                gridRows = 1,
                fitEntirePhone = true,
                proportionalZoom = false,
                masterAudioMuted = false,
                windowsJson = encodeWindows(singleFocusWindow),
                isBuiltIn = true,
                createdAtMs = 1700000003000L
            )
        )
    }
}
