package io.dupuis.zzzt.ui.manage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.dupuis.zzzt.alarm.AlarmScheduler
import io.dupuis.zzzt.data.repository.Alarm
import io.dupuis.zzzt.data.repository.AlarmRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ManageAlarmsViewModel(
    private val alarmRepository: AlarmRepository,
    private val alarmScheduler: AlarmScheduler,
) : ViewModel() {

    val alarms: StateFlow<List<Alarm>> = alarmRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyList())

    fun setEnabled(id: String, enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = alarmRepository.setEnabled(id, enabled) ?: return@launch
            if (enabled) alarmScheduler.schedule(updated) else alarmScheduler.cancel(id)
        }
    }

    fun setStarred(id: String, starred: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            alarmRepository.setStarred(id, starred)
        }
    }

    companion object {
        fun factory(
            alarmRepository: AlarmRepository,
            alarmScheduler: AlarmScheduler,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ManageAlarmsViewModel(alarmRepository, alarmScheduler) as T
        }
    }
}
