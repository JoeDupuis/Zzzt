package io.dupuis.zzzt.ui.bedtime

import android.app.AlarmManager
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.dupuis.zzzt.alarm.AlarmScheduler
import io.dupuis.zzzt.data.calendar.AgendaEvent
import io.dupuis.zzzt.data.calendar.CalendarRepository
import io.dupuis.zzzt.data.repository.Alarm
import io.dupuis.zzzt.data.repository.AlarmRepository
import io.dupuis.zzzt.data.repository.Clip
import io.dupuis.zzzt.data.repository.ClipRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

data class BedtimeUiState(
    val nextAlarmMs: Long? = null,
    val nextAlarmLabel: String? = null,
    val starredAlarms: List<Alarm> = emptyList(),
    val lastPlayedClip: Clip? = null,
    val agendaDateMs: Long? = null,
    val agenda: List<AgendaEvent> = emptyList(),
    val calendarPermission: Boolean = false,
)

class BedtimeViewModel(
    private val appContext: Context,
    private val alarmRepository: AlarmRepository,
    private val clipRepository: ClipRepository,
    private val calendarRepository: CalendarRepository,
    private val alarmScheduler: AlarmScheduler,
) : ViewModel() {

    private val _state = MutableStateFlow(BedtimeUiState())
    val state: StateFlow<BedtimeUiState> = _state.asStateFlow()

    // Bumped whenever the UI resumes (or external state may have changed). Re-runs the
    // combine so systemNextAlarmClock() and calendar query get a fresh read even when
    // no Zzzt-owned DB row changed — e.g., user edited an alarm in the system Clock.
    private val refreshTicker = MutableStateFlow(0L)

    init {
        combine(
            alarmRepository.observeAll(),
            alarmRepository.observeStarred(),
            clipRepository.observeMostRecentlyPlayed(),
            refreshTicker,
        ) { all, starred, lastPlayed, _ ->
            val result = BedtimeNextAlarm.compute(
                alarms = all,
                systemNextMs = systemNextAlarmMs(appContext),
            )
            // Preserve the agenda across re-emissions that don't change the day being shown.
            // Without this, every unrelated DB emit (alarm toggle, clip markPlayed from play/pause)
            // wipes the agenda to empty before refreshAgenda() repopulates it — that's the flicker.
            val current = _state.value
            val keepAgenda = current.agendaDateMs == result.triggerMs
            BedtimeUiState(
                nextAlarmMs = result.triggerMs,
                nextAlarmLabel = result.label,
                starredAlarms = starred,
                lastPlayedClip = lastPlayed,
                agendaDateMs = result.triggerMs,
                agenda = if (keepAgenda) current.agenda else emptyList(),
                calendarPermission = calendarRepository.hasPermission(),
            )
        }.onEach { newState ->
            val previous = _state.value
            _state.value = newState
            val dateChanged = previous.agendaDateMs != newState.agendaDateMs
            val permissionChanged = previous.calendarPermission != newState.calendarPermission
            val firstLoad = previous.agenda.isEmpty() && newState.agenda.isEmpty() &&
                newState.calendarPermission
            if (dateChanged || permissionChanged || firstLoad) {
                refreshAgenda()
            }
        }.launchIn(viewModelScope)
    }

    fun refreshAgenda() {
        viewModelScope.launch(Dispatchers.IO) {
            val ms = _state.value.nextAlarmMs ?: System.currentTimeMillis()
            val hasPerm = calendarRepository.hasPermission()
            val events = if (hasPerm) calendarRepository.eventsForDayOf(ms) else emptyList()
            _state.value = _state.value.copy(agenda = events, calendarPermission = hasPerm)
        }
    }

    /**
     * Called when the Bedtime screen comes to the foreground. Re-reads the system's next
     * alarm (which may have changed in another app such as the Clock) and re-queries the
     * calendar (events may have been edited externally).
     */
    fun onForeground() {
        refreshTicker.value = System.currentTimeMillis()
        refreshAgenda()
    }

    fun toggleEnabled(alarmId: String, enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            if (enabled) {
                val updated = alarmRepository.setEnabled(alarmId, true) ?: return@launch
                alarmScheduler.schedule(updated)
            } else {
                // Order matters: cancel the system AlarmManager registration BEFORE the DB write.
                // Room emits on setEnabled(false), which re-runs the state combine — at that point
                // systemNextAlarmClock() must already reflect the cancel, otherwise the banner
                // shows a stale trigger time for an alarm that is now off.
                alarmScheduler.cancel(alarmId)
                alarmRepository.setEnabled(alarmId, false)
            }
        }
    }

    fun skipNext(alarmId: String) {
        alarmScheduler.skipNext(alarmId)
    }

    private fun systemNextAlarmMs(context: Context): Long? {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return am.nextAlarmClock?.triggerTime
    }

    companion object {
        fun factory(
            appContext: Context,
            alarmRepository: AlarmRepository,
            clipRepository: ClipRepository,
            calendarRepository: CalendarRepository,
            alarmScheduler: AlarmScheduler,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return BedtimeViewModel(
                    appContext = appContext,
                    alarmRepository = alarmRepository,
                    clipRepository = clipRepository,
                    calendarRepository = calendarRepository,
                    alarmScheduler = alarmScheduler,
                ) as T
            }
        }
    }
}
