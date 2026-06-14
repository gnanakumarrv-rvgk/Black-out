package com.example.data

import kotlinx.coroutines.flow.Flow

class AppRepository(private val db: AppDatabase) {
    val allBookmarks: Flow<List<Bookmark>> = db.bookmarkDao().getAllBookmarks()
    val allHistory: Flow<List<BlackoutHistory>> = db.blackoutHistoryDao().getAllHistory()

    suspend fun insertBookmark(bookmark: Bookmark) {
        db.bookmarkDao().insertBookmark(bookmark)
    }

    suspend fun deleteBookmark(bookmark: Bookmark) {
        db.bookmarkDao().deleteBookmark(bookmark)
    }

    suspend fun insertHistory(history: BlackoutHistory) {
        db.blackoutHistoryDao().insertHistory(history)
    }

    suspend fun deleteHistoryById(id: Int) {
        db.blackoutHistoryDao().deleteHistoryById(id)
    }

    suspend fun clearAllHistory() {
        db.blackoutHistoryDao().clearAllHistory()
    }
}
