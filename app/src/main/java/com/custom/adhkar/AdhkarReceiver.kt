package com.custom.adhkar

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

class AdhkarReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prefs = context.getSharedPreferences("AdhkarPrefs", Context.MODE_PRIVATE)
        val isEnabled = prefs.getBoolean("ENABLED", false)
        if (!isEnabled) return

        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            scheduleAll(context)
            return
        }

        val scheduleType = intent.getIntExtra(EXTRA_SCHEDULE_TYPE, TYPE_HOURLY_00)
        val soundIndex = intent.getIntExtra(EXTRA_SOUND_INDEX, defaultSoundForType(scheduleType))

        val serviceIntent = Intent(context, AudioPlayerService::class.java).apply {
            putExtra("SOUND_INDEX", soundIndex)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }

        rescheduleSingle(context, scheduleType, soundIndex)
    }

    companion object {
        const val EXTRA_SOUND_INDEX = "EXTRA_SOUND_INDEX"
        const val EXTRA_SCHEDULE_TYPE = "EXTRA_SCHEDULE_TYPE"

        const val TYPE_HOURLY_00 = 1
        const val TYPE_HOURLY_15 = 2
        const val TYPE_DAILY_830_AM = 3
        const val TYPE_DAILY_830_PM = 4
        const val TYPE_DAILY_930_AM = 5
        const val TYPE_DAILY_930_PM = 6
        const val TYPE_DAILY_1030_AM = 7
        const val TYPE_DAILY_1030_PM = 8

        private val allTypes = listOf(
            TYPE_HOURLY_00,
            TYPE_HOURLY_15,
            TYPE_DAILY_830_AM,
            TYPE_DAILY_830_PM,
            TYPE_DAILY_930_AM,
            TYPE_DAILY_930_PM,
            TYPE_DAILY_1030_AM,
            TYPE_DAILY_1030_PM
        )

        private fun defaultSoundForType(scheduleType: Int): Int = when (scheduleType) {
            TYPE_HOURLY_00 -> 1
            TYPE_HOURLY_15 -> 2
            TYPE_DAILY_830_AM, TYPE_DAILY_830_PM -> 3
            TYPE_DAILY_930_AM, TYPE_DAILY_930_PM -> 4
            TYPE_DAILY_1030_AM, TYPE_DAILY_1030_PM -> 5
            else -> 1
        }

        fun scheduleAll(context: Context) {
            val prefs = context.getSharedPreferences("AdhkarPrefs", Context.MODE_PRIVATE)
            val firstSound = prefs.getInt("SELECTED_SOUND", 1)
            for (type in allTypes) {
                val sound = if (type == TYPE_HOURLY_00) firstSound else defaultSoundForType(type)
                rescheduleSingle(context, type, sound)
            }
        }

        fun rescheduleSingle(context: Context, scheduleType: Int, soundIndex: Int) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                return
            }

            val triggerAtMillis = computeNextTriggerMillis(scheduleType)
            val pendingIntent = buildPendingIntent(context, scheduleType, soundIndex)

            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }

        fun cancelSchedule(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            for (type in allTypes) {
                val sound = defaultSoundForType(type)
                val pendingIntent = buildPendingIntent(context, type, sound)
                alarmManager.cancel(pendingIntent)
            }
        }

        private fun buildPendingIntent(
            context: Context,
            scheduleType: Int,
            soundIndex: Int
        ): PendingIntent {
            val intent = Intent(context, AdhkarReceiver::class.java).apply {
                putExtra(EXTRA_SCHEDULE_TYPE, scheduleType)
                putExtra(EXTRA_SOUND_INDEX, soundIndex)
            }
            return PendingIntent.getBroadcast(
                context,
                scheduleType,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        private fun computeNextTriggerMillis(scheduleType: Int): Long {
            val now = System.currentTimeMillis()
            val cal = Calendar.getInstance().apply {
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            return when (scheduleType) {
                TYPE_HOURLY_00 -> {
                    cal.set(Calendar.MINUTE, 0)
                    if (cal.timeInMillis <= now) {
                        cal.add(Calendar.HOUR_OF_DAY, 1)
                    }
                    cal.timeInMillis
                }

                TYPE_HOURLY_15 -> {
                    cal.set(Calendar.MINUTE, 15)
                    if (cal.timeInMillis <= now) {
                        cal.add(Calendar.HOUR_OF_DAY, 1)
                    }
                    cal.timeInMillis
                }

                TYPE_DAILY_830_AM -> nextDailyMillis(cal, now, hourOfDay = 8, minute = 30)
                TYPE_DAILY_830_PM -> nextDailyMillis(cal, now, hourOfDay = 20, minute = 30)
                TYPE_DAILY_930_AM -> nextDailyMillis(cal, now, hourOfDay = 9, minute = 30)
                TYPE_DAILY_930_PM -> nextDailyMillis(cal, now, hourOfDay = 21, minute = 30)
                TYPE_DAILY_1030_AM -> nextDailyMillis(cal, now, hourOfDay = 10, minute = 30)
                TYPE_DAILY_1030_PM -> nextDailyMillis(cal, now, hourOfDay = 22, minute = 30)
                else -> {
                    cal.set(Calendar.MINUTE, 0)
                    if (cal.timeInMillis <= now) cal.add(Calendar.HOUR_OF_DAY, 1)
                    cal.timeInMillis
                }
            }
        }

        private fun nextDailyMillis(
            cal: Calendar,
            now: Long,
            hourOfDay: Int,
            minute: Int
        ): Long {
            cal.set(Calendar.HOUR_OF_DAY, hourOfDay)
            cal.set(Calendar.MINUTE, minute)
            if (cal.timeInMillis <= now) {
                cal.add(Calendar.DAY_OF_YEAR, 1)
            }
            return cal.timeInMillis
        }
    }
}
