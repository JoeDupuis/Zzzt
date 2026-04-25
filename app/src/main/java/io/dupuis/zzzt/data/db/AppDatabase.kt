package io.dupuis.zzzt.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [ClipEntity::class, AlarmEntity::class],
    version = 2,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun clipDao(): ClipDao
    abstract fun alarmDao(): AlarmDao

    companion object {
        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE clips ADD COLUMN lastPlayedAt INTEGER")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS alarms (
                        id TEXT NOT NULL PRIMARY KEY,
                        hour INTEGER NOT NULL,
                        minute INTEGER NOT NULL,
                        label TEXT NOT NULL,
                        daysMask INTEGER NOT NULL,
                        enabled INTEGER NOT NULL,
                        starred INTEGER NOT NULL,
                        skipNextAtMs INTEGER,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }
    }
}
