package com.urgentcall.guard

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class BlacklistAdapter(
    private val entries: MutableList<BlacklistEntry>,
    private val onDelete: (BlacklistEntry) -> Unit
) : RecyclerView.Adapter<BlacklistAdapter.EntryViewHolder>() {

    class EntryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.contactName)
        val phone: TextView = view.findViewById(R.id.contactPhone)
        val deleteButton: android.widget.Button = view.findViewById(R.id.deleteContactButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EntryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_whitelist_contact, parent, false)
        return EntryViewHolder(view)
    }

    override fun onBindViewHolder(holder: EntryViewHolder, position: Int) {
        val entry = entries[position]
        holder.name.text = entry.label
        holder.phone.text = entry.phoneNumber
        holder.deleteButton.setOnClickListener { onDelete(entry) }
    }

    override fun getItemCount(): Int = entries.size

    fun updateData(newEntries: List<BlacklistEntry>) {
        entries.clear()
        entries.addAll(newEntries)
        notifyDataSetChanged()
    }
}
