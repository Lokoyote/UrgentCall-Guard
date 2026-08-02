package com.urgentcall.guard

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class AddFromContactsActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_TARGET = "target"
        const val TARGET_WHITELIST = "whitelist"
        const val TARGET_BLACKLIST = "blacklist"
    }

    private lateinit var searchInput: EditText
    private lateinit var sectionTitle: TextView
    private lateinit var emptyText: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ContactPickerAdapter
    private var target: String = TARGET_WHITELIST

    private fun normalize(number: String): String = number.filter { it.isDigit() }.takeLast(9)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_from_contacts)
        title = getString(R.string.contacts_picker_title)

        target = intent.getStringExtra(EXTRA_TARGET) ?: TARGET_WHITELIST

        searchInput = findViewById(R.id.searchInput)
        sectionTitle = findViewById(R.id.sectionTitle)
        emptyText = findViewById(R.id.emptyText)
        recyclerView = findViewById(R.id.contactsRecyclerView)

        adapter = ContactPickerAdapter(emptyList(), emptySet()) { contact ->
            if (target == TARGET_BLACKLIST) {
                BlacklistHelper.addEntry(this, BlacklistEntry(contact.name, contact.phoneNumber))
            } else {
                WhitelistHelper.addContact(this, WhitelistContact(contact.name, contact.phoneNumber, isPriority = true))
            }
            loadFavorites()
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString().orEmpty()
                if (query.isBlank()) loadFavorites() else runSearch(query)
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        loadFavorites()
    }

    private fun currentTargetKeys(): Set<String> =
        if (target == TARGET_BLACKLIST) {
            BlacklistHelper.getEntries(this).map { normalize(it.phoneNumber) }.toSet()
        } else {
            WhitelistHelper.getContacts(this).map { normalize(it.phoneNumber) }.toSet()
        }

    private fun loadFavorites() {
        sectionTitle.text = getString(R.string.contacts_favorites_section)
        val favorites = ContactsHelper.getFavoriteContacts(this)
        showResults(favorites, getString(R.string.contacts_empty_favorites))
    }

    private fun runSearch(query: String) {
        sectionTitle.text = getString(R.string.contacts_search_section)
        val results = ContactsHelper.searchContacts(this, query)
        showResults(results, getString(R.string.contacts_empty_search))
    }

    private fun showResults(contacts: List<ContactEntry>, emptyMessage: String) {
        adapter.updateData(contacts, currentTargetKeys())
        emptyText.text = emptyMessage
        emptyText.visibility = if (contacts.isEmpty()) View.VISIBLE else View.GONE
        recyclerView.visibility = if (contacts.isEmpty()) View.GONE else View.VISIBLE
    }
}
