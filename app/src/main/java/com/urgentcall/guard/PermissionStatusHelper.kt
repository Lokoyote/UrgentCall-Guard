package com.urgentcall.guard

import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import androidx.core.content.ContextCompat

/**
 * Centralise la vérification de toutes les permissions et accès spéciaux
 * nécessaires au bon fonctionnement de l'application, avec un libellé
 * lisible pour chacune.
 */
object PermissionStatusHelper {

    data class PermissionStatus(
        val label: String,
        val granted: Boolean
    )

    /** Permissions runtime standard requises (hors accès spéciaux). */
    fun runtimePermissions(): Array<String> = buildList {
        add(android.Manifest.permission.READ_PHONE_STATE)
        add(android.Manifest.permission.SEND_SMS)
        add(android.Manifest.permission.READ_CONTACTS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }.toTypedArray()

    private fun label(context: Context, permission: String): String = when (permission) {
        android.Manifest.permission.READ_PHONE_STATE -> context.getString(R.string.perm_phone_state)
        android.Manifest.permission.SEND_SMS -> context.getString(R.string.perm_sms)
        android.Manifest.permission.READ_CONTACTS -> context.getString(R.string.perm_contacts)
        android.Manifest.permission.POST_NOTIFICATIONS -> context.getString(R.string.perm_notifications)
        else -> permission
    }

    fun isDndAccessGranted(context: Context): Boolean {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return notificationManager.isNotificationPolicyAccessGranted
    }

    fun isBatteryOptimizationIgnored(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    /** Liste complète et ordonnée de tous les statuts, pour affichage. */
    fun getAllStatuses(context: Context): List<PermissionStatus> {
        val list = mutableListOf<PermissionStatus>()

        runtimePermissions().forEach { permission ->
            val granted = ContextCompat.checkSelfPermission(context, permission) ==
                PackageManager.PERMISSION_GRANTED
            list.add(PermissionStatus(label(context, permission), granted))
        }

        list.add(PermissionStatus(context.getString(R.string.perm_dnd), isDndAccessGranted(context)))
        list.add(PermissionStatus(context.getString(R.string.perm_battery), isBatteryOptimizationIgnored(context)))

        return list
    }

    fun allGranted(context: Context): Boolean = getAllStatuses(context).all { it.granted }

    fun hasMissingRuntimePermissions(context: Context): Boolean =
        runtimePermissions().any {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
}
