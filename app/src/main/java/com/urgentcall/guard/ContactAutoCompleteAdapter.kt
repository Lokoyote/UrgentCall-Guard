package com.urgentcall.guard

import android.content.Context
import android.widget.ArrayAdapter
import android.widget.Filter

/**
 * Adaptateur pour AutoCompleteTextView qui recherche en direct dans le
 * répertoire de contacts du téléphone (via ContactsHelper.searchContacts).
 */
class ContactAutoCompleteAdapter(context: Context) :
    ArrayAdapter<ContactEntry>(context, android.R.layout.simple_dropdown_item_1line) {

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
