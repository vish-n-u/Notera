package com.example.devaudioreccordings.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [AudioTextDbData::class],
    version = 7
)
abstract class AudioTextDatabase:RoomDatabase() {
    abstract val dao: AudioTextDao
    companion object {
        @Volatile private var instance: AudioTextDatabase? = null

        fun getDatabase(context: Context): AudioTextDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AudioTextDatabase::class.java,
                    "audio_text_db"
                ).fallbackToDestructiveMigration()
                    .build().also { instance = it }
            }
    }
}