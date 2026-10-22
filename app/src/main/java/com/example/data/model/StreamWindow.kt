package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class FeedType(val label: String, val badgeColorHex: Long) {
    GAME_CLOUD("Emulator / Game", 0xFF00E5FF),
    LIVE_TV("Live Broadcast", 0xFFFF4081),
    INTEL_DASHBOARD("Telemetry / OSINT", 0xFF00E676),
    SOCIAL_FEED("Social Stream", 0xFF7C4DFF),
    CUSTOM_WEB("Custom Web", 0xFFFFAB00)
}

enum class LoginState(val label: String) {
    LOGGED_OUT("Guest"),
    LOGGING_IN("Authenticating"),
    LOGGED_IN("Authenticated")
}

enum class FeedHealthStatus(val label: String, val badgeHex: Long) {
    HEALTHY("Healthy", 0xFF00E676),
    FROZEN("Frozen", 0xFFFF9100),
    DROPPED("Dropped", 0xFFFF1744),
    RECOVERING("Soft-Refreshing", 0xFF00E5FF),
    RECOVERED("Recovered", 0xFF69F0AE);

    companion object {
        fun fromString(value: String): FeedHealthStatus {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: HEALTHY
        }
    }
}

@Entity(tableName = "stream_windows")
data class StreamWindow(
    @PrimaryKey val id: String,
    val title: String,
    val url: String,
    val feedType: FeedType = FeedType.CUSTOM_WEB,
    val isAutoScrollEnabled: Boolean = false,
    val scrollIntervalSec: Int = 6,
    val isMuted: Boolean = true,
    val loginState: LoginState = LoginState.LOGGED_OUT,
    val zoomWidthCols: Int = 1,
    val zoomHeightRows: Int = 1,
    val fitEntirePhone: Boolean = true,
    val proportionalZoom: Boolean = false,
    val orderIndex: Int = 0,
    val isMainChoice: Boolean = false,
    val healthStatus: String = FeedHealthStatus.HEALTHY.name,
    val lastHeartbeatMs: Long = System.currentTimeMillis(),
    val softRefreshCount: Int = 0,
    val lastRecoveryReason: String = ""
)

data class CuratedFeed(
    val title: String,
    val url: String,
    val feedType: FeedType,
    val description: String,
    val isMainChoice: Boolean = false,
    val defaultZoomCols: Int = 1,
    val defaultZoomRows: Int = 1
)

