package io.dupuis.zzzt.data.settings

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsStore(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _selectedCalendarIds = MutableStateFlow(readSelectedCalendarIds())
    val selectedCalendarIds: StateFlow<Set<Long>?> = _selectedCalendarIds.asStateFlow()

    fun setSelectedCalendarIds(ids: Set<Long>?) {
        prefs.edit().apply {
            if (ids == null) {
                remove(KEY_SELECTED_CALENDARS)
            } else {
                putString(KEY_SELECTED_CALENDARS, ids.joinToString(","))
            }
        }.apply()
        _selectedCalendarIds.value = ids
    }

    private fun readSelectedCalendarIds(): Set<Long>? {
        if (!prefs.contains(KEY_SELECTED_CALENDARS)) return null
        val raw = prefs.getString(KEY_SELECTED_CALENDARS, null) ?: return null
        if (raw.isEmpty()) return emptySet()
        return raw.split(',').mapNotNull { it.toLongOrNull() }.toSet()
    }

    companion object {
        private const val PREFS_NAME = "zzzt_settings"
        private const val KEY_SELECTED_CALENDARS = "selected_calendar_ids"
    }
}
