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
        "يومياً - صباحاً فقط",
        "يومياً - مساءً فقط",
        "يومياً - صباحاً ومساءً"
    )
    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = 5894

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
        }
        val subtitle = android.widget.TextView(this).apply {
            text = "(تصميم محمد الدسوقي)"
            textSize = 14f
            gravity = android.view.Gravity.CENTER
            setTextColor(android.graphics.Color.parseColor("#E0E0E0"))
            setPadding(0, 4, 0, 24)
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
                layoutParams = android.widget.LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 18 }
            }
            val sw = android.widget.Switch(this).apply {
                text = name; textSize = 18f; setTextColor(android.graphics.Color.WHITE)
                isChecked = prefs.getBoolean("ACTIVE_$id", false)
            }
            switches.add(sw); card.addView(sw)
            val row = android.widget.LinearLayout(this).apply {
                orientation = android.widget.LinearLayout.HORIZONTAL
                setPadding(0, 14, 0, 0)
            }
            val btnTime = android.widget.Button(this).apply {
                text = formatScheduleText(prefs, id)
                textSize = 12f
                setTextColor(android.graphics.Color.WHITE)
                background = roundedBg("#1B5E20", 18f)
                layoutParams = android.widget.LinearLayout.LayoutParams(0, -2, 1.5f).apply { marginEnd = 12 }
            }
            val btnPlay = android.widget.Button(this).apply {
                text = "تجربة الصوت"
                textSize = 12f
                setTextColor(android.graphics.Color.WHITE)
                background = roundedBg("#2E7D32", 18f)
                layoutParams = android.widget.LinearLayout.LayoutParams(0, -2, 1f)
            }
            row.addView(btnTime); row.addView(btnPlay); card.addView(row)
            sw.setOnCheckedChangeListener { _, isChecked ->
                prefs.edit().putBoolean("ACTIVE_$id", isChecked).apply()
                if (isChecked) AdhkarReceiver.scheduleForSound(this, id)
                else AdhkarReceiver.cancelForSound(this, id)
            }
            btnPlay.setOnClickListener {
                val intent = android.content.Intent(this, AudioPlayerService::class.java).apply { putExtra("SOUND_INDEX", id) }
                if (android.os.Build.VERSION.SDK_INT >= 26) startForegroundService(intent) else startService(intent)
            }
            btnTime.setOnClickListener {
                android.app.AlertDialog.Builder(this)
                    .setTitle("موعد وتكرار: $name")
                    .setItems(freqLabels) { _, which ->
                        val h = prefs.getInt("HOUR_$id", 9)
                        val m = prefs.getInt("MIN_$id", 0)
                        android.app.TimePickerDialog(this, { _, selH, selM ->
                            saveSchedule(prefs, id, which, selH, selM, btnTime, sw)
                        }, h, m, false).show()
                    }.show()
            }
            root.addView(card)
        }
             val btnAbout = android.widget.Button(this).apply {
         text = "عن التطبيق"
         textSize = 16f
         setTextColor(android.graphics.Color.WHITE)
         background = roundedBg("#9905261D", 24f)
         layoutParams = android.widget.LinearLayout.LayoutParams(-1, -2).apply { topMargin = 8 }
         setOnClickListener {
             android.app.AlertDialog.Builder(this@MainActivity)
                 .setTitle("عن التطبيق: صل على الحبيب")
                 .setMessage("الإصدار: 2.0\n\nاللهم اجعل هذا العمل خالصاً لوجهك الكريم، واغفر لصاحبه ولمن ساهم فيه ولوالديه، وارزقهم شفاعة حبيبك المصطفى ﷺ.")
                 .setPositiveButton("آمين", null)
                 .setNegativeButton("رجوع", null)

                 .show()
         }
     }
     root.addView(btnAbout)

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
        val h = p.getInt("HOUR_$id", 9)
        val m = p.getInt("MIN_$id", 0)
        val label = freqLabels.getOrElse(freq) { "كل ساعة" }
        val h12 = if (h % 12 == 0) 12 else h % 12
        return String.format("%s (%02d:%02d)", label, h12, m)
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
