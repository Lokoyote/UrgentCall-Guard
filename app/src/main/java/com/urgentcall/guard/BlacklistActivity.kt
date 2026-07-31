package com.urgentcall.guard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton

class BlacklistActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyText: TextView
    private lateinit var adapter: BlacklistAdapter
    private lateinit var blockHiddenSwitch: Switch
    private lateinit var blockUnknownSwitch: Switch

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_blacklist)
        title = getString(R.string.blacklist_title)

        recyclerView = findViewById(R.id.blacklistRecyclerView)
        emptyText = findViewById(R.id.emptyText)
        blockHiddenSwitch = findViewById(R.id.blockHiddenSwitch)
        blockUnknownSwitch = findViewById(R.id.blockUnknownSwitch)
        val addButton = findViewById<MaterialButton>(R.id.addButton)

        blockHiddenSwitch.isChecked = BlacklistHelper.isBlockHiddenNumbers(this)
        blockHiddenSwitch.setOnCheckedChangeListener { _, checked ->
            BlacklistHelper.setBlockHiddenNumbers(this, checked)
        }

        blockUnknownSwitch.isChecked = BlacklistHelper.isBlockUnknownNumbers(this)
        blockUnknownSwitch.setOnCheckedChangeListener { _, checked ->
            BlacklistHelper.setBlockUnknownNumbers(this, checked)
        }

        adapter = BlacklistAdapter(BlacklistHelper.getEntries(this).toMutableList()) { entry ->
            BlacklistHelper.removeEntry(this, entry.phoneNumber)
            refreshList()
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        addButton.setOnClickListener { showAddDialog() }

        refreshList()
    }

    private fun refreshList() {
        val entries = BlacklistHelper.getEntries(this)
        adapter.updateData(entries)
        emptyText.visibility = if (entries.isEmpty()) View.VISIBLE else View.GONE
        recyclerView.visibility = if (entries.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun showAddDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_contact, null)
        val nameInput = dialogView.findViewById<EditText>(R.id.dialogNameInput)
        val phoneInput = dialogView.findViewById<EditText>(R.id.dialogPhoneInput)
        nameInput.hint = getString(R.string.blacklist_label_hint)

        AlertDialog.Builder(this)
            .setTitle(R.string.blacklist_add_title)
            .setView(dialogView)
            .setPositiveButton(R.string.blacklist_add_button) { _, _ ->
                val label = nameInput.text.toString().trim().ifBlank { getString(R.string.blacklist_default_label) }
                val phone = phoneInput.text.toString().trim()
                if (phone.isNotEmpty()) {
                    BlacklistHelper.addEntry(this, BlacklistEntry(label, phone))
                    refreshList()
                } else {
                    Toast.makeText(this, R.string.whitelist_phone_hint, Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.whitelist_cancel_button, null)
            .show()
    }
}
