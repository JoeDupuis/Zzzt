package io.dupuis.zzzt.di

import android.content.Context
import androidx.room.Room
import io.dupuis.zzzt.alarm.AlarmScheduler
import io.dupuis.zzzt.data.calendar.CalendarRepository
import io.dupuis.zzzt.data.db.AppDatabase
import io.dupuis.zzzt.data.repository.AlarmRepository
import io.dupuis.zzzt.data.repository.ClipRepository
import io.dupuis.zzzt.data.settings.SettingsStore
import io.dupuis.zzzt.player.PlayerController
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AppContainer(context: Context) {

    data class PendingClip(
        val sourceUrl: String,
        val title: String,
        val audioPath: String,
        val thumbnailPath: String,
        val durationMs: Long,
    )

    val appScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val clipsDir: File = File(context.filesDir, "clips").apply { mkdirs() }
    val thumbsDir: File = File(context.filesDir, "thumbs").apply { mkdirs() }

    val database: AppDatabase = Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java,
        "zzzt.db",
    )
        .addMigrations(AppDatabase.MIGRATION_1_2)
        .build()

    val clipRepository: ClipRepository = ClipRepository(
        dao = database.clipDao(),
        clipsDir = clipsDir,
        thumbsDir = thumbsDir,
    )

    val alarmRepository: AlarmRepository = AlarmRepository(database.alarmDao())

    val settingsStore: SettingsStore = SettingsStore(context.applicationContext)

    val calendarRepository: CalendarRepository = CalendarRepository(
        context = context.applicationContext,
        settingsStore = settingsStore,
    )

    val alarmScheduler: AlarmScheduler = AlarmScheduler(
        context = context.applicationContext,
        repository = alarmRepository,
        appScope = appScope,
    )

    val playerController: PlayerController = PlayerController(
        context = context.applicationContext,
        onPlaybackStart = { clipId ->
            appScope.launch(Dispatchers.IO) {
                clipRepository.markPlayed(clipId)
            }
        },
    )

    val pendingClips: MutableMap<String, PendingClip> = ConcurrentHashMap()

    init {
        alarmScheduler.reconcileAll()
    }
}
