package com.busly.trackingbus

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView


class BusResultAdapter(
    private val buses: List<String>,
    private val onClick: (String) -> Unit
) : RecyclerView.Adapter<BusResultAdapter.BusViewHolder>() {

    inner class BusViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val text: TextView = view.findViewById(android.R.id.text1)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BusViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_1, parent, false)
        return BusViewHolder(view)
    }

    override fun onBindViewHolder(holder: BusViewHolder, position: Int) {
        val busNumber = buses[position]
        holder.text.text = busNumber
        holder.itemView.setOnClickListener { onClick(busNumber) }
    }

    override fun getItemCount() = buses.size
}
