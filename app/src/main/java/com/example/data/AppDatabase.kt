package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [Bookmark::class, BlackoutHistory::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun blackoutHistoryDao(): BlackoutHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "blackout_audio_db"
                )
                .addCallback(DatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    val bookmarkDao = database.bookmarkDao()
                    if (bookmarkDao.getCount() == 0) {
                        bookmarkDao.insertBookmark(
                            Bookmark(name = "YouTube Mobile", url = "https://m.youtube.com", iconType = "play")
                        )
                        bookmarkDao.insertBookmark(
                            Bookmark(name = "Twitch", url = "https://m.twitch.tv", iconType = "play")
                        )
                        bookmarkDao.insertBookmark(
                            Bookmark(name = "Vimeo", url = "https://vimeo.com", iconType = "play")
                        )
                        bookmarkDao.insertBookmark(
                            Bookmark(name = "SoundCloud", url = "https://m.soundcloud.com", iconType = "music")
                        )
                        bookmarkDao.insertBookmark(
                            Bookmark(name = "DailyMotion", url = "https://www.dailymotion.com", iconType = "play")
                        )
                    }
                }
            }
        }
    }
}
