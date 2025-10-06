package com.example.myapplication.ui

import android.os.Bundle
import android.text.Editable
import android.text.Html
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.example.myapplication.R
import java.util.Calendar

class RegisterFragment : Fragment() {

    private lateinit var etFullName: EditText
    private lateinit var tvFullNameError: TextView
    private lateinit var rgGender: RadioGroup
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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_register, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        etFullName = view.findViewById(R.id.etFullName)
        tvFullNameError = view.findViewById(R.id.tvFullNameError)
        rgGender = view.findViewById(R.id.rgGender)
        spCourse = view.findViewById(R.id.spCourse)
        seekDifficulty = view.findViewById(R.id.seekDifficulty)
        tvDifficultyLabel = view.findViewById(R.id.tvDifficultyLabel)
        calendarView = view.findViewById(R.id.calendarBirth)
        btnRegister = view.findViewById(R.id.btnRegister)
        tvResult = view.findViewById(R.id.tvResult)
        ivZodiac = view.findViewById(R.id.ivZodiac)

        ArrayAdapter.createFromResource(
            requireContext(), R.array.courses, android.R.layout.simple_spinner_item
        ).also { a ->
            a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spCourse.adapter = a
        }

        tvDifficultyLabel.text = "Сложность: 1"
        seekDifficulty.max = 9
        seekDifficulty.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, p: Int, f: Boolean) {
                tvDifficultyLabel.text = "Сложность: ${p + 1}"
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        val cal = Calendar.getInstance().apply { timeInMillis = calendarView.date }
        selectedYear = cal.get(Calendar.YEAR)
        selectedMonth = cal.get(Calendar.MONTH)
        selectedDay = cal.get(Calendar.DAY_OF_MONTH)
        calendarView.setOnDateChangeListener { _, y, m, d -> selectedYear = y; selectedMonth = m; selectedDay = d }

        etFullName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val t = s?.toString().orEmpty()
                val ok = t.isBlank() || isValidFullName(t)
                tvFullNameError.visibility = if (ok) View.GONE else View.VISIBLE
                if (!ok && t.isNotBlank()) {
                    tvFullNameError.text =
                        "ФИО введено некорректно. Нужно минимум два слова на русском, каждое с заглавной буквы."
                }
            }
        })

        btnRegister.setOnClickListener {
            val fullName = etFullName.text.toString().trim()
            if (!isValidFullName(fullName)) {
                tvFullNameError.visibility = View.VISIBLE
                tvFullNameError.text =
                    "ФИО введено некорректно. Нужно минимум два слова на русском, каждое с заглавной буквы."
                etFullName.error = "Проверьте формат ФИО"
                return@setOnClickListener
            } else {
                tvFullNameError.visibility = View.GONE
                etFullName.error = null
            }

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

            if (zodiacDrawable != null) ivZodiac.setImageResource(zodiacDrawable)
            else ivZodiac.setImageDrawable(null)
        }
    }

    private fun isValidFullName(input: String): Boolean {
        val words = input.trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }
        if (words.size < 2) return false
        val wordRegex = Regex("^[А-ЯЁ][а-яё]+(?:-[А-ЯЁ][а-яё]+)*$")
        return words.all { wordRegex.matches(it) }
    }

    private fun getZodiac(day: Int, monthZeroBased: Int): Pair<String, Int?> {
        val m = monthZeroBased + 1
        val (name, key) = when {
            (m == 3 && day >= 21) || (m == 4 && day <= 19)  -> "Овен" to "zodiac_aries"
            (m == 4 && day >= 20) || (m == 5 && day <= 20)  -> "Телец" to "zodiac_taurus"
            (m == 5 && day >= 21) || (m == 6 && day <= 20)  -> "Близнецы" to "zodiac_gemini"
            (m == 6 && day >= 21) || (m == 7 && day <= 22)  -> "Рак" to "zodiac_cancer"
            (m == 7 && day >= 23) || (m == 8 && day <= 22)  -> "Лев" to "zodiac_leo"
            (m == 8 && day >= 23) || (m == 9 && day <= 22)  -> "Дева" to "zodiac_virgo"
            (m == 9 && day >= 23) || (m == 10 && day <= 22) -> "Весы" to "zodiac_libra"
            (m == 10 && day >= 23) || (m == 11 && day <= 21)-> "Скорпион" to "zodiac_scorpio"
            (m == 11 && day >= 22) || (m == 12 && day <= 21)-> "Стрелец" to "zodiac_sagittarius"
            (m == 12 && day >= 22) || (m == 1 && day <= 19) -> "Козерог" to "zodiac_capricorn"
            (m == 1 && day >= 20) || (m == 2 && day <= 18)  -> "Водолей" to "zodiac_aquarius"
            else -> "Рыбы" to "zodiac_pisces"
        }
        val resId = resources.getIdentifier(key, "drawable", requireContext().packageName)
        return if (resId != 0) name to resId else name to null
    }
}
