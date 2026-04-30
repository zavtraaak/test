package com.devin.browser.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [HistoryEntry::class, BookmarkEntry::class],
    version = 1,
    exportSchema = false
)
abstract class BrowserDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao
    abstract fun bookmarkDao(): BookmarkDao

    companion object {
        fun create(context: Context): BrowserDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                BrowserDatabase::class.java,
                "browser.db"
            ).build()
    }
}
