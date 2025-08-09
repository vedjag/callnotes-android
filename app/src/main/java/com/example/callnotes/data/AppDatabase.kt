package com.example.callnotes.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [CallNote::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): CallNoteDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        fun get(ctx: Context) = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(ctx, AppDatabase::class.java, "callnotes.db")
                .build().also { INSTANCE = it }
        }
    }
}
