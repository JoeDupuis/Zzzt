package io.dupuis.zzzt.data.repository

import io.dupuis.zzzt.data.db.AlarmEntity

data class Alarm(
    val id: String,
    val hour: Int,
    val minute: Int,
    val label: String,
    val daysMask: Int,
    val enabled: Boolean,
    val starred: Boolean,
    val skipNextAtMs: Long?,
    val createdAt: Long,
) {
    val isOneShot: Boolean get() = daysMask == 0
}

fun AlarmEntity.toDomain(): Alarm = Alarm(
    id = id,
    hour = hour,
    minute = minute,
    label = label,
    daysMask = daysMask,
    enabled = enabled,
    starred = starred,
    skipNextAtMs = skipNextAtMs,
    createdAt = createdAt,
)

fun Alarm.toEntity(): AlarmEntity = AlarmEntity(
    id = id,
    hour = hour,
    minute = minute,
    label = label,
    daysMask = daysMask,
    enabled = enabled,
    starred = starred,
    skipNextAtMs = skipNextAtMs,
    createdAt = createdAt,
)
