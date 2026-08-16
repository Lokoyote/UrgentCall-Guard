package com.urgentcall.guard

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.TextView
import java.text.Normalizer

/**
 * Adaptateur pour AutoCompleteTextView qui filtre en direct la liste des
 * pays (CountrySearchHelper) par nom, insensible à la casse et aux accents.
 */
class CountryAutoCompleteAdapter(
    context: Context,
    private val excludedCodes: () -> Set<String> = { emptySet() }
) : ArrayAdapter<CountryEntry>(context, android.R.layout.simple_dropdown_item_1line) {

    private val allEntries: List<CountryEntry> by lazy { CountrySearchHelper.allCountries(context) }

    private fun normalize(text: String): String =
        Normalizer.normalize(text, Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .lowercase()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView
            ?: LayoutInflater.from(context).inflate(android.R.layout.simple_dropdown_item_1line, parent, false)
        val entry = getItem(position)
        (view as TextView).text = entry?.label ?: ""
        return view
    }

    override fun getFilter(): Filter = object : Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            val query = normalize(constraint?.toString().orEmpty())
            val excluded = excludedCodes()
            val results = if (query.isBlank()) emptyList() else allEntries.filter { entry ->
                entry.code !in excluded && normalize(entry.name).contains(query)
            }
            return FilterResults().apply {
                values = results
                count = results.size
            }
        }

        @Suppress("UNCHECKED_CAST")
        override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
            clear()
            (results?.values as? List<CountryEntry>)?.let { addAll(it) }
            notifyDataSetChanged()
        }

        override fun convertResultToString(resultValue: Any?): CharSequence =
            (resultValue as? CountryEntry)?.label ?: ""
    }
}
