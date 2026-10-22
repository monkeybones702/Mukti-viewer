package com.example.data.repository

import com.example.data.model.CloudChatMessage
import com.example.data.model.CloudGridPreset
import com.example.data.model.GridPreset
import com.example.data.model.PresetJsonSerializer
import com.example.data.model.StreamHealthIncident
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class CloudSyncRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    // --- Cloud Grid Presets ---

    fun observeCloudPresets(userId: String = requireUserId()): Flow<List<CloudGridPreset>> = flow {
        val path = "users/$userId/presets"
        emitAll(
            db.collection("users").document(userId).collection("presets")
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.documents.mapNotNull { doc ->
                        doc.toObject(
                            CloudGridPreset::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    }
                }
                .catch { error ->
                    if (error is Exception) {
                        handleFirestoreError(error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun saveCloudPreset(preset: GridPreset): Result<String> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val cleanId = preset.id.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(120).ifBlank {
            "preset_${UUID.randomUUID().toString().take(8)}"
        }
        val windowCount = PresetJsonSerializer.decodeWindows(preset.windowsJson).size.coerceIn(1, 25)
        val cloudPreset = CloudGridPreset(
            id = cleanId,
            userId = uid,
            name = preset.name,
            description = preset.description,
            gridColumns = preset.gridColumns,
            gridRows = preset.gridRows,
            fitEntirePhone = preset.fitEntirePhone,
            proportionalZoom = preset.proportionalZoom,
            masterAudioMuted = preset.masterAudioMuted,
            windowCount = windowCount,
            windowsJson = preset.windowsJson
        )
        val docRef = db.collection("users").document(uid).collection("presets").document(cleanId)
        return try {
            docRef.set(cloudPreset.toCreateMap()).await()
            Result.success(cleanId)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun getCloudPresetById(ownerUserId: String, presetId: String): Result<CloudGridPreset?> {
        val docRef = db.collection("users").document(ownerUserId).collection("presets").document(presetId)
        return try {
            val snap = docRef.get().await()
            val item = snap.toObject(
                CloudGridPreset::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
            Result.success(item)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteCloudPreset(presetId: String): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val cleanId = presetId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(120)
        val docRef = db.collection("users").document(uid).collection("presets").document(cleanId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    // --- Stream Feed Health Incidents ---

    fun observeHealthIncidents(userId: String = requireUserId()): Flow<List<StreamHealthIncident>> = flow {
        val path = "users/$userId/health_logs"
        emitAll(
            db.collection("users").document(userId).collection("health_logs")
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.documents.mapNotNull { doc ->
                        doc.toObject(
                            StreamHealthIncident::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    }
                }
                .catch { error ->
                    if (error is Exception) {
                        handleFirestoreError(error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun logHealthIncident(
        windowId: String,
        windowTitle: String,
        streamUrl: String,
        status: String,
        reason: String,
        recoveryAction: String,
        attemptCount: Int,
        latencyMs: Int
    ): Result<String> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val logId = "hlth_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val incident = StreamHealthIncident(
            id = logId,
            userId = uid,
            windowId = windowId,
            windowTitle = windowTitle,
            streamUrl = streamUrl,
            status = status,
            reason = reason,
            recoveryAction = recoveryAction,
            attemptCount = attemptCount,
            latencyMs = latencyMs
        )
        val docRef = db.collection("users").document(uid).collection("health_logs").document(logId)
        return try {
            docRef.set(incident.toCreateMap()).await()
            Result.success(logId)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun getHealthIncidentById(ownerUserId: String, logId: String): Result<StreamHealthIncident?> {
        val docRef = db.collection("users").document(ownerUserId).collection("health_logs").document(logId)
        return try {
            val snap = docRef.get().await()
            Result.success(
                snap.toObject(
                    StreamHealthIncident::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            )
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            Result.failure(e)
        }
    }

    // --- Cloud AI Command Assistant Thread ---

    fun observeChatMessages(userId: String = requireUserId()): Flow<List<CloudChatMessage>> = flow {
        val path = "users/$userId/chat_messages"
        emitAll(
            db.collection("users").document(userId).collection("chat_messages")
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.documents.mapNotNull { doc ->
                        doc.toObject(
                            CloudChatMessage::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    }
                }
                .catch { error ->
                    if (error is Exception) {
                        handleFirestoreError(error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun saveChatMessage(
        role: String,
        content: String,
        modelId: String,
        agentPersona: String,
        sourcesSummary: String = ""
    ): Result<String> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val msgId = "msg_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val message = CloudChatMessage(
            id = msgId,
            userId = uid,
            role = role,
            content = content,
            modelId = modelId,
            agentPersona = agentPersona,
            sourcesSummary = sourcesSummary
        )
        val docRef = db.collection("users").document(uid).collection("chat_messages").document(msgId)
        return try {
            docRef.set(message.toCreateMap()).await()
            Result.success(msgId)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }
}
