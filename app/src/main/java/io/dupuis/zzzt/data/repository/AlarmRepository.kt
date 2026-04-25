package io.dupuis.zzzt.data.repository

import io.dupuis.zzzt.data.db.AlarmDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class AlarmRepository(private val dao: AlarmDao) {
    fun observeAll(): Flow<List<Alarm>> = dao.observeAll()
        .map { list -> list.map { it.toDomain() } }
        .flowOn(Dispatchers.IO)

    fun observeStarred(): Flow<List<Alarm>> = dao.observeStarred()
        .map { list -> list.map { it.toDomain() } }
        .flowOn(Dispatchers.IO)

    suspend fun getById(id: String): Alarm? = withContext(Dispatchers.IO) {
        dao.getById(id)?.toDomain()
    }

    suspend fun allEnabled(): List<Alarm> = withContext(Dispatchers.IO) {
        dao.getAllEnabled().map { it.toDomain() }
    }

    suspend fun upsert(alarm: Alarm) = withContext(Dispatchers.IO) {
        dao.upsert(alarm.toEntity())
    }

    suspend fun delete(id: String) = withContext(Dispatchers.IO) {
        dao.deleteById(id)
    }

    suspend fun setEnabled(id: String, enabled: Boolean) = withContext(Dispatchers.IO) {
        val existing = dao.getById(id) ?: return@withContext null
        val updated = existing.copy(enabled = enabled, skipNextAtMs = null)
        dao.update(updated)
        updated.toDomain()
    }

    suspend fun setStarred(id: String, starred: Boolean) = withContext(Dispatchers.IO) {
        val existing = dao.getById(id) ?: return@withContext null
        val updated = existing.copy(starred = starred)
        dao.update(updated)
        updated.toDomain()
    }

    suspend fun setSkipNext(id: String, skipAtMs: Long?) = withContext(Dispatchers.IO) {
        val existing = dao.getById(id) ?: return@withContext null
        val updated = existing.copy(skipNextAtMs = skipAtMs)
        dao.update(updated)
        updated.toDomain()
    }
}
