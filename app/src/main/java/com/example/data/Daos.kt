package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks ORDER BY id ASC")
    fun getAllBookmarks(): Flow<List<Bookmark>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: Bookmark)

    @Delete
    suspend fun deleteBookmark(bookmark: Bookmark)

    @Query("SELECT COUNT(*) FROM bookmarks")
    suspend fun getCount(): Int
}

@Dao
interface BlackoutHistoryDao {
    @Query("SELECT * FROM blackout_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<BlackoutHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: BlackoutHistory)

    @Query("DELETE FROM blackout_history WHERE id = :id")
    suspend fun deleteHistoryById(id: Int)

    @Query("DELETE FROM blackout_history")
    suspend fun clearAllHistory()
}
