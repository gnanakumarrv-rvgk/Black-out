package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookmarks")
data class Bookmark(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val url: String,
    val iconType: String = "globe" // "play", "music", "globe", etc.
)

@Entity(tableName = "blackout_history")
data class BlackoutHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val durationSeconds: Long,
    val triggerType: String, // "Proximity", "Face-down", "Manual"
    val energySavedEstWh: Double // simulated power saved
)
