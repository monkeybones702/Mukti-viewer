package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.data.model.FeedType
import com.example.data.model.GridPreset
import com.example.data.model.LoginState
import com.example.data.model.StreamWindow

class Converters {
    @TypeConverter
    fun fromFeedType(value: FeedType): String = value.name

    @TypeConverter
    fun toFeedType(value: String): FeedType =
        runCatching { FeedType.valueOf(value) }.getOrDefault(FeedType.CUSTOM_WEB)

    @TypeConverter
    fun fromLoginState(value: LoginState): String = value.name

    @TypeConverter
    fun toLoginState(value: String): LoginState =
        runCatching { LoginState.valueOf(value) }.getOrDefault(LoginState.LOGGED_OUT)
}

@Database(
    entities = [StreamWindow::class, GridPreset::class],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun streamWindowDao(): StreamWindowDao
    abstract fun gridPresetDao(): GridPresetDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "streamsy_command_db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
