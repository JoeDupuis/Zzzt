package io.dupuis.zzzt.data.repository

import io.dupuis.zzzt.data.db.ClipDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File

class ClipRepository(
    private val dao: ClipDao,
    private val clipsDir: File,
    private val thumbsDir: File,
) {
    fun observeAll(): Flow<List<Clip>> = dao.observeAll()
        .map { list -> list.map { it.toDomain() } }
        .flowOn(Dispatchers.IO)

    suspend fun getById(id: String): Clip? = withContext(Dispatchers.IO) {
        dao.getById(id)?.toDomain()
    }

    suspend fun insert(clip: Clip) = withContext(Dispatchers.IO) {
        dao.insert(clip.toEntity())
    }

    suspend fun update(clip: Clip) = withContext(Dispatchers.IO) {
        dao.update(clip.toEntity())
    }

    suspend fun delete(clip: Clip) = withContext(Dispatchers.IO) {
        dao.delete(clip.toEntity())
        runCatching { File(clip.audioPath).delete() }
        runCatching { File(clip.thumbnailPath).delete() }
        Unit
    }
}
