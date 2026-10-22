package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.GridPreset
import kotlinx.coroutines.flow.Flow

@Dao
interface GridPresetDao {
    @Query("SELECT * FROM grid_presets ORDER BY isBuiltIn DESC, createdAtMs DESC")
    fun getAllPresets(): Flow<List<GridPreset>>

    @Query("SELECT * FROM grid_presets WHERE id = :id LIMIT 1")
    suspend fun getPresetById(id: String): GridPreset?

    @Query("SELECT COUNT(*) FROM grid_presets")
    suspend fun getPresetCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreset(preset: GridPreset)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPresets(presets: List<GridPreset>)

    @Delete
    suspend fun deletePreset(preset: GridPreset)

    @Query("DELETE FROM grid_presets WHERE id = :id")
    suspend fun deletePresetById(id: String)
}
