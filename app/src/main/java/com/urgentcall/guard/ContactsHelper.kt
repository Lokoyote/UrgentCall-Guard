package com.urgentcall.guard

import android.content.Context
import android.provider.ContactsContract

data class ContactEntry(
    val name: String,
    val phoneNumber: String
)

/**
 * Accède au répertoire de contacts du téléphone (nécessite READ_CONTACTS) :
 * liste des favoris système, et recherche par nom.
 */
object ContactsHelper {

    private fun normalize(number: String): String = number.filter { it.isDigit() }.takeLast(9)

    /** Contacts marqués comme favoris ("étoilés") dans l'application Contacts du téléphone. */
    fun getFavoriteContacts(context: Context): List<ContactEntry> {
        val results = mutableListOf<ContactEntry>()
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )
        val selection = "${ContactsContract.Data.STARRED} = 1"

        try {
            context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                selection,
                null,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
            )?.use { cursor ->
                val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val seen = mutableSetOf<String>()
                while (cursor.moveToNext()) {
                    val name = cursor.getString(nameIdx) ?: continue
                    val number = cursor.getString(numberIdx) ?: continue
                    val key = normalize(number)
                    if (key.isNotEmpty() && seen.add(key)) {
                        results.add(ContactEntry(name, number))
                    }
                }
            }
        } catch (e: SecurityException) {
            // Permission READ_CONTACTS non accordée : retourne une liste vide.
        }
        return results
    }

    /** Recherche de contacts par nom dans tout le répertoire. */
    fun searchContacts(context: Context, query: String): List<ContactEntry> {
        if (query.isBlank()) return emptyList()
        val results = mutableListOf<ContactEntry>()
        val uri = android.net.Uri.withAppendedPath(
            ContactsContract.CommonDataKinds.Phone.CONTENT_FILTER_URI,
            android.net.Uri.encode(query)
        )
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )

        try {
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val seen = mutableSetOf<String>()
                while (cursor.moveToNext()) {
                    val name = cursor.getString(nameIdx) ?: continue
                    val number = cursor.getString(numberIdx) ?: continue
                    val key = normalize(number)
                    if (key.isNotEmpty() && seen.add(key)) {
                        results.add(ContactEntry(name, number))
                    }
                }
            }
        } catch (e: SecurityException) {
            // Permission READ_CONTACTS non accordée : retourne une liste vide.
        }
        return results
    }

    /** Vérifie si un numéro appartient à un contact favori du téléphone. */
    fun isSystemFavorite(context: Context, phoneNumber: String): Boolean {
        val target = normalize(phoneNumber)
        if (target.isEmpty()) return false
        return getFavoriteContacts(context).any { normalize(it.phoneNumber) == target }
    }
}
