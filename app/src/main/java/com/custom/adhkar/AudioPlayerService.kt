package com.custom.adhkar

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class AudioPlayerService : Service() {
    private var mediaPlayer: MediaPlayer? = null
    private val channelId = "adhkar_exact_channel"

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val soundIndex = intent?.getIntExtra("SOUND_INDEX", 1) ?: 1
        createNotificationChannel()

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("أذكار الساعة")
            .setContentText("تشغيل الذكر المجدول...")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        startForeground(101, notification)
        playAudio(soundIndex)
        return START_NOT_STICKY
    }

    private fun playAudio(index: Int) {
        val resId = when (index) {
            1 -> R.raw.dhikr_1
            2 -> R.raw.dhikr_2
            3 -> R.raw.dhikr_3
            4 -> R.raw.dhikr_4
            5 -> R.raw.dhikr_5
            else -> R.raw.dhikr_1
        }

        mediaPlayer?.release()
        mediaPlayer = MediaPlayer.create(this, resId)?.apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .build()
            )
            setOnCompletionListener {
                stopSelf()
            }
            start()
        } ?: run {
            stopSelf()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "تنبيهات الأذكار الدقيقة",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                setSound(null, null)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        mediaPlayer?.release()
        mediaPlayer = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}