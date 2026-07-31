package com.urgentcall.guard

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton

class WhitelistActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyText: TextView
    private lateinit var adapter: WhitelistAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_whitelist)
        title = getString(R.string.whitelist_title)

        recyclerView = findViewById(R.id.whitelistRecyclerView)
        emptyText = findViewById(R.id.emptyText)
        val addButton = findViewById<MaterialButton>(R.id.addContactButton)
        val addFromContactsButton = findViewById<MaterialButton>(R.id.addFromContactsButton)

        adapter = WhitelistAdapter(WhitelistHelper.getContacts(this).toMutableList()) { contact ->
            WhitelistHelper.removeContact(this, contact.phoneNumber)
            refreshList()
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        addButton.setOnClickListener { showAddContactDialog() }
        addFromContactsButton.setOnClickListener {
            startActivity(Intent(this, AddFromContactsActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        // Rafraîchit au retour de l'écran "Ajouter depuis les contacts"
        refreshList()
    }

    private fun refreshList() {
        val contacts = WhitelistHelper.getContacts(this)
        adapter.updateData(contacts)
        emptyText.visibility = if (contacts.isEmpty()) View.VISIBLE else View.GONE
        recyclerView.visibility = if (contacts.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun showAddContactDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_contact, null)
        val nameInput = dialogView.findViewById<EditText>(R.id.dialogNameInput)
        val phoneInput = dialogView.findViewById<EditText>(R.id.dialogPhoneInput)

        AlertDialog.Builder(this)
            .setTitle(R.string.whitelist_add_title)
            .setView(dialogView)
            .setPositiveButton(R.string.whitelist_add_button) { _, _ ->
                val name = nameInput.text.toString().trim()
                val phone = phoneInput.text.toString().trim()
                if (name.isNotEmpty() && phone.isNotEmpty()) {
                    WhitelistHelper.addContact(this, WhitelistContact(name, phone, isPriority = true))
                    refreshList()
                } else {
                    Toast.makeText(this, R.string.whitelist_add_title, Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.whitelist_cancel_button, null)
            .show()
    }
}
