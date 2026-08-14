package com.urgentcall.guard

import android.content.Context
import io.michaelrocks.libphonenumber.android.PhoneNumberUtil
import io.michaelrocks.libphonenumber.android.Phonenumber.PhoneNumber

/**
 * Détermine si un appel manqué provient probablement d'un mobile, pour savoir
 * s'il est utile d'envoyer le SMS d'urgence (une ligne fixe ne peut pas
 * recevoir de SMS).
 *
 * Conçu de façon volontairement prudente : en cas de doute (parsing
 * impossible, type ambigu FIXED_LINE_OR_MOBILE comme aux USA/Canada où
 * mobiles et fixes partagent la même plage de numéros, ou pays non
 * sélectionné dans les réglages), on considère que c'est peut-être un mobile
 * et on envoie quand même le SMS : le coût d'un SMS envoyé "pour rien" à une
 * ligne fixe est nul, celui de rater une vraie urgence ne l'est pas.
 */
object PhoneNumberTypeHelper {

    @Volatile
    private var cachedUtil: PhoneNumberUtil? = null

    private fun util(context: Context): PhoneNumberUtil {
        cachedUtil?.let { return it }
        synchronized(this) {
            cachedUtil?.let { return it }
            val created = PhoneNumberUtil.createInstance(context.applicationContext)
            cachedUtil = created
            return created
        }
    }

    private enum class Kind { MOBILE, FIXED_LINE, AMBIGUOUS, UNKNOWN }

    fun isProbablyMobile(context: Context, rawNumber: String): Boolean {
        val trimmed = rawNumber.trim()
        if (trimmed.isEmpty()) return true // pas de numéro exploitable -> on envoie par prudence

        // Format international (+33...) : le pays se déduit de l'indicatif, pas besoin des réglages.
        if (trimmed.startsWith("+")) {
            val kind = classify(context, trimmed, null)
            return kind == null || kind != Kind.FIXED_LINE
        }

        // Format national (0612345678) : on teste chaque pays sélectionné dans les réglages.
        val regions = PreferencesHelper.getMobileDetectionCountries(context).ifEmpty { setOf("FR") }
        val results = regions.mapNotNull { region -> classify(context, trimmed, region) }

        if (results.isEmpty()) return true // aucun pays sélectionné n'a pu l'interpréter -> on envoie par prudence

        // Mobile dès qu'UN pays le considère mobile ou ambigu ; fixe seulement si TOUS le confirment fixe.
        return results.any { it != Kind.FIXED_LINE }
    }

    private fun classify(context: Context, number: String, region: String?): Kind? {
        return try {
            val phoneUtil = util(context)
            val parsed: PhoneNumber = phoneUtil.parse(number, region)
            if (!phoneUtil.isValidNumber(parsed)) return null
            when (phoneUtil.getNumberType(parsed)) {
                PhoneNumberUtil.PhoneNumberType.MOBILE -> Kind.MOBILE
                PhoneNumberUtil.PhoneNumberType.FIXED_LINE -> Kind.FIXED_LINE
                PhoneNumberUtil.PhoneNumberType.FIXED_LINE_OR_MOBILE -> Kind.AMBIGUOUS
                else -> Kind.UNKNOWN
            }
        } catch (e: Exception) {
            null
        }
    }
}
