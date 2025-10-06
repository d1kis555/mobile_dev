package com.example.myapplication.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.data.Score

class RecordsAdapter : RecyclerView.Adapter<RecordsAdapter.ViewHolder>() {

    private var items: List<Score> = listOf()

    fun setItems(list: List<Score>) {
        items = list
        notifyDataSetChanged()
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvUserName)
        val tvPoints: TextView = view.findViewById(R.id.tvPoints)
        val tvDifficulty: TextView = view.findViewById(R.id.tvDifficulty)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_record, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val score = items[position]
        holder.tvName.text = score.userName
        holder.tvPoints.text = score.points.toString()
        holder.tvDifficulty.text = score.difficulty.toString()
    }

    override fun getItemCount(): Int = items.size
}
