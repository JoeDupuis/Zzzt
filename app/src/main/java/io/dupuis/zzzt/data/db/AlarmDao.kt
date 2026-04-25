package io.dupuis.zzzt.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AlarmDao {
    @Query("SELECT * FROM alarms ORDER BY hour, minute")
    fun observeAll(): Flow<List<AlarmEntity>>

    @Query("SELECT * FROM alarms WHERE starred = 1 ORDER BY hour, minute")
    fun observeStarred(): Flow<List<AlarmEntity>>

    @Query("SELECT * FROM alarms WHERE enabled = 1")
    fun getAllEnabled(): List<AlarmEntity>

    @Query("SELECT * FROM alarms WHERE id = :id LIMIT 1")
    fun getById(id: String): AlarmEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(alarm: AlarmEntity)

    @Update
    fun update(alarm: AlarmEntity)

    @Delete
    fun delete(alarm: AlarmEntity)

    @Query("DELETE FROM alarms WHERE id = :id")
    fun deleteById(id: String)
}
