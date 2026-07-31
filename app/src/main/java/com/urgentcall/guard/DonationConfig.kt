package com.urgentcall.guard

/**
 * Coordonnées de don du développeur.
 *
 * ⚠️ À COMPLÉTER : remplacez les valeurs ci-dessous par vos propres informations
 * avant de compiler. Sans cela, le bouton de don ouvrira Wero/PayPal à vide,
 * sans destinataire pré-rempli (voir explication dans le dialogue de don).
 */
object DonationConfig {
    /** Numéro de téléphone ou e-mail associé à votre compte Wero (pour l'affichage). */
    const val WERO_CONTACT = "TODO : votre numéro ou e-mail Wero"

    /** Votre identifiant PayPal.me, ex: "https://paypal.me/votrenom" — laissez vide si non utilisé. */
    const val PAYPAL_ME_URL = ""

    /** Package Android officiel de l'application Wero (EPI Company). */
    const val WERO_PACKAGE = "eu.epicompany.wero.wallet"
}
