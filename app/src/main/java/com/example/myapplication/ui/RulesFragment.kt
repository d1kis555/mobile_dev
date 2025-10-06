package com.example.myapplication.ui

import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.myapplication.R

class RulesFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, s: Bundle?): View {
        val v = inflater.inflate(R.layout.fragment_rules, container, false)
        v.findViewById<TextView>(R.id.textRules).text =
            Html.fromHtml(getString(R.string.rules_html), Html.FROM_HTML_MODE_COMPACT)
        return v
    }
}
