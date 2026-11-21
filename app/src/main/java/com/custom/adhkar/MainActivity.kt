package com.custom.adhkar

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 90, 50, 50)
        }

        val title = TextView(this).apply {
            text = "منبه الأذكار المجدول"
            textSize = 21f
            setPadding(0, 0, 0, 14)
        }

        val scheduleInfo = TextView(this).apply {
            text = "ملف 1: رأس الساعة (:00) | ملف 2: الساعة وربع (:15)\nملف 3: 8:30 ص/م | ملف 4: 9:30 ص/م | ملف 5: 10:30 ص/م"
            textSize = 14f
            setPadding(0, 0, 0, 24)
        }

        val prefs = getSharedPreferences("AdhkarPrefs", Context.MODE_PRIVATE)
        val savedSound = prefs.getInt("SELECTED_SOUND", 1)
        val savedRotate = prefs.getBoolean("ROTATE_MODE", true)

        val chkRotate = CheckBox(this).apply {
            text = "تبديل الملف الأول تلقائياً كل ساعة"
            textSize = 16f
            isChecked = savedRotate
            setPadding(0, 0, 0, 20)
        }

        val radioGroup = RadioGroup(this)
        val names = listOf("الملف 1", "الملف 2", "الملف 3", "الملف 4", "الملف 5")

        names.forEachIndexed { index, name ->
            val rb = RadioButton(this).apply {
                text = name
                id = index + 1
                textSize = 17f
                setPadding(12, 14, 12, 14)
            }
            radioGroup.addView(rb)
            if (index + 1 == savedSound) radioGroup.check(rb.id)
        }

        val btnStart = Button(this).apply {
            text = "تفعيل جميع المواعيد المجدولة"
            textSize = 16f
        }

        val btnTest = Button(this).apply {
            text = "تجربة الصوت المختار الآن"
            textSize = 16f
        }

        val btnStop = Button(this).apply {
            text = "إيقاف التنبيهات"
            textSize = 16f
        }

        layout.addView(title)
        layout.addView(scheduleInfo)
        layout.addView(chkRotate)
        layout.addView(radioGroup)
        layout.addView(btnStart)
        layout.addView(btnTest)
        layout.addView(btnStop)
        setContentView(layout)

        requestPermissionsIfNeeded()

        btnStart.setOnClickListener {
            val selected = radioGroup.checkedRadioButtonId.takeIf { it in 1..5 } ?: 1
            prefs.edit()
                .putInt("SELECTED_SOUND", selected)
                .putBoolean("ROTATE_MODE", chkRotate.isChecked)
                .putBoolean("ENABLED", true)
                .apply()

            AdhkarReceiver.scheduleAll(this)
            Toast.makeText(this, "تم تفعيل جميع المواعيد المجدولة بنجاح", Toast.LENGTH_LONG).show()
        }

        btnTest.setOnClickListener {
            val selected = radioGroup.checkedRadioButtonId.takeIf { it in 1..5 } ?: 1
            val intent = Intent(this, AudioPlayerService::class.java).apply {
                putExtra("SOUND_INDEX", selected)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent) else startService(intent)
        }

        btnStop.setOnClickListener {
            prefs.edit().putBoolean("ENABLED", false).apply()
            AdhkarReceiver.cancelSchedule(this)
            stopService(Intent(this, AudioPlayerService::class.java))
            Toast.makeText(this, "تم إيقاف التنبيهات", Toast.LENGTH_SHORT).show()
        }
    }

    private fun requestPermissionsIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100)
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
            }
        }
    }
}
