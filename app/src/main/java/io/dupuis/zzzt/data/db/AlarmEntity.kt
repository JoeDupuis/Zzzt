package io.dupuis.zzzt.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey val id: String,
    val hour: Int,
    val minute: Int,
    val label: String,
    val daysMask: Int,
    val enabled: Boolean,
    val starred: Boolean,
    val skipNextAtMs: Long?,
    val createdAt: Long,
)
