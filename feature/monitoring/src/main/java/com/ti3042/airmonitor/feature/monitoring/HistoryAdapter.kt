package com.ti3042.airmonitor.feature.monitoring

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/**
 * 🔄 Adaptador simple para el historial de datos
 */
class HistoryAdapter : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {
    
    private val items = mutableListOf<String>()
    
    fun submitList(newItems: List<String>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_1, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }
    
    override fun getItemCount(): Int = items.size
    
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val textView = view.findViewById<TextView>(android.R.id.text1)
        
        fun bind(item: String) {
            textView.text = item
        }
    }
}