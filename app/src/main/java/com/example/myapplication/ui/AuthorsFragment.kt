package com.example.myapplication.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.myapplication.R

data class Author(val name: String, val photoRes: Int)

class AuthorsAdapter(private val items: List<Author>) : BaseAdapter() {
    override fun getCount() = items.size
    override fun getItem(i: Int) = items[i]
    override fun getItemId(i: Int) = i.toLong()

    override fun getView(i: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(parent.context)
            .inflate(R.layout.item_author, parent, false)
        val item = getItem(i)
        view.findViewById<TextView>(R.id.name).text = item.name
        view.findViewById<ImageView>(R.id.img).setImageResource(item.photoRes)
        return view
    }
}

class AuthorsFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, s: Bundle?): View {
        val v = inflater.inflate(R.layout.fragment_authors, container, false)
        val list = v.findViewById<ListView>(R.id.listAuthors)


        val authors = listOf(
            Author("Артём Баринов", R.drawable.artem),
            Author("Фёдор Симаков", R.drawable.fedya),
        )

        list.adapter = AuthorsAdapter(authors)
        return v
    }
}
