package com.jnano.jngcsmsapp.utils

import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SmsManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.jnano.jngcsmsapp.Appointment
import com.jnano.jngcsmsapp.dataStore
import java.text.SimpleDateFormat
import java.util.Locale

object AndroidHelpers {

    fun sendSms(
        destination: String,
        message: String,
        sentPendingIntents: PendingIntent?,
        deliveredPendingIntent: PendingIntent?
    ) {
        SmsManager.getDefault()
            .sendTextMessage(destination, null, message, sentPendingIntents, deliveredPendingIntent)
    }

    fun isPermissionGrantedFor(context: Context, permission: String): Boolean {
        return context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    }

    suspend fun updateDataStoreValue(context: Context, key: String, newValue: String) {
        context.dataStore.edit { urlDataStore ->
            urlDataStore[stringPreferencesKey(key)] = newValue
        }
    }


    fun generateSmsForAppointment(appointment: Appointment): String {
        return "Rappel du Rendez-Vous pour le monsieur ${appointment.patientName} pour la date ${
            SimpleDateFormat(
                "dd/MM/yyyy",
                Locale.getDefault()
            ).format(appointment.date)
        } à ${
            SimpleDateFormat(
                "HH:mm",
                Locale.getDefault()
            ).format(appointment.date)
        }. Merci"
    }

}