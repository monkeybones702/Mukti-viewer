package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.LoginState
import com.example.data.model.StreamWindow
import kotlinx.coroutines.flow.Flow

@Dao
interface StreamWindowDao {
    @Query("SELECT * FROM stream_windows ORDER BY isMainChoice DESC, orderIndex ASC")
    fun getAllWindows(): Flow<List<StreamWindow>>

    @Query("SELECT * FROM stream_windows ORDER BY isMainChoice DESC, orderIndex ASC")
    suspend fun getAllWindowsSync(): List<StreamWindow>

    @Query("SELECT COUNT(*) FROM stream_windows")
    suspend fun getWindowCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWindow(window: StreamWindow)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWindows(windows: List<StreamWindow>)

    @Update
    suspend fun updateWindow(window: StreamWindow)

    @Delete
    suspend fun deleteWindow(window: StreamWindow)

    @Query("DELETE FROM stream_windows")
    suspend fun deleteAllWindows()

    @Query("UPDATE stream_windows SET isMuted = :isMuted WHERE id = :id")
    suspend fun updateMuteState(id: String, isMuted: Boolean)

    @Query("UPDATE stream_windows SET isMuted = :isMuted")
    suspend fun setAllMuted(isMuted: Boolean)

    @Query("UPDATE stream_windows SET isAutoScrollEnabled = :enabled WHERE id = :id")
    suspend fun updateAutoScroll(id: String, enabled: Boolean)

    @Query("UPDATE stream_windows SET isAutoScrollEnabled = :enabled")
    suspend fun setAllAutoScroll(enabled: Boolean)

    @Query("UPDATE stream_windows SET loginState = :state WHERE id = :id")
    suspend fun updateLoginState(id: String, state: LoginState)

    @Query("UPDATE stream_windows SET isMainChoice = (CASE WHEN id = :id THEN 1 ELSE 0 END)")
    suspend fun setMainChoiceWindow(id: String)

    @Query(
        "UPDATE stream_windows SET zoomWidthCols = :cols, zoomHeightRows = :rows, " +
            "fitEntirePhone = :fitPhone, proportionalZoom = :proportional WHERE id = :id"
    )
    suspend fun updateWindowZoom(
        id: String,
        cols: Int,
        rows: Int,
        fitPhone: Boolean,
        proportional: Boolean
    )

    @Query(
        "UPDATE stream_windows SET zoomWidthCols = :cols, zoomHeightRows = :rows, " +
            "fitEntirePhone = :fitPhone, proportionalZoom = :proportional"
    )
    suspend fun setAllWindowsZoom(
        cols: Int,
        rows: Int,
        fitPhone: Boolean,
        proportional: Boolean
    )

    @Query(
        "UPDATE stream_windows SET healthStatus = :status, lastHeartbeatMs = :heartbeatMs, " +
            "lastRecoveryReason = :reason WHERE id = :id"
    )
    suspend fun updateHealthState(
        id: String,
        status: String,
        heartbeatMs: Long,
        reason: String
    )

    @Query(
        "UPDATE stream_windows SET healthStatus = :status, softRefreshCount = softRefreshCount + 1, " +
            "lastHeartbeatMs = :heartbeatMs, lastRecoveryReason = :reason WHERE id = :id"
    )
    suspend fun recordSoftRefresh(
        id: String,
        status: String,
        heartbeatMs: Long,
        reason: String
    )
}
