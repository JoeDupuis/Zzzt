package io.dupuis.zzzt.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.dupuis.zzzt.data.calendar.CalendarInfo
import io.dupuis.zzzt.data.calendar.CalendarRepository
import io.dupuis.zzzt.data.settings.SettingsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val loading: Boolean = true,
    val hasPermission: Boolean = false,
    val calendars: List<CalendarInfo> = emptyList(),
    val selectedIds: Set<Long>? = null,
)

class SettingsViewModel(
    private val calendarRepository: CalendarRepository,
    private val settingsStore: SettingsStore,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val hasPerm = calendarRepository.hasPermission()
            val cals = if (hasPerm) calendarRepository.listCalendars() else emptyList()
            _state.value = SettingsUiState(
                loading = false,
                hasPermission = hasPerm,
                calendars = cals,
                selectedIds = settingsStore.selectedCalendarIds.value,
            )
        }
    }

    fun toggleCalendar(id: Long, checked: Boolean) {
        val current = _state.value
        val base = current.selectedIds ?: current.calendars.map { it.id }.toSet()
        val next = if (checked) base + id else base - id
        settingsStore.setSelectedCalendarIds(next)
        _state.value = current.copy(selectedIds = next)
    }

    fun showAllCalendars() {
        settingsStore.setSelectedCalendarIds(null)
        _state.value = _state.value.copy(selectedIds = null)
    }

    companion object {
        fun factory(
            calendarRepository: CalendarRepository,
            settingsStore: SettingsStore,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                SettingsViewModel(calendarRepository, settingsStore) as T
        }
    }
}
