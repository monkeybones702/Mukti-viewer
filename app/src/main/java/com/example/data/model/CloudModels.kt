package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue

data class CloudGridPreset(
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val description: String = "",
    val gridColumns: Int = 2,
    val gridRows: Int = 2,
    val fitEntirePhone: Boolean = true,
    val proportionalZoom: Boolean = false,
    val masterAudioMuted: Boolean = true,
    val windowCount: Int = 4,
    val windowsJson: String = "[]",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> {
        return mapOf<String, Any?>(
            "id" to id,
            "userId" to userId,
            "name" to name.take(100).ifBlank { "Custom Preset" },
            "description" to description.take(500),
            "gridColumns" to gridColumns.coerceIn(1, 5),
            "gridRows" to gridRows.coerceIn(1, 10),
            "fitEntirePhone" to fitEntirePhone,
            "proportionalZoom" to proportionalZoom,
            "masterAudioMuted" to masterAudioMuted,
            "windowCount" to windowCount.coerceIn(1, 25),
            "windowsJson" to windowsJson.ifBlank { "[]" }.take(50000),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null }.mapValues { it.value!! }
    }

    fun toUpdateMap(): Map<String, Any> {
        return mapOf<String, Any?>(
            "name" to name.take(100).ifBlank { "Custom Preset" },
            "description" to description.take(500),
            "gridColumns" to gridColumns.coerceIn(1, 5),
            "gridRows" to gridRows.coerceIn(1, 10),
            "fitEntirePhone" to fitEntirePhone,
            "proportionalZoom" to proportionalZoom,
            "masterAudioMuted" to masterAudioMuted,
            "windowCount" to windowCount.coerceIn(1, 25),
            "windowsJson" to windowsJson.ifBlank { "[]" }.take(50000),
            "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null }.mapValues { it.value!! }
    }
}

data class StreamHealthIncident(
    val id: String = "",
    val userId: String = "",
    val windowId: String = "",
    val windowTitle: String = "",
    val streamUrl: String = "",
    val status: String = FeedHealthStatus.HEALTHY.name,
    val reason: String = "",
    val recoveryAction: String = "",
    val attemptCount: Int = 1,
    val latencyMs: Int = 0,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> {
        return mapOf<String, Any?>(
            "id" to id,
            "userId" to userId,
            "windowId" to windowId.take(128).ifBlank { "unknown_window" },
            "windowTitle" to windowTitle.take(120).ifBlank { "Stream Feed" },
            "streamUrl" to streamUrl.take(2048).ifBlank { "https://www.uptoplay.net" },
            "status" to status,
            "reason" to reason.take(500).ifBlank { "Feed health check" },
            "recoveryAction" to recoveryAction.take(200).ifBlank { "Soft-refresh WebView" },
            "attemptCount" to attemptCount.coerceIn(0, 100),
            "latencyMs" to latencyMs.coerceIn(0, 600000),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null }.mapValues { it.value!! }
    }

    fun toUpdateMap(): Map<String, Any> {
        return mapOf<String, Any?>(
            "status" to status,
            "reason" to reason.take(500).ifBlank { "Feed recovered" },
            "recoveryAction" to recoveryAction.take(200).ifBlank { "Soft-refresh WebView" },
            "attemptCount" to attemptCount.coerceIn(0, 100),
            "latencyMs" to latencyMs.coerceIn(0, 600000),
            "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null }.mapValues { it.value!! }
    }
}

data class CloudChatMessage(
    val id: String = "",
    val userId: String = "",
    val role: String = "user",
    val content: String = "",
    val modelId: String = "gemini-3.5-flash",
    val agentPersona: String = "Stream Ops Commander",
    val sourcesSummary: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> {
        return mapOf<String, Any?>(
            "id" to id,
            "userId" to userId,
            "role" to if (role == "model") "model" else "user",
            "content" to content.take(20000).ifBlank { "..." },
            "modelId" to modelId.take(100).ifBlank { "gemini-3.5-flash" },
            "agentPersona" to agentPersona.take(100).ifBlank { "Stream Ops Commander" },
            "sourcesSummary" to sourcesSummary.take(5000),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null }.mapValues { it.value!! }
    }
}
