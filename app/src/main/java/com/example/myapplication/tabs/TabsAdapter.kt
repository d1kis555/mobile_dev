package com.example.myapplication.tabs

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.myapplication.ui.AuthorsFragment
import com.example.myapplication.ui.RegisterFragment
import com.example.myapplication.ui.RulesFragment
import com.example.myapplication.ui.SettingsFragment

class TabsAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
    override fun getItemCount() = 4
    override fun createFragment(position: Int): Fragment = when (position) {
        0 -> RegisterFragment()
        1 -> RulesFragment()
        2 -> AuthorsFragment()
        else -> SettingsFragment()
    }
}
