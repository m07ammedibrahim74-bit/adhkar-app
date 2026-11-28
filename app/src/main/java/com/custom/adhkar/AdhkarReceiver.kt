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
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            rescheduleAllActive(context)
            return
        }
        val soundIndex = intent.getIntExtra("SOUND_INDEX", -1)
        if (soundIndex !in 1..5) return
        if (!prefs.getBoolean("ACTIVE_$soundIndex", false)) return

        val serviceIntent = Intent(context, AudioPlayerService::class.java).apply {
            putExtra("SOUND_INDEX", soundIndex)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
        scheduleForSound(context, soundIndex)
    }
    companion object {
        const val FREQ_15M = 0
        const val FREQ_30M = 1
        const val FREQ_1H = 2
        const val FREQ_DAILY = 3

        fun rescheduleAllActive(context: Context) {
            val prefs = context.getSharedPreferences("AdhkarPrefs", Context.MODE_PRIVATE)
            for (i in 1..5) {
                if (prefs.getBoolean("ACTIVE_$i", false)) {
                    scheduleForSound(context, i)
                }
            }
        }

        fun scheduleForSound(context: Context, soundIndex: Int) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) return

            val prefs = context.getSharedPreferences("AdhkarPrefs", Context.MODE_PRIVATE)
            val freq = prefs.getInt("FREQ_$soundIndex", FREQ_1H)
            val hour = prefs.getInt("HOUR_$soundIndex", 9)
            val minute = prefs.getInt("MIN_$soundIndex", 0)

            val triggerAt = computeNextTrigger(freq, hour, minute)
            val pi = buildPendingIntent(context, soundIndex)
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }
        fun cancelForSound(context: Context, soundIndex: Int) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(buildPendingIntent(context, soundIndex))
        }

        fun cancelAll(context: Context) {
            for (i in 1..5) cancelForSound(context, i)
        }

        private fun buildPendingIntent(context: Context, soundIndex: Int): PendingIntent {
            val intent = Intent(context, AdhkarReceiver::class.java).apply {
                putExtra("SOUND_INDEX", soundIndex)
            }
            return PendingIntent.getBroadcast(
                context, soundIndex, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        private fun computeNextTrigger(freq: Int, hour: Int, minute: Int): Long {
            val now = System.currentTimeMillis()
            val cal = Calendar.getInstance().apply {
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            return when (freq) {
                FREQ_15M -> now + 15 * 60 * 1000L
                FREQ_30M -> now + 30 * 60 * 1000L
                FREQ_1H -> now + 60 * 60 * 1000L
                FREQ_DAILY -> {
                    cal.set(Calendar.HOUR_OF_DAY, hour)
                    cal.set(Calendar.MINUTE, minute)
                    if (cal.timeInMillis <= now) cal.add(Calendar.DAY_OF_YEAR, 1)
                    cal.timeInMillis
                }
                else -> now + 60 * 60 * 1000L
            }
        }
    }
}
