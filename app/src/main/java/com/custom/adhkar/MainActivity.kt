package com.custom.adhkar

import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private val adhkarList = listOf(
        "الصلاة على النبي 1",
        "الصلاة على النبي 2",
        "الصلاة على النبي 3",
        "التسبيح 1",
        "التسبيح 2",
        "أحب الكلام إلى الله",
        "لا إله إلا الله",
        "سيد الاستغفار"
    )

    private val freqLabels = arrayOf(
        "كل 15 دقيقة",
        "كل 30 دقيقة",
        "كل ساعة",
        "يومياً في ساعة محددة"
    )
    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
                supportActionBar?.hide()
        if (android.os.Build.VERSION.SDK_INT >= 30) {
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.let {
                it.hide(android.view.WindowInsets.Type.statusBars() or android.view.WindowInsets.Type.navigationBars())
                it.systemBarsBehavior = android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
                or android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )
        }

        val prefs = getSharedPreferences("AdhkarPrefs", MODE_PRIVATE)

        val scroll = android.widget.ScrollView(this).apply {
            isFillViewport = true
            setBackgroundResource(R.drawable.background)
        }
        val root = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(40, 56, 40, 48)
        }
                val title = android.widget.TextView(this).apply {
            text = "صل على الحبيب"
            textSize = 28f
            gravity = android.view.Gravity.CENTER
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(android.graphics.Color.WHITE)
            setPadding(0, 0, 0, 4)
        }
        val subtitle = android.widget.TextView(this).apply {
            text = "(تصميم محمد الدسوقي)"
            textSize = 14f
            gravity = android.view.Gravity.CENTER
            setTextColor(android.graphics.Color.parseColor("#E0E0E0"))
            setPadding(0, 0, 0, 24)
        }
        root.addView(title)
        root.addView(subtitle)

        val switches = mutableListOf<android.widget.Switch>()

        adhkarList.forEachIndexed { idx, name ->
            val id = idx + 1
            val card = android.widget.LinearLayout(this).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                setPadding(28, 22, 28, 22)
                background = roundedBg("#9905261D", 24f)
                layoutParams = android.widget.LinearLayout.LayoutParams(-1, -2).apply {
                    bottomMargin = 18
                }
            }
            val sw = android.widget.Switch(this).apply {
                text = name
                textSize = 18f
                setTextColor(android.graphics.Color.WHITE)
                isChecked = prefs.getBoolean("ACTIVE_$id", false)
            }
            switches.add(sw)
            card.addView(sw)
            val row = android.widget.LinearLayout(this).apply {
                orientation = android.widget.LinearLayout.HORIZONTAL
                setPadding(0, 14, 0, 0)
            }
            val btnTime = android.widget.Button(this).apply {
                text = formatScheduleText(prefs, id)
                textSize = 13f
                setTextColor(android.graphics.Color.WHITE)
                background = roundedBg("#1B5E20", 18f)
                layoutParams = android.widget.LinearLayout.LayoutParams(0, -2, 1.4f).apply {
                    marginEnd = 12
                }
            }
            val btnPlay = android.widget.Button(this).apply {
                text = "تجربة الصوت"
                textSize = 13f
                setTextColor(android.graphics.Color.WHITE)
                background = roundedBg("#2E7D32", 18f)
                layoutParams = android.widget.LinearLayout.LayoutParams(0, -2, 1f)
            }
            row.addView(btnTime)
            row.addView(btnPlay)
            card.addView(row)
            sw.setOnCheckedChangeListener { _, isChecked ->
                prefs.edit().putBoolean("ACTIVE_$id", isChecked).apply()
                if (isChecked) {
                    AdhkarReceiver.scheduleForSound(this, id)
                } else {
                    AdhkarReceiver.cancelForSound(this, id)
                }
            }

            btnPlay.setOnClickListener {
                val intent = android.content.Intent(this, AudioPlayerService::class.java).apply {
                    putExtra("SOUND_INDEX", id)
                }
                if (android.os.Build.VERSION.SDK_INT >= 26) {
                    startForegroundService(intent)
                } else {
                    startService(intent)
                }
            }
            btnTime.setOnClickListener {
                android.app.AlertDialog.Builder(this)
                    .setTitle("موعد وتكرار: $name")
                    .setItems(freqLabels) { _, which ->
                        if (which == AdhkarReceiver.FREQ_DAILY) {
                            val h = prefs.getInt("HOUR_$id", 9)
                            val m = prefs.getInt("MIN_$id", 0)
                            android.app.TimePickerDialog(this, { _, selH, selM ->
                                saveSchedule(prefs, id, which, selH, selM, btnTime, sw)
                            }, h, m, false).show()
                        } else {
                            saveSchedule(prefs, id, which, 9, 0, btnTime, sw)
                        }
                    }.show()
            }
            root.addView(card)
        }
        val btnStopAll = android.widget.Button(this).apply {
            text = "إيقاف جميع التنبيهات"
            // ... إعدادات المظهر والخط للزر ...
            setOnClickListener {
                for (i in 1..8) prefs.edit().putBoolean("ACTIVE_$i", false).apply()
                switches.forEach { it.isChecked = false }
                AdhkarReceiver.cancelAll(this@MainActivity)
                stopService(android.content.Intent(this@MainActivity, AudioPlayerService::class.java))
            }
        }
        root.addView(btnStopAll)
        scroll.addView(root)
        setContentView(scroll)
        askPermissions()
    }
    private fun saveSchedule(p: android.content.SharedPreferences, id: Int, freq: Int, h: Int, m: Int, btn: android.widget.Button, sw: android.widget.Switch) {
        p.edit().putInt("FREQ_$id", freq).putInt("HOUR_$id", h).putInt("MIN_$id", m).putBoolean("ACTIVE_$id", true).apply()
        btn.text = formatScheduleText(p, id)
        sw.isChecked = true
        AdhkarReceiver.scheduleForSound(this, id)
    }
    private fun formatScheduleText(p: android.content.SharedPreferences, id: Int): String {
        val freq = p.getInt("FREQ_$id", AdhkarReceiver.FREQ_1H)
        return if (freq == AdhkarReceiver.FREQ_DAILY) {
            String.format("يومياً %02d:%02d", p.getInt("HOUR_$id", 9), p.getInt("MIN_$id", 0))
        } else {
            freqLabels.getOrElse(freq) { "كل ساعة" }
        }
    }
    private fun roundedBg(hex: String, r: Float) = android.graphics.drawable.GradientDrawable().apply {
        setColor(android.graphics.Color.parseColor(hex)); cornerRadius = r
    }

    private fun askPermissions() {
        if (android.os.Build.VERSION.SDK_INT >= 33) requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 100)
        if (android.os.Build.VERSION.SDK_INT >= 31) {
            val am = getSystemService(ALARM_SERVICE) as android.app.AlarmManager
            if (!am.canScheduleExactAlarms()) startActivity(android.content.Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
        }
    }
}
