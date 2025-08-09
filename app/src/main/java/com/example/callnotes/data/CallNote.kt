package com.example.callnotes.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(
    tableName = "call_notes",
    indices = [Index(value = ["normalizedNumber"], unique = true)]
)
data class CallNote(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val rawNumber: String,
    val normalizedNumber: String,
    val company: String?,
    val subject: String?,
    val role: String?,
    val salary: String?,
    val noticePeriod: String?,
    val description: String?,
    val followUpNeeded: Boolean,
    val followUpAtMillis: Long?,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
