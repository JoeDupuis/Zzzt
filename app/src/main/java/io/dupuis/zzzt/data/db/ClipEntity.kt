package io.dupuis.zzzt.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "clips")
data class ClipEntity(
    @PrimaryKey val id: String,
    val title: String,
    val sourceUrl: String,
    val audioPath: String,
    val thumbnailPath: String,
    val durationMs: Long,
    val trimStartMs: Long,
    val trimEndMs: Long,
    val createdAt: Long,
)
