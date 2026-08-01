package com.urgentcall.guard

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.TextView

/**
 * Adaptateur pour AutoCompleteTextView qui recherche en direct dans le
 * répertoire de contacts du téléphone (via ContactsHelper.searchContacts).
 */
class ContactAutoCompleteAdapter(context: Context) :
    ArrayAdapter<ContactEntry>(context, android.R.layout.simple_dropdown_item_1line) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView
            ?: LayoutInflater.from(context).inflate(android.R.layout.simple_dropdown_item_1line, parent, false)
        val contact = getItem(position)
        (view as TextView).text = if (contact != null) "${contact.name} — ${contact.phoneNumber}" else ""
        return view
    }

    override fun getFilter(): Filter = object : Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            val query = constraint?.toString().orEmpty()
            val results = if (query.isBlank()) emptyList() else ContactsHelper.searchContacts(context, query)
            return FilterResults().apply {
                values = results
                count = results.size
            }
        }

        @Suppress("UNCHECKED_CAST")
        override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
            clear()
            (results?.values as? List<ContactEntry>)?.let { addAll(it) }
            notifyDataSetChanged()
        }

        override fun convertResultToString(resultValue: Any?): CharSequence =
            (resultValue as? ContactEntry)?.name ?: ""
    }
}
