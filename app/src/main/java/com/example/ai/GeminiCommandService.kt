package com.example.ai

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

enum class GeminiTaskTier(
    val modelId: String,
    val label: String,
    val description: String,
    val supportsSearchGrounding: Boolean
) {
    GENERAL_GROUNDED(
        modelId = "gemini-3.5-flash",
        label = "General + Live Search (3.5 Flash)",
        description = "Balanced reasoning with Google Search grounding for live stream URLs & status",
        supportsSearchGrounding = true
    ),
    COMPLEX_PRO(
        modelId = "gemini-3.1-pro-preview",
        label = "Deep Diagnostics (3.1 Pro)",
        description = "Complex multi-window grid optimization, WebRTC/codec & freeze root-cause analysis",
        supportsSearchGrounding = false
    ),
    FAST_LITE(
        modelId = "gemini-3.1-flash-lite-preview",
        label = "Instant Ops (3.1 Flash Lite)",
        description = "Ultra-low-latency preset suggestions & quick stream actions",
        supportsSearchGrounding = false
    )
}

enum class ChatbotRolePersona(
    val title: String,
    val badgeLabel: String,
    val systemInstruction: String
) {
    STREAM_OPS_COMMANDER(
        title = "Stream Ops Commander",
        badgeLabel = "OPS",
        systemInstruction = "You are the Streamsy Multi-View Operations Commander. Help the user configure multi-window emulator grids (such as 4-Window Focus 2x2 or 10-Window Overview 2x5), manage master audio across active WebViews, and optimize zoom-out dimensions from 1x1 Full Phone Fit up to 5x10. Keep answers concise, actionable, and structured."
    ),
    FEED_HEALTH_DIAGNOSTICIAN(
        title = "Feed Health & Recovery Engineer",
        badgeLabel = "HEALTH",
        systemInstruction = "You are the Streamsy Background Feed Health & Recovery Engineer. Diagnose frozen or dropped emulator streams, explain WebView heartbeat stalls, recommend freeze timeout thresholds, and analyze soft-refresh recovery telemetry."
    ),
    LIVE_STREAM_SCOUT(
        title = "Live Stream & OSINT Scout",
        badgeLabel = "SCOUT",
        systemInstruction = "You are the Streamsy Live Feed & Emulator Discovery Scout. Use Google Search grounding when enabled to discover verified browser-compatible emulators, public live telemetry dashboards, and low-latency stream feeds suitable for multi-window monitoring."
    )
}

data class GroundingSource(
    val title: String,
    val uri: String
)

data class GeminiAssistantReply(
    val text: String,
    val modelId: String,
    val persona: String,
    val groundingSources: List<GroundingSource> = emptyList()
)

object GeminiCommandService {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/"

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    suspend fun sendConversationTurn(
        prompt: String,
        history: List<Pair<String, String>>,
        tier: GeminiTaskTier,
        persona: ChatbotRolePersona,
        enableSearchGrounding: Boolean,
        gridContextSummary: String
    ): GeminiAssistantReply = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext GeminiAssistantReply(
                text = "Gemini API key is not configured yet. Please add your `GEMINI_API_KEY` in the **Secrets panel in AI Studio** to enable live AI responses.\n\n" +
                    "**Active Context Snapshot:**\n$gridContextSummary",
                modelId = tier.modelId,
                persona = persona.title
            )
        }

        val contentsArray = JSONArray()
        // Include recent conversation turns (up to last 8 messages for context window efficiency)
        history.takeLast(8).forEach { (role, messageText) ->
            val mappedRole = if (role == "model") "model" else "user"
            contentsArray.put(
                JSONObject().apply {
                    put("role", mappedRole)
                    put(
                        "parts",
                        JSONArray().put(JSONObject().put("text", messageText))
                    )
                }
            )
        }

        // Current user turn with live multi-stream telemetry context
        val enrichedPrompt = buildString {
            appendLine("[Current Multi-Stream Telemetry Context: $gridContextSummary]")
            appendLine()
            append(prompt)
        }
        contentsArray.put(
            JSONObject().apply {
                put("role", "user")
                put(
                    "parts",
                    JSONArray().put(JSONObject().put("text", enrichedPrompt))
                )
            }
        )

        val requestJson = JSONObject().apply {
            put("contents", contentsArray)
            put(
                "systemInstruction",
                JSONObject().apply {
                    put(
                        "parts",
                        JSONArray().put(JSONObject().put("text", persona.systemInstruction))
                    )
                }
            )
            put(
                "generationConfig",
                JSONObject().apply {
                    put("temperature", 0.6)
                    put("topP", 0.95)
                }
            )
            if (enableSearchGrounding && tier.supportsSearchGrounding) {
                put(
                    "tools",
                    JSONArray().put(
                        JSONObject().put("google_search", JSONObject())
                    )
                )
            }
        }

        val endpoint = "$BASE_URL${tier.modelId}:generateContent?key=$apiKey"
        val request = Request.Builder()
            .url(endpoint)
            .post(requestJson.toString().toRequestBody("application/json".toMediaType()))
            .build()

        return@withContext try {
            okHttpClient.newCall(request).execute().use { response ->
                val rawBody = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@use GeminiAssistantReply(
                        text = "Gemini API request returned HTTP ${response.code}: ${rawBody.take(280)}",
                        modelId = tier.modelId,
                        persona = persona.title
                    )
                }
                parseGenerateContentResponse(rawBody, tier.modelId, persona.title)
            }
        } catch (e: Exception) {
            GeminiAssistantReply(
                text = "Network error communicating with ${tier.modelId}: ${e.localizedMessage ?: e.message}",
                modelId = tier.modelId,
                persona = persona.title
            )
        }
    }

    fun parseGenerateContentResponse(
        rawJson: String,
        modelId: String,
        personaTitle: String
    ): GeminiAssistantReply {
        return try {
            val root = JSONObject(rawJson)
            val candidates = root.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val contentObj = firstCandidate?.optJSONObject("content")
            val parts = contentObj?.optJSONArray("parts")

            val textBuilder = StringBuilder()
            if (parts != null) {
                for (i in 0 until parts.length()) {
                    val partText = parts.optJSONObject(i)?.optString("text").orEmpty()
                    if (partText.isNotBlank()) {
                        if (textBuilder.isNotEmpty()) textBuilder.append("\n")
                        textBuilder.append(partText)
                    }
                }
            }

            val sources = mutableListOf<GroundingSource>()
            val groundingMetadata = firstCandidate?.optJSONObject("groundingMetadata")
            val groundingChunks = groundingMetadata?.optJSONArray("groundingChunks")
            if (groundingChunks != null) {
                for (i in 0 until groundingChunks.length()) {
                    val webObj = groundingChunks.optJSONObject(i)?.optJSONObject("web")
                    if (webObj != null) {
                        val uri = webObj.optString("uri")
                        val title = webObj.optString("title").ifBlank { uri }
                        if (uri.isNotBlank()) {
                            sources.add(GroundingSource(title = title, uri = uri))
                        }
                    }
                }
            }

            GeminiAssistantReply(
                text = textBuilder.toString().ifBlank { "No response text returned." },
                modelId = modelId,
                persona = personaTitle,
                groundingSources = sources.distinctBy { it.uri }
            )
        } catch (e: Exception) {
            GeminiAssistantReply(
                text = "Failed to parse Gemini response: ${e.message}",
                modelId = modelId,
                persona = personaTitle
            )
        }
    }
}
