package io.dupuis.zzzt.data.calendar

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import io.dupuis.zzzt.data.settings.SettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class AgendaEvent(
    val startMs: Long,
    val endMs: Long,
    val title: String,
    val calendarColor: Int?,
    val allDay: Boolean,
)

data class CalendarInfo(
    val id: Long,
    val displayName: String,
    val accountName: String,
    val color: Int?,
)

class CalendarRepository(
    private val context: Context,
    private val settingsStore: SettingsStore,
) {

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) ==
            PackageManager.PERMISSION_GRANTED

    suspend fun eventsForDayOf(epochMs: Long, zone: ZoneId = ZoneId.systemDefault()): List<AgendaEvent> =
        withContext(Dispatchers.IO) {
            if (!hasPermission()) return@withContext emptyList()
            val date = Instant.ofEpochMilli(epochMs).atZone(zone).toLocalDate()
            val selected = settingsStore.selectedCalendarIds.value
            query(date, zone, selected)
        }

    suspend fun listCalendars(): List<CalendarInfo> = withContext(Dispatchers.IO) {
        if (!hasPermission()) return@withContext emptyList()
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.CALENDAR_COLOR,
            CalendarContract.Calendars.VISIBLE,
        )
        val results = mutableListOf<CalendarInfo>()
        context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            "${CalendarContract.Calendars.VISIBLE} = 1",
            null,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME + " ASC",
        )?.use { cursor ->
            val iId = cursor.getColumnIndexOrThrow(CalendarContract.Calendars._ID)
            val iName = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
            val iAcct = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.ACCOUNT_NAME)
            val iColor = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_COLOR)
            while (cursor.moveToNext()) {
                results += CalendarInfo(
                    id = cursor.getLong(iId),
                    displayName = cursor.getString(iName) ?: "(unnamed)",
                    accountName = cursor.getString(iAcct) ?: "",
                    color = if (cursor.isNull(iColor)) null else cursor.getInt(iColor),
                )
            }
        }
        results
    }

    private fun query(date: LocalDate, zone: ZoneId, selectedIds: Set<Long>?): List<AgendaEvent> {
        val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon().run {
            ContentUris.appendId(this, start)
            ContentUris.appendId(this, end)
            build()
        }

        val projection = arrayOf(
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.CALENDAR_COLOR,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.CALENDAR_ID,
        )

        val selection: String?
        val selectionArgs: Array<String>?
        if (selectedIds == null) {
            selection = null
            selectionArgs = null
        } else if (selectedIds.isEmpty()) {
            return emptyList()
        } else {
            val placeholders = selectedIds.joinToString(",") { "?" }
            selection = "${CalendarContract.Instances.CALENDAR_ID} IN ($placeholders)"
            selectionArgs = selectedIds.map { it.toString() }.toTypedArray()
        }

        val results = mutableListOf<AgendaEvent>()
        context.contentResolver.query(
            uri,
            projection,
            selection,
            selectionArgs,
            CalendarContract.Instances.BEGIN + " ASC",
        )?.use { cursor ->
            val iBegin = cursor.getColumnIndexOrThrow(CalendarContract.Instances.BEGIN)
            val iEnd = cursor.getColumnIndexOrThrow(CalendarContract.Instances.END)
            val iTitle = cursor.getColumnIndexOrThrow(CalendarContract.Instances.TITLE)
            val iColor = cursor.getColumnIndexOrThrow(CalendarContract.Instances.CALENDAR_COLOR)
            val iAllDay = cursor.getColumnIndexOrThrow(CalendarContract.Instances.ALL_DAY)
            while (cursor.moveToNext()) {
                val title = cursor.getString(iTitle) ?: continue
                results += AgendaEvent(
                    startMs = cursor.getLong(iBegin),
                    endMs = cursor.getLong(iEnd),
                    title = title,
                    calendarColor = if (cursor.isNull(iColor)) null else cursor.getInt(iColor),
                    allDay = cursor.getInt(iAllDay) == 1,
                )
            }
        }
        return results
    }
}
