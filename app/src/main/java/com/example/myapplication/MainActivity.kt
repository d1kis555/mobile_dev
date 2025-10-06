package com.example.myapplication

import android.os.Bundle
import android.widget.*
import androidx.activity.ComponentActivity
import com.example.myapplication.R
import java.util.Calendar

class MainActivity : ComponentActivity() {

    private lateinit var etFullName: EditText
    private lateinit var rgGender: RadioGroup
    private lateinit var rbMale: RadioButton
    private lateinit var rbFemale: RadioButton
    private lateinit var spCourse: Spinner
    private lateinit var seekDifficulty: SeekBar
    private lateinit var tvDifficultyLabel: TextView
    private lateinit var calendarView: CalendarView
    private lateinit var btnRegister: Button
    private lateinit var tvResult: TextView
    private lateinit var ivZodiac: ImageView

    private var selectedDay = 1
    private var selectedMonth = 0 // 0..11
    private var selectedYear = 2000

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Подключаем XML-разметку вместо Compose
        setContentView(R.layout.activity_main)

        // --- View-ссылки
        etFullName = findViewById(R.id.etFullName)
        rgGender = findViewById(R.id.rgGender)
        rbMale = findViewById(R.id.rbMale)
        rbFemale = findViewById(R.id.rbFemale)
        spCourse = findViewById(R.id.spCourse)
        seekDifficulty = findViewById(R.id.seekDifficulty)
        tvDifficultyLabel = findViewById(R.id.tvDifficultyLabel)
        calendarView = findViewById(R.id.calendarBirth)
        btnRegister = findViewById(R.id.btnRegister)
        tvResult = findViewById(R.id.tvResult)
        ivZodiac = findViewById(R.id.ivZodiac)

        // --- Spinner "Курс"
        ArrayAdapter.createFromResource(
            this,
            R.array.courses,                       // добавлен ранее в res/values/strings.xml
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spCourse.adapter = adapter
        }

        // --- SeekBar "Сложность" (1..10)
        tvDifficultyLabel.text = "Сложность: 1"
        seekDifficulty.max = 9
        seekDifficulty.progress = 0
        seekDifficulty.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val value = progress + 1
                tvDifficultyLabel.text = "Сложность: $value"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // --- CalendarView: запоминаем выбранную дату
        val cal = Calendar.getInstance().apply { timeInMillis = calendarView.date }
        selectedYear = cal.get(Calendar.YEAR)
        selectedMonth = cal.get(Calendar.MONTH)          // 0..11
        selectedDay = cal.get(Calendar.DAY_OF_MONTH)

        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            selectedYear = year
            selectedMonth = month
            selectedDay = dayOfMonth
        }

        // --- Кнопка "Зарегистрировать"
        btnRegister.setOnClickListener {
            val fullName = etFullName.text.toString().trim()
            val gender = when (rgGender.checkedRadioButtonId) {
                R.id.rbMale -> "Мужской"
                R.id.rbFemale -> "Женский"
                else -> "Не указан"
            }
            val course = spCourse.selectedItem?.toString() ?: "Не выбран"
            val difficulty = seekDifficulty.progress + 1

            val (zodiacName, zodiacDrawable) = getZodiac(selectedDay, selectedMonth)

            tvResult.text = """
                ФИО: $fullName
                Пол: $gender
                Курс: $course
                Сложность: $difficulty
                Дата рождения: %02d.%02d.%04d
                Знак зодиака: $zodiacName
            """.trimIndent().format(selectedDay, selectedMonth + 1, selectedYear)

            if (zodiacDrawable != null) {
                ivZodiac.setImageResource(zodiacDrawable)
            } else {
                ivZodiac.setImageDrawable(null) // если нет картинок — оставим пустым
            }
        }
    }

    /**
     * Возвращает (название знака, drawable-ресурс или null) по дате рождения.
     */
    private fun getZodiac(day: Int, monthZeroBased: Int): Pair<String, Int?> {
        val m = monthZeroBased + 1 // делаем 1..12

        val (name, key) = when {
            (m == 3  && day >= 21) || (m == 4 && day <= 19) -> "Овен"      to "zodiac_aries"
            (m == 4  && day >= 20) || (m == 5 && day <= 20) -> "Телец"     to "zodiac_taurus"
            (m == 5  && day >= 21) || (m == 6 && day <= 20) -> "Близнецы"  to "zodiac_gemini"
            (m == 6  && day >= 21) || (m == 7 && day <= 22) -> "Рак"       to "zodiac_cancer"
            (m == 7  && day >= 23) || (m == 8 && day <= 22) -> "Лев"       to "zodiac_leo"
            (m == 8  && day >= 23) || (m == 9 && day <= 22) -> "Дева"      to "zodiac_virgo"
            (m == 9  && day >= 23) || (m == 10 && day <= 22)-> "Весы"      to "zodiac_libra"
            (m == 10 && day >= 23) || (m == 11 && day <= 21)-> "Скорпион"  to "zodiac_scorpio"
            (m == 11 && day >= 22) || (m == 12 && day <= 21)-> "Стрелец"   to "zodiac_sagittarius"
            (m == 12 && day >= 22) || (m == 1 && day <= 19)-> "Козерог"    to "zodiac_capricorn"
            (m == 1  && day >= 20) || (m == 2 && day <= 18)-> "Водолей"    to "zodiac_aquarius"
            else                                               -> "Рыбы"     to "zodiac_pisces"
        }

        // Пытаемся найти ресурс по имени. Если картинок ещё нет — вернётся null.
        val resId = resources.getIdentifier(key, "drawable", packageName)
        return if (resId != 0) name to resId else name to null
    }
}
