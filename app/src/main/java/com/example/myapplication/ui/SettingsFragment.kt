package com.example.myapplication.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.myapplication.R

class SettingsFragment : Fragment() {

    private val prefsName = "game_prefs"
    private val KEY_SPEED = "speed"
    private val KEY_MAX_BUGS = "max_bugs"
    private val KEY_BONUS_INTERVAL = "bonus_interval"
    private val KEY_ROUND_DURATION = "round_duration"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, s: Bundle?): View {
        val v = inflater.inflate(R.layout.fragment_settings, container, false)

        val seekSpeed = v.findViewById<SeekBar>(R.id.seekSpeed)
        val seekMax = v.findViewById<SeekBar>(R.id.seekMaxBugs)
        val seekBonus = v.findViewById<SeekBar>(R.id.seekBonusInterval)
        val seekRound = v.findViewById<SeekBar>(R.id.seekRoundDuration)

        val tvSpeed = v.findViewById<TextView>(R.id.tvSpeed)
        val tvMax = v.findViewById<TextView>(R.id.tvMaxBugs)
        val tvBonus = v.findViewById<TextView>(R.id.tvBonusInterval)
        val tvRound = v.findViewById<TextView>(R.id.tvRoundDuration)

        val p = requireContext().getSharedPreferences(prefsName, Context.MODE_PRIVATE)

        // Диапазоны: (храним реальные значения)
        // скорость: 1..10 ; макс тараканы: 1..50 ; бонусы: 5..60 сек ; раунд: 30..600 сек
        seekSpeed.max = 9
        seekMax.max = 49
        seekBonus.max = 55
        seekRound.max = 570

        fun setProgressesFromPrefs() {
            seekSpeed.progress = (p.getInt(KEY_SPEED, 5) - 1)
            seekMax.progress = (p.getInt(KEY_MAX_BUGS, 10) - 1)
            seekBonus.progress = (p.getInt(KEY_BONUS_INTERVAL, 15) - 5)
            seekRound.progress = (p.getInt(KEY_ROUND_DURATION, 120) - 30)
        }

        fun showValues() {
            tvSpeed.text = "${seekSpeed.progress + 1}"
            tvMax.text = "${seekMax.progress + 1}"
            tvBonus.text = "${seekBonus.progress + 5}"
            tvRound.text = "${seekRound.progress + 30}"
        }

        fun setListeners() {
            val editor = p.edit()
            seekSpeed.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, p0: Int, f: Boolean) { tvSpeed.text = "${p0 + 1}" }
                override fun onStartTrackingTouch(s: SeekBar?) {}
                override fun onStopTrackingTouch(s: SeekBar?) {
                    editor.putInt(KEY_SPEED, seekSpeed.progress + 1).apply()
                }
            })
            seekMax.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, p0: Int, f: Boolean) { tvMax.text = "${p0 + 1}" }
                override fun onStartTrackingTouch(s: SeekBar?) {}
                override fun onStopTrackingTouch(s: SeekBar?) {
                    editor.putInt(KEY_MAX_BUGS, seekMax.progress + 1).apply()
                }
            })
            seekBonus.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, p0: Int, f: Boolean) { tvBonus.text = "${p0 + 5}" }
                override fun onStartTrackingTouch(s: SeekBar?) {}
                override fun onStopTrackingTouch(s: SeekBar?) {
                    editor.putInt(KEY_BONUS_INTERVAL, seekBonus.progress + 5).apply()
                }
            })
            seekRound.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, p0: Int, f: Boolean) { tvRound.text = "${p0 + 30}" }
                override fun onStartTrackingTouch(s: SeekBar?) {}
                override fun onStopTrackingTouch(s: SeekBar?) {
                    editor.putInt(KEY_ROUND_DURATION, seekRound.progress + 30).apply()
                }
            })
        }

        setProgressesFromPrefs()
        showValues()
        setListeners()

        return v
    }
}
