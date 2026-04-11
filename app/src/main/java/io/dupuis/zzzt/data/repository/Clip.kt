package io.dupuis.zzzt.data.repository

import io.dupuis.zzzt.data.db.ClipEntity

data class Clip(
    val id: String,
    val title: String,
    val sourceUrl: String,
    val audioPath: String,
    val thumbnailPath: String,
    val durationMs: Long,
    val trimStartMs: Long,
    val trimEndMs: Long,
    val createdAt: Long,
)

fun ClipEntity.toDomain(): Clip = Clip(
    id = id,
    title = title,
    sourceUrl = sourceUrl,
    audioPath = audioPath,
    thumbnailPath = thumbnailPath,
    durationMs = durationMs,
    trimStartMs = trimStartMs,
    trimEndMs = trimEndMs,
    createdAt = createdAt,
)

fun Clip.toEntity(): ClipEntity = ClipEntity(
    id = id,
    title = title,
    sourceUrl = sourceUrl,
    audioPath = audioPath,
    thumbnailPath = thumbnailPath,
    durationMs = durationMs,
    trimStartMs = trimStartMs,
    trimEndMs = trimEndMs,
    createdAt = createdAt,
)
