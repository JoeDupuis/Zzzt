package io.dupuis.zzzt.ui.alarms

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.dupuis.zzzt.alarm.AlarmScheduler
import io.dupuis.zzzt.data.repository.Alarm
import io.dupuis.zzzt.data.repository.AlarmRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class EditAlarmState(
    val id: String? = null,
    val hour: Int = 7,
    val minute: Int = 0,
    val label: String = "",
    val daysMask: Int = 0b0011111,
    val enabled: Boolean = true,
    val starred: Boolean = true,
    val loaded: Boolean = false,
    val saving: Boolean = false,
    val deleted: Boolean = false,
    val saved: Boolean = false,
)

class EditAlarmViewModel(
    private val alarmRepository: AlarmRepository,
    private val alarmScheduler: AlarmScheduler,
) : ViewModel() {

    private val _state = MutableStateFlow(EditAlarmState())
    val state: StateFlow<EditAlarmState> = _state.asStateFlow()

    fun load(id: String?) {
        if (_state.value.loaded && _state.value.id == id) return
        if (id == null) {
            _state.value = _state.value.copy(loaded = true, id = null)
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val a = alarmRepository.getById(id)
            if (a != null) {
                _state.value = EditAlarmState(
                    id = a.id,
                    hour = a.hour,
                    minute = a.minute,
                    label = a.label,
                    daysMask = a.daysMask,
                    enabled = a.enabled,
                    starred = a.starred,
                    loaded = true,
                )
            } else {
                _state.value = _state.value.copy(loaded = true)
            }
        }
    }

    fun setTime(hour: Int, minute: Int) {
        _state.value = _state.value.copy(hour = hour, minute = minute)
    }

    fun setLabel(label: String) {
        _state.value = _state.value.copy(label = label)
    }

    fun toggleDay(bit: Int) {
        val cur = _state.value.daysMask
        val next = cur xor (1 shl bit)
        _state.value = _state.value.copy(daysMask = next)
    }

    fun setStarred(on: Boolean) {
        _state.value = _state.value.copy(starred = on)
    }

    fun setEnabled(on: Boolean) {
        _state.value = _state.value.copy(enabled = on)
    }

    fun save() {
        val s = _state.value
        if (s.saving) return
        _state.value = s.copy(saving = true)
        viewModelScope.launch(Dispatchers.IO) {
            val id = s.id ?: UUID.randomUUID().toString()
            val alarm = Alarm(
                id = id,
                hour = s.hour,
                minute = s.minute,
                label = s.label.trim(),
                daysMask = s.daysMask,
                enabled = s.enabled,
                starred = s.starred,
                skipNextAtMs = null,
                createdAt = System.currentTimeMillis(),
            )
            alarmRepository.upsert(alarm)
            if (alarm.enabled) alarmScheduler.schedule(alarm) else alarmScheduler.cancel(alarm.id)
            _state.value = _state.value.copy(saving = false, saved = true, id = id)
        }
    }

    fun delete() {
        val id = _state.value.id ?: return
        viewModelScope.launch(Dispatchers.IO) {
            alarmScheduler.cancel(id)
            alarmRepository.delete(id)
            _state.value = _state.value.copy(deleted = true)
        }
    }

    companion object {
        fun factory(
            alarmRepository: AlarmRepository,
            alarmScheduler: AlarmScheduler,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                EditAlarmViewModel(alarmRepository, alarmScheduler) as T
        }
    }
}
