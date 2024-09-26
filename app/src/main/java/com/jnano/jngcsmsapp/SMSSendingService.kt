package com.jnano.jngcsmsapp

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.jnano.jngcsmsapp.utils.AndroidHelpers
import com.jnano.jngcsmsapp.utils.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SMSSendingService : LifecycleService() {

    companion object {
        const val CHANNEL_ID = "sms_sending_channel"
        const val CHANNEL_NAME = "Notification du service d'envoi des SMS"
        const val SEND_SMS_BROADCAST_CODE = 321477
        const val SMS_SENT = "SMS_SENT"
        const val DESTROY_SELF = "DESTROY_SELF"
        const val NOTIFICATION_ID = 854
        const val NOTIFICATION_ID_INFO = 85412
    }

    private var list: List<Appointment>? = null;

    private var receiver: InnerServiceSMSReceiver? = null

    private var errorCount = 0

    private var totalCount = 0

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }


    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        receiver = InnerServiceSMSReceiver()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(
                receiver,
                IntentFilter(SMS_SENT),
                RECEIVER_NOT_EXPORTED
            )
        } else {
            registerReceiver(
                receiver,
                IntentFilter(SMS_SENT),
            )
        }
    }


    override fun onDestroy() {
        if (receiver != null) unregisterReceiver(receiver)
        Log.i("stop", "stopped the service lil bro")
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        super.onStartCommand(intent, flags, startId)

        errorCount = 0

        totalCount = 0

        if (intent?.action == DESTROY_SELF) {
            stopSelf()
            return START_STICKY
        }

        val notification = createProgressNotification()

        startForeground(NOTIFICATION_ID, notification)

        val date = intent?.getLongExtra("date", Date().time) ?: Date().time

        val allIncluded = intent?.getBooleanExtra("all", false) ?: false

        lifecycleScope.launch(Dispatchers.IO) {

            val datesPair = DateUtils.getStartOfDayAndTommorowForDate(Date(date))

            list = if(allIncluded) DBHelpers.getInstance(applicationContext).appointmentDAO()
                .getAllEntriesBetweenDates(datesPair.first, datesPair.second) else DBHelpers.getInstance(applicationContext).appointmentDAO()
                .getUnsentEntriesBetweenDates(datesPair.first, datesPair.second)

            Log.i("date_date", Date(date).toString())
            Log.i("date_list", list.toString())

            withContext(Dispatchers.Main) {

                list?.forEach {

                    try {

                        val sentPendingIntent = PendingIntent.getBroadcast(
                            applicationContext,
                            Date().time.toInt(),
                            Intent(SMS_SENT).apply {
                                putExtra("id", it.id)
                            },
                            PendingIntent.FLAG_IMMUTABLE
                        )

                        AndroidHelpers.sendSms(
                            it.patientPhone,
                            AndroidHelpers.generateSmsForAppointment(it),
                            sentPendingIntent,
                            sentPendingIntent
                        )

                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                }


            }

        }

        return START_STICKY
    }


    private fun createNotificationChannel() {
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT)
        )
    }

    private fun createProgressNotification(): Notification {

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Envoi des SMS en cours")
            .setProgress(list?.size ?: 0, 1, true)
            .setSmallIcon(R.drawable.sms)
            .addAction(
                R.drawable.download,
                "Annuler",
                PendingIntent.getService(
                    applicationContext,
                    120,
                    Intent(DESTROY_SELF),
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
            .build()

        return notification

    }


    private fun createSuccessNotification() {

        if (list == null || (list?.isEmpty() != false)) {
            Log.e("failure", "failed to call notification, the list is empty or null")
        }

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("Succès")
            .setContentText("Tout les ${list!!.size} SMS ont été envoyés avec succès!")
            .setSmallIcon(R.drawable.sms)
            .setContentIntent(
                PendingIntent.getActivity(
                    applicationContext,
                    14788,
                    Intent(this@SMSSendingService, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
            .build()

        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(
            NOTIFICATION_ID_INFO, notification
        )

    }


    private fun createErroredNotification() {
        if (list == null || (list?.isEmpty() != false)) {
            Log.e("failure", "failed to call notification, the list is empty or null")
        }

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("Information")
            .setContentText("${list!!.size - errorCount} SMS ont été envoyés, ${errorCount} n'ont pas été envoyés.")
            .setSmallIcon(R.drawable.sms)
            .setContentIntent(
                PendingIntent.getActivity(
                    applicationContext,
                    14788,
                    Intent(this@SMSSendingService, MainActivity::class.java),
                    PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            .build()

        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(
            NOTIFICATION_ID_INFO, notification
        )
    }

    inner class InnerServiceSMSReceiver : BroadcastReceiver() {

        override fun onReceive(p0: Context?, p1: Intent?) {

            Log.i("yeah", "yeah brother")

            val code = resultCode

            val id = p1?.getIntExtra("id", -1) ?: -1

            if (id == -1) {
                Log.e("error", "the id is -1 brother")
                return
            }

            totalCount++

            when (resultCode) {

                Activity.RESULT_OK -> {

                    lifecycleScope.launch(Dispatchers.IO) {


                        Log.i(
                            "send_status",
                            "the message with id ${
                                p1?.getIntExtra(
                                    "id",
                                    -1
                                ) ?: "unknown"
                            } has been sent with success"
                        )

                        val entry =
                            DBHelpers.getInstance(applicationContext).appointmentDAO()
                                .getEntryByID(id)

                        if(entry == null) {
                            Log.e("null", "entry is null brother")
                            return@launch
                        }

                        entry.sent = true

                        DBHelpers.getInstance(applicationContext).appointmentDAO()
                            .updateEntries(entry)

                    }

                }

                else -> {

                    errorCount++

                    Log.i(
                        "send_status",
                        "the message with id ${
                            p1?.getIntExtra(
                                "id",
                                -1
                            ) ?: "unknown"
                        } has not not not not been sent with success"
                    )
                }

            }

            if (totalCount == list!!.size) {

                sendBroadcast(Intent(MainActivity.UPDATE_DISPATCHER))

                stopForeground(STOP_FOREGROUND_REMOVE)

                if (errorCount > 0) createErroredNotification() else createSuccessNotification()

                stopSelf()

            }

        }

    }

}