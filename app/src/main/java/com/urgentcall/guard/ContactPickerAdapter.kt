package com.urgentcall.guard

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton

class ContactPickerAdapter(
    private var contacts: List<ContactEntry>,
    private var alreadyAdded: Set<String>,
    private val onAdd: (ContactEntry) -> Unit
) : RecyclerView.Adapter<ContactPickerAdapter.ContactViewHolder>() {

    class ContactViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.pickerContactName)
        val phone: TextView = view.findViewById(R.id.pickerContactPhone)
        val addButton: MaterialButton = view.findViewById(R.id.pickerAddButton)
    }

    private fun normalize(number: String): String = number.filter { it.isDigit() }.takeLast(9)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_contact_picker, parent, false)
        return ContactViewHolder(view)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        val contact = contacts[position]
        holder.name.text = contact.name
        holder.phone.text = contact.phoneNumber

        val isAdded = normalize(contact.phoneNumber) in alreadyAdded
        holder.addButton.isEnabled = !isAdded
        holder.addButton.text = if (isAdded) {
            holder.itemView.context.getString(R.string.contacts_already_added)
        } else {
            holder.itemView.context.getString(R.string.contacts_add_action)
        }
        holder.addButton.setOnClickListener {
            if (!isAdded) onAdd(contact)
        }
    }

    override fun getItemCount(): Int = contacts.size

    fun updateData(newContacts: List<ContactEntry>, newAlreadyAdded: Set<String>) {
        contacts = newContacts
        alreadyAdded = newAlreadyAdded
        notifyDataSetChanged()
    }
}
