package com.urgentcall.guard

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.tabs.TabLayout

/**
 * Écran unique avec deux onglets : Liste blanche et Liste noire.
 * Fusionne l'ancienne WhitelistActivity et l'ancienne BlacklistActivity.
 */
class ListsActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_INITIAL_TAB = "initial_tab"
        const val TAB_WHITELIST = 0
        const val TAB_BLACKLIST = 1
    }

    // --- Liste blanche ---
    private lateinit var whitelistTabContent: View
    private lateinit var whitelistRecyclerView: RecyclerView
    private lateinit var whitelistEmptyText: TextView
    private lateinit var whitelistAdapter: WhitelistAdapter

    // --- Liste noire ---
    private lateinit var blacklistTabContent: View
    private lateinit var blacklistRecyclerView: RecyclerView
    private lateinit var blacklistEmptyText: TextView
    private lateinit var blacklistAdapter: BlacklistAdapter
    private lateinit var blockHiddenSwitch: Switch
    private lateinit var blockUnknownSwitch: Switch

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lists)
        title = getString(R.string.lists_title)

        setupWhitelistTab()
        setupBlacklistTab()

        val tabLayout = findViewById<TabLayout>(R.id.listsTabLayout)
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) = showTab(tab.position)
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        val initialTab = intent.getIntExtra(EXTRA_INITIAL_TAB, TAB_WHITELIST)
        tabLayout.getTabAt(initialTab)?.select()
        showTab(initialTab)
    }

    override fun onResume() {
        super.onResume()
        // Rafraîchit au retour de l'écran "Ajouter depuis les contacts"
        refreshWhitelist()
    }

    private fun showTab(position: Int) {
        whitelistTabContent.visibility = if (position == TAB_WHITELIST) View.VISIBLE else View.GONE
        blacklistTabContent.visibility = if (position == TAB_BLACKLIST) View.VISIBLE else View.GONE
    }

    // ----------------------- Liste blanche -----------------------

    private fun setupWhitelistTab() {
        whitelistTabContent = findViewById(R.id.whitelistTabContent)
        whitelistRecyclerView = findViewById(R.id.whitelistRecyclerView)
        whitelistEmptyText = findViewById(R.id.whitelistEmptyText)
        val addButton = findViewById<MaterialButton>(R.id.addContactButton)
        val addFromContactsButton = findViewById<MaterialButton>(R.id.addFromContactsButton)

        whitelistAdapter = WhitelistAdapter(WhitelistHelper.getContacts(this).toMutableList()) { contact ->
            WhitelistHelper.removeContact(this, contact.phoneNumber)
            refreshWhitelist()
        }
        whitelistRecyclerView.layoutManager = LinearLayoutManager(this)
        whitelistRecyclerView.adapter = whitelistAdapter

        addButton.setOnClickListener { showAddWhitelistDialog() }
        addFromContactsButton.setOnClickListener {
            startActivity(Intent(this, AddFromContactsActivity::class.java))
        }
    }

    private fun refreshWhitelist() {
        val contacts = WhitelistHelper.getContacts(this)
        whitelistAdapter.updateData(contacts)
        whitelistEmptyText.visibility = if (contacts.isEmpty()) View.VISIBLE else View.GONE
        whitelistRecyclerView.visibility = if (contacts.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun showAddWhitelistDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_contact, null)
        val nameInput = dialogView.findViewById<AutoCompleteTextView>(R.id.dialogNameInput)
        val phoneInput = dialogView.findViewById<EditText>(R.id.dialogPhoneInput)
        setupContactAutocomplete(nameInput, phoneInput)

        AlertDialog.Builder(this)
            .setTitle(R.string.whitelist_add_title)
            .setView(dialogView)
            .setPositiveButton(R.string.whitelist_add_button) { _, _ ->
                val name = nameInput.text.toString().trim()
                val phone = phoneInput.text.toString().trim()
                if (name.isNotEmpty() && phone.isNotEmpty()) {
                    WhitelistHelper.addContact(this, WhitelistContact(name, phone, isPriority = true))
                    refreshWhitelist()
                } else {
                    Toast.makeText(this, R.string.whitelist_add_title, Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.whitelist_cancel_button, null)
            .show()
    }

    private fun setupContactAutocomplete(nameInput: AutoCompleteTextView, phoneInput: EditText) {
        nameInput.setAdapter(ContactAutoCompleteAdapter(this))
        nameInput.threshold = 1
        nameInput.setOnItemClickListener { parent, _, position, _ ->
            val selected = parent.getItemAtPosition(position) as ContactEntry
            nameInput.setText(selected.name)
            phoneInput.setText(selected.phoneNumber)
        }
    }

    // ----------------------- Liste noire -----------------------

    private fun setupBlacklistTab() {
        blacklistTabContent = findViewById(R.id.blacklistTabContent)
        blacklistRecyclerView = findViewById(R.id.blacklistRecyclerView)
        blacklistEmptyText = findViewById(R.id.blacklistEmptyText)
        blockHiddenSwitch = findViewById(R.id.blockHiddenSwitch)
        blockUnknownSwitch = findViewById(R.id.blockUnknownSwitch)
        val addButton = findViewById<MaterialButton>(R.id.blacklistAddButton)

        blockHiddenSwitch.isChecked = BlacklistHelper.isBlockHiddenNumbers(this)
        blockHiddenSwitch.setOnCheckedChangeListener { _, checked ->
            BlacklistHelper.setBlockHiddenNumbers(this, checked)
        }

        blockUnknownSwitch.isChecked = BlacklistHelper.isBlockUnknownNumbers(this)
        blockUnknownSwitch.setOnCheckedChangeListener { _, checked ->
            BlacklistHelper.setBlockUnknownNumbers(this, checked)
        }

        blacklistAdapter = BlacklistAdapter(BlacklistHelper.getEntries(this).toMutableList()) { entry ->
            BlacklistHelper.removeEntry(this, entry.phoneNumber)
            refreshBlacklist()
        }
        blacklistRecyclerView.layoutManager = LinearLayoutManager(this)
        blacklistRecyclerView.adapter = blacklistAdapter

        addButton.setOnClickListener { showAddBlacklistDialog() }

        refreshBlacklist()
    }

    private fun refreshBlacklist() {
        val entries = BlacklistHelper.getEntries(this)
        blacklistAdapter.updateData(entries)
        blacklistEmptyText.visibility = if (entries.isEmpty()) View.VISIBLE else View.GONE
        blacklistRecyclerView.visibility = if (entries.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun showAddBlacklistDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_contact, null)
        val nameInput = dialogView.findViewById<AutoCompleteTextView>(R.id.dialogNameInput)
        val phoneInput = dialogView.findViewById<EditText>(R.id.dialogPhoneInput)
        setupContactAutocomplete(nameInput, phoneInput)
        nameInput.hint = getString(R.string.blacklist_label_hint)

        AlertDialog.Builder(this)
            .setTitle(R.string.blacklist_add_title)
            .setView(dialogView)
            .setPositiveButton(R.string.blacklist_add_button) { _, _ ->
                val label = nameInput.text.toString().trim().ifBlank { getString(R.string.blacklist_default_label) }
                val phone = phoneInput.text.toString().trim()
                if (phone.isNotEmpty()) {
                    BlacklistHelper.addEntry(this, BlacklistEntry(label, phone))
                    refreshBlacklist()
                } else {
                    Toast.makeText(this, R.string.whitelist_phone_hint, Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.whitelist_cancel_button, null)
            .show()
    }
}
