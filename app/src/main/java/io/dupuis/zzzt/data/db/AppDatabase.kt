package io.dupuis.zzzt.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [ClipEntity::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun clipDao(): ClipDao
}
