package com.urgentcall.guard

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class WhitelistAdapter(
    private val contacts: MutableList<WhitelistContact>,
    private val onDelete: (WhitelistContact) -> Unit
) : RecyclerView.Adapter<WhitelistAdapter.ContactViewHolder>() {

    class ContactViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.contactName)
        val phone: TextView = view.findViewById(R.id.contactPhone)
        val deleteButton: android.widget.Button = view.findViewById(R.id.deleteContactButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_whitelist_contact, parent, false)
        return ContactViewHolder(view)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        val contact = contacts[position]
        holder.name.text = if (contact.isSystemFavorite) "⭐ ${contact.name}" else contact.name
        holder.phone.text = contact.phoneNumber
        if (contact.isSystemFavorite) {
            // Non décochable individuellement : seul le switch "Inclure les favoris..."
            // permet de retirer ces entrées, toutes en même temps.
            holder.deleteButton.visibility = View.GONE
        } else {
            holder.deleteButton.visibility = View.VISIBLE
            holder.deleteButton.setOnClickListener { onDelete(contact) }
        }
    }

    override fun getItemCount(): Int = contacts.size

    fun updateData(newContacts: List<WhitelistContact>) {
        contacts.clear()
        contacts.addAll(newContacts)
        notifyDataSetChanged()
    }
}
