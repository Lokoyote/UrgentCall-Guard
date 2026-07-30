package com.urgentcall.guard

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class WhitelistContact(
    val name: String,
    val phoneNumber: String,
    val isPriority: Boolean
)

/**
 * Gère la liste blanche des contacts qui peuvent forcer la sonnerie
 * immédiatement, sans attendre le premier appel manqué + SMS.
 * Les numéros sont stockés normalisés (chiffres uniquement) pour la comparaison.
 */
object WhitelistHelper {

    private const val PREFS_NAME = "urgentcall_whitelist_v1"
    private const val KEY_CONTACTS = "contacts"

    private fun normalize(number: String): String =
        number.filter { it.isDigit() }.takeLast(9) // compare sur les 9 derniers chiffres (évite pb d'indicatif)

    fun getContacts(context: Context): List<WhitelistContact> {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_CONTACTS, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                WhitelistContact(
                    name = obj.getString("name"),
                    phoneNumber = obj.getString("phoneNumber"),
                    isPriority = obj.optBoolean("isPriority", true)
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveContacts(context: Context, contacts: List<WhitelistContact>) {
        val arr = JSONArray()
        contacts.forEach {
            val obj = JSONObject()
            obj.put("name", it.name)
            obj.put("phoneNumber", it.phoneNumber)
            obj.put("isPriority", it.isPriority)
            arr.put(obj)
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_CONTACTS, arr.toString()).apply()
    }

    fun addContact(context: Context, contact: WhitelistContact) {
        val updated = getContacts(context).toMutableList()
        updated.add(contact)
        saveContacts(context, updated)
    }

    fun removeContact(context: Context, phoneNumber: String) {
        val updated = getContacts(context).filterNot { normalize(it.phoneNumber) == normalize(phoneNumber) }
        saveContacts(context, updated)
    }

    fun isWhitelistedAndImmediate(context: Context, phoneNumber: String): Boolean {
        if (!PreferencesHelper.isImmediateWhitelistBreakthrough(context)) return false
        val target = normalize(phoneNumber)
        return getContacts(context).any { normalize(it.phoneNumber) == target }
    }
}
