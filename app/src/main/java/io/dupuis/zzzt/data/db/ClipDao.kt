package io.dupuis.zzzt.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ClipDao {
    @Query("SELECT * FROM clips ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<ClipEntity>>

    @Query("SELECT * FROM clips WHERE id = :id LIMIT 1")
    fun getById(id: String): ClipEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(clip: ClipEntity)

    @Update
    fun update(clip: ClipEntity)

    @Delete
    fun delete(clip: ClipEntity)
}
