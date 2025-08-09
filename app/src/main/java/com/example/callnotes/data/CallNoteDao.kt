package com.example.callnotes.data

import androidx.room.*

@Dao
interface CallNoteDao {
    @Query("SELECT * FROM call_notes WHERE normalizedNumber = :n LIMIT 1")
    suspend fun getByNumber(n: String): CallNote?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(note: CallNote): Long
}
