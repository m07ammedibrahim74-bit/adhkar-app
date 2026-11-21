package com.custom.adhkar

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    companion object {
        private const val PREFS_NAME = "AdhkarPrefs"
        private const val KEY_SELECTED_SOUND = "SELECTED_SOUND"
        private const val KEY_ROTATE_MODE = "ROTATE_MODE"
        private const val KEY_ENABLED = "ENABLED"
        private const val REQ_POST_NOTIFICATIONS = 100

        // يجب أن يتطابق ترتيب العناصر (1 إلى 5) مع ترتيب الأصوات في AudioPlayerService
        private val ADHKAR_OPTIONS = listOf(
            "الصلاة على النبي 1",
            "الصلاة على النبي 2",
            "التسبيح 1",
            "التسبيح 2",
            "أحب الكلام إلى الله"
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedSound = prefs.getInt(KEY_SELECTED_SOUND, 1).coerceIn(1, ADHKAR_OPTIONS.size)
        val savedRotate = prefs.getBoolean(KEY_ROTATE_MODE, true)
        val isEnabled = prefs.getBoolean(KEY_ENABLED, false)

        val scrollView = ScrollView(this).apply {
            fillViewport = true
            setBackgroundResource(R.drawable.bg_islamic)
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 84, 48, 56)
        }

        val textPrimary = Color.WHITE
        val accentMint = ColorStateList.valueOf(Color.parseColor("#A5D6A7"))
        val cardBg = Color.parseColor("#9905261D")

        val title = TextView(this).apply {
            text = "منبه الأذكار المجدول"
            textSize = 23f
            setTextColor(textPrimary)
            setPadding(0, 0, 0, 10)
        }

        val statusBadge = TextView(this).apply {
            text = if (isEnabled) {
                "الحالة: مفعّل • المختار: ${ADHKAR_OPTIONS[savedSound - 1]}"
            } else {
                "الحالة: متوقف • المختار: ${ADHKAR_OPTIONS[savedSound - 1]}"
            }
            textSize = 14f
            setTextColor(Color.parseColor("#E8F5E9"))
            setPadding(20, 12, 20, 12)
            background = createRoundedBackground(cardBg, 20f)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 20
            }
            layoutParams = params
        }

        val radioGroup = RadioGroup(this).apply {
            setPadding(18, 14, 18, 14)
            background = createRoundedBackground(cardBg, 24f)
        }

        ADHKAR_OPTIONS.forEachIndexed { index, label ->
            val soundIndex = index + 1
            val rb = RadioButton(this).apply {
                text = label
                id = soundIndex
                textSize = 17f
                setTextColor(textPrimary)
                buttonTintList = accentMint
                setPadding(12, 14, 12, 14)
            }
            radioGroup.addView(rb)
            if (soundIndex == savedSound) {
                radioGroup.check(rb.id)
            }
        }

        val btnStart = createActionButton("تفعيل جميع المواعيد المجدولة", "#1B5E20", topMargin = 24)
        val btnTest = createActionButton("تجربة الصوت المختار الآن", "#2E7D32", topMargin = 14)
        val btnStop = createActionButton("إيقاف التنبيهات", "#7F1D1D", topMargin = 14)

        layout.addView(title)
        layout.addView(statusBadge)
        layout.addView(radioGroup)
        layout.addView(btnStart)
        layout.addView(btnTest)
        layout.addView(btnStop)

        scrollView.addView(layout)
        setContentView(scrollView)

        requestPermissionsIfNeeded()

        btnStart.setOnClickListener {
            val selected = getSelectedSoundIndex(radioGroup)
            prefs.edit()
                .putInt(KEY_SELECTED_SOUND, selected)
                .putBoolean(KEY_ROTATE_MODE, savedRotate)
                .putBoolean(KEY_ENABLED, true)
                .apply()

            statusBadge.text = "الحالة: مفعّل • المختار: ${ADHKAR_OPTIONS[selected - 1]}"
            AdhkarReceiver.scheduleAll(this)
            Toast.makeText(this, "تم تفعيل جميع المواعيد المجدولة بنجاح", Toast.LENGTH_LONG).show()
        }

        btnTest.setOnClickListener {
            val selected = getSelectedSoundIndex(radioGroup)
            prefs.edit().putInt(KEY_SELECTED_SOUND, selected).apply()
            statusBadge.text = "الحالة: ${if (prefs.getBoolean(KEY_ENABLED, false)) "مفعّل" else "متوقف"} • المختار: ${ADHKAR_OPTIONS[selected - 1]}"

            val intent = Intent(this, AudioPlayerService::class.java).apply {
                putExtra("SOUND_INDEX", selected)
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(intent)
                } else {
                    startService(intent)
                }
            } catch (e: Exception) {
                Toast.makeText(this, "تعذر تشغيل الصوت حالياً", Toast.LENGTH_SHORT).show()
            }
        }

        btnStop.setOnClickListener {
            val selected = getSelectedSoundIndex(radioGroup)
            prefs.edit().putBoolean(KEY_ENABLED, false).apply()
            statusBadge.text = "الحالة: متوقف • المختار: ${ADHKAR_OPTIONS[selected - 1]}"

            AdhkarReceiver.cancelSchedule(this)
            try {
                stopService(Intent(this, AudioPlayerService::class.java))
            } catch (_: Exception) {
            }
            Toast.makeText(this, "تم إيقاف التنبيهات", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getSelectedSoundIndex(radioGroup: RadioGroup): Int {
        return radioGroup.checkedRadioButtonId.takeIf { it in 1..ADHKAR_OPTIONS.size } ?: 1
    }

    private fun createRoundedBackground(color: Int, radius: Float): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = radius
        }
    }

    private fun createActionButton(text: String, fillHex: String, topMargin: Int): Button {
        return Button(this).apply {
            this.text = text
            textSize = 16f
            setTextColor(Color.WHITE)
            background = createRoundedBackground(Color.parseColor(fillHex), 24f)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                this.topMargin = topMargin
            }
        }
    }

    private fun requestPermissionsIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQ_POST_NOTIFICATIONS)
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                try {
                    startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                } catch (_: Exception) {
                }
            }
        }
    }
}
