package com.jnano.jngcsmsapp

import android.Manifest
import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.DatabaseUtils
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.lifecycleScope
import com.jnano.jngcsmsapp.ui.theme.JnGcSMSAppTheme
import com.jnano.jngcsmsapp.utils.AndroidHelpers
import com.jnano.jngcsmsapp.utils.Constants
import com.jnano.jngcsmsapp.utils.DateUtils
import com.jnano.jngcsmsapp.utils.retrofit.RetrofitHelpers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "url")

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {

    private var receiver: Any? = null

    companion object {
        const val UPDATE_DISPATCHER = "UPDATE_DISPATCHER"
        const val SMS_SENT = "SMS_SENT"
    }

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->

            if (isGranted) {

                startService(
                    Intent(
                        this@MainActivity,
                        SMSSendingService::class.java
                    )
                )
            } else {
                Toast.makeText(
                    this@MainActivity,
                    "Vous devez accorder la permission de notification pour continuer!",
                    Toast.LENGTH_SHORT
                ).show()
            }

        }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

//        enableEdgeToEdge()

        if (!AndroidHelpers.isPermissionGrantedFor(this, android.Manifest.permission.SEND_SMS)) {

            Toast.makeText(this, "Permission not granted", Toast.LENGTH_SHORT).show()

            val smsPermissionResultLauncher =
                registerForActivityResult(contract = ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
                    if (isGranted) {
                        Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Permission Rejected lil jit", Toast.LENGTH_SHORT)
                            .show()
                    }
                }

            smsPermissionResultLauncher.launch(Manifest.permission.SEND_SMS)

        }

        setContent {
            JnGcSMSAppTheme {

                var urlState by remember {
                    mutableStateOf("")
                }

                var selectedDate by remember {
                    mutableStateOf(Date())
                }

                var list by remember {
                    mutableStateOf<List<Appointment>>(emptyList())
                }

                val dateState = rememberDatePickerState(
                    initialSelectedDateMillis = selectedDate.time,

                    )

                var showDatePickerDialog by remember {
                    mutableStateOf(false)
                }

                var loading by remember {
                    mutableStateOf(false)
                }

                var showConfirmationDialog by remember {
                    mutableStateOf(false)
                }

                var showSingleSMSConfirmationDialog by remember {
                    mutableStateOf(false)
                }

                var selectedAppointment by remember {
                    mutableStateOf<Appointment?>(null)
                }

                LaunchedEffect(key1 = selectedDate) {
                    loading = true
                    lifecycleScope.launch(Dispatchers.IO) {
                        val dates = DateUtils.getStartOfDayAndTommorowForDate(selectedDate)
                        val obj = DBHelpers.getInstance(applicationContext).appointmentDAO()
                            .getAllEntriesBetweenDates(
                                dates.first,
                                dates.second
                            )
                        withContext(Dispatchers.Main) {
                            loading = false
                            list = obj
                        }
                    }
                }

                LaunchedEffect(key1 = Unit) {
                    class UpdateBroadcastReceiver : BroadcastReceiver() {
                        override fun onReceive(p0: Context?, p1: Intent?) {

                            if(p1?.action == SMS_SENT) {

                                Log.i("wow", "yes inside sms sent stuff")

                                val id = p1.getIntExtra("id", -1)

                                when(resultCode) {
                                    RESULT_OK -> {
                                        Toast.makeText(this@MainActivity, "SMS envoyé avec succès!", Toast.LENGTH_LONG).show()
                                        lifecycleScope.launch(Dispatchers.IO) {

                                            val appointment = DBHelpers.getInstance(applicationContext)
                                                .appointmentDAO()
                                                .getEntryByID(id)

                                            if(appointment == null) {
                                                return@launch
                                            }

                                            appointment.sent = true

                                            DBHelpers.getInstance(applicationContext)
                                                .appointmentDAO()
                                                .updateEntries(appointment)

                                            val dates = DateUtils.getStartOfDayAndTommorowForDate(selectedDate)

                                            val obj = DBHelpers.getInstance(applicationContext).appointmentDAO()
                                                .getAllEntriesBetweenDates(
                                                    dates.first,
                                                    dates.second
                                                )

                                            withContext(Dispatchers.Main) {
                                                loading = false
                                                list = obj
                                            }

                                        }
                                    }
                                    else -> {
                                        Toast.makeText(this@MainActivity, "Une erreur est survenue lors de l'envoi du SMS", Toast.LENGTH_LONG).show()
                                    }
                                }

                                return

                            }

                            Log.i("Received", "Received the updating broadcast brother")

                            loading = true

                            lifecycleScope.launch(Dispatchers.IO) {
                                val dates = DateUtils.getStartOfDayAndTommorowForDate(selectedDate)
                                val obj = DBHelpers.getInstance(applicationContext).appointmentDAO()
                                    .getAllEntriesBetweenDates(
                                        dates.first,
                                        dates.second
                                    )
                                withContext(Dispatchers.Main) {
                                    loading = false
                                    list = obj
                                }
                            }

                        }

                    }

                    receiver = UpdateBroadcastReceiver()

                    Log.i("registering", "starting the registration brother")

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        registerReceiver(
                            receiver as UpdateBroadcastReceiver, IntentFilter(UPDATE_DISPATCHER).apply {
                                addAction(SMS_SENT)
                            },
                            RECEIVER_NOT_EXPORTED,
                        )
                    } else {
                        registerReceiver(
                            receiver as UpdateBroadcastReceiver, IntentFilter(UPDATE_DISPATCHER).apply {
                                addAction(SMS_SENT)
                            },
                        )
                    }

                    runBlocking {
                        Log.i("getting", "getting the datastore data lil bro")
                        urlState = dataStore.data.first()[stringPreferencesKey(Constants.URL_KEY)]
                            ?: "http://192.168.1.3:8080"
                    }

                }

                Scaffold(modifier = Modifier.fillMaxSize(), floatingActionButton = {
                    FloatingActionButton(
                        onClick = {
                            if (list.isNotEmpty()) showConfirmationDialog = true
                            else Toast.makeText(
                                this@MainActivity,
                                "Aucun rendez-vous importé",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                    ) {
                        Icon(
                            Icons.AutoMirrored.Outlined.Send,
                            contentDescription = "Envoyer les sms"
                        )
                    }
                }, floatingActionButtonPosition = FabPosition.End) { _innerpadding ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "Utilitaire d'envoi de SMS des Rendez-vous",
                            modifier = Modifier.padding(10.dp)
                        )
//                        Notes()
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = urlState,
                                onValueChange = { newText ->
                                    urlState = newText
                                },

                                placeholder = { Text(text = "Adresse Ip de l'ordinateur") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                                modifier = Modifier
                                    .fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                leadingIcon = {
                                    Icon(
                                        Icons.Outlined.Settings,
                                        contentDescription = "Lien du serveur"
                                    )
                                },
                                trailingIcon = {
                                    IconButton(
                                        onClick = {
                                            Log.i("Saving", "Saving brother")
                                            lifecycleScope.launch {
                                                try {
                                                    AndroidHelpers.updateDataStoreValue(
                                                        this@MainActivity,
                                                        Constants.URL_KEY,
                                                        urlState
                                                    )
                                                    Toast.makeText(
                                                        baseContext,
                                                        "Enregistrement terminé avec succès!",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                } catch (e: Exception) {
                                                    e.printStackTrace()
                                                    withContext(Dispatchers.Main) {
                                                        Toast.makeText(
                                                            baseContext,
                                                            "Une erreur est survenue!",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    }
                                                }
                                            }
                                        }, modifier = Modifier
                                            .wrapContentWidth()

                                    ) {
                                        Icon(
                                            Icons.Outlined.Check,
                                            contentDescription = "Enregistrer",
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                },
                            )

                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            Row(modifier = Modifier
                                .weight(1f)
                                .border(
                                    2.dp,
                                    color = TextFieldDefaults.colors().unfocusedContainerColor,
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .clip(RoundedCornerShape(20.dp))
                                .clickable {
                                    showDatePickerDialog = true
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp)

                            ) {
                                Icon(
                                    Icons.Outlined.DateRange,
                                    contentDescription = "Date selectionnée",
                                    modifier = Modifier.padding(end = 10.dp)
                                )
                                Text(
                                    text = SimpleDateFormat(
                                        "dd/MM/yyyy",
                                        Locale.getDefault()
                                    ).format(selectedDate)
                                )
                            }

                            Button(
                                onClick = {
                                    loading = true
                                    lifecycleScope.launch(Dispatchers.IO) {
                                        try {
                                            val result = RetrofitHelpers.getIntance(urlState)
                                                .getAppointments(selectedDate)
                                            Log.i("result", result.toString())
                                            if (result.error) throw Exception(result.message)
//                                            val available = DBHelpers.getInstance(applicationContext).appointmentDAO().getAppointmentsByIds(result.data.map { return@map it.id }.joinToString())
                                            val theList: List<Appointment> = result.data.map {
                                                val entryInDB = DBHelpers.getInstance(applicationContext).appointmentDAO().getEntryByID(it.id)
                                                if(entryInDB != null) it.sent = entryInDB.sent
                                                return@map it
                                            }
                                            DBHelpers.getInstance(applicationContext)
                                                .appointmentDAO()
                                                .upsertEntries(*theList.toTypedArray())
                                            withContext(Dispatchers.Main) {
                                                loading = false
                                                list = result.data
                                            }
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                            withContext(Dispatchers.Main) {
                                                loading = false
                                                Toast.makeText(
                                                    this@MainActivity,
                                                    "Erreur lors de la récupération des rendez-vous",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .wrapContentWidth()
                                    .padding(start = 10.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.download),
                                    contentDescription = "Importer",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(end = 5.dp)
                                )
                                Text(
                                    text = "Importer",
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }

                        if (loading) {
                            CircularProgressIndicator(modifier = Modifier.padding(top = 40.dp))
                        } else
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.padding(horizontal = 5.dp)
                            ) {
                                items(key = { list[it].id }, count = list.size) { itemIndex ->
                                    AppointmentItem(
                                        entry = list[itemIndex],
                                        onAppointmentClicked = {
                                            selectedAppointment = list[itemIndex]
                                            showSingleSMSConfirmationDialog = true
                                        })
                                }
                            }


                    }


                }

                if (showDatePickerDialog) {
                    DatePickerDialog(
                        onDismissRequest = {
                            showDatePickerDialog = false
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                dateState.selectedDateMillis?.let {
                                    selectedDate = Date(it)
                                }
                                showDatePickerDialog = false
                            }) {
                                Text(
                                    text = "Confirmer",
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }) {
                        DatePicker(
                            state = dateState,
                            showModeToggle = false,

                            )
                    }
                }

                if (showConfirmationDialog) {

                    if (list.isNotEmpty())
                        SendSMSAlertDialog(
                            onDismiss = {
                                Log.i("hiding", "hiding confirtmation dialog brother")
                                showConfirmationDialog = false
                            },
                            onConfirm = {

                                if (AndroidHelpers.isPermissionGrantedFor(
                                        this@MainActivity,
                                        Manifest.permission.POST_NOTIFICATIONS
                                    )
                                ) {
                                    startService(
                                        Intent(
                                            this@MainActivity,
                                            SMSSendingService::class.java
                                        ).apply {
                                            putExtra("date", selectedDate.time)
                                            putExtra("all", it)
                                        }
                                    )
                                } else {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                    startService(
                                        Intent(
                                            this@MainActivity,
                                            SMSSendingService::class.java
                                        ).apply {
                                            putExtra("date", selectedDate.time)
                                            putExtra("all", it)
                                        }
                                    )
                                }

                                showConfirmationDialog = false

                            }, size = list.size
                        )
                    else Toast.makeText(
                        this@MainActivity,
                        "Aucun rendez-vous importé!",
                        Toast.LENGTH_SHORT
                    ).show()

                }


                if (showSingleSMSConfirmationDialog && selectedAppointment != null) {
                    SendSingleSMSAlertDialog(
                        onDismiss = { showSingleSMSConfirmationDialog = false },
                        onConfirm = {
                            Log.i("clicked", "clicked brother")
                            AndroidHelpers.sendSms(
                                selectedAppointment!!.patientPhone,
                                AndroidHelpers.generateSmsForAppointment(selectedAppointment!!),
                                PendingIntent.getBroadcast(
                                    this@MainActivity,
                                    12544,
                                    Intent(SMS_SENT).apply {
                                        Log.i("id", selectedAppointment!!.id.toString())
                                        putExtra("id", selectedAppointment!!.id)
                                    },
                                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                                ),
                                null
                            )
                            showSingleSMSConfirmationDialog = false
                        },
                        item = selectedAppointment!!
                    )
                }


            }
        }

    }

    override fun onDestroy() {
        unregisterReceiver(receiver as BroadcastReceiver?)
        super.onDestroy()
    }


}


@Preview(showBackground = true)
@Composable
fun Notes() {
    JnGcSMSAppTheme {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.tertiaryContainer)
                .padding(10.dp)

        ) {
            Text(
                fontSize = TextUnit(12f, TextUnitType.Sp),
                text = "Assurez-vous que l'ordinateur duquel vous souhaitez aspirer les rendez-vous est allumé, le logiciel est activé et lancé, et que l'ordinateur et ce téléphone sont dans le même réseau local",
                fontWeight = FontWeight.Bold,
                lineHeight = TextUnit(18f, TextUnitType.Sp),
            )

        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppointmentItem(entry: Appointment, onAppointmentClicked: () -> Unit = {}) {
    JnGcSMSAppTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(
                    RoundedCornerShape(10.dp)
                )
                .background(MaterialTheme.colorScheme.secondaryContainer)
                .clickable(
                    indication = rememberRipple(bounded = true),
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = onAppointmentClicked,
                )
                .padding(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp), horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Person, contentDescription = "Patient",
                        modifier = Modifier
                            .padding(end = 10.dp)
                            .size(20.dp),
                    )
                    Text(text = entry.patientName, fontSize = TextUnit(13f, TextUnitType.Sp))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.DateRange, contentDescription = "Date",
                        modifier = Modifier
                            .padding(end = 10.dp)
                            .size(20.dp),
                    )
                    Text(
                        text = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("fr", "DZ")).format(
                            Date(
                                entry.date
                            )
                        ),
                        fontSize = TextUnit(13f, TextUnitType.Sp)
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp), horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Edit, contentDescription = "Patient",
                        modifier = Modifier
                            .padding(end = 10.dp)
                            .size(20.dp),
                    )
                    Text(
                        text = entry.note.ifEmpty { "Aucune Note" },
                        fontSize = TextUnit(13f, TextUnitType.Sp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }


                if (entry.sent) Text(
                    "Envoyé",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    fontStyle = FontStyle.Italic
                )


            }
        }
    }
}


@Composable
fun SendSMSAlertDialog(
    onDismiss: () -> Unit,
    onConfirm: (checkStatus: Boolean) -> Unit,
    size: Int = 0
) {

    var checked by remember {
        mutableStateOf(false)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(checked) }) {
                Text(text = "Envoyer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        },
        title = { Text("Confirmer") },
        text = {
            Column {
                Text(
                    "Vous allez envoyer $size messages SMS de rendez-vous pour vos patients.\nVotre Carte SIM par défaut sera utilisée pour cette action, continuer? ",
                    modifier = Modifier.padding(bottom = 20.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Tout Envoyer", modifier = Modifier.weight(1F))
                    Switch(
                        checked = checked,
                        onCheckedChange = { checked = it },
                        modifier = Modifier.scale(.7f)
                    )
                }
            }
        },
        icon = { Icon(painter = painterResource(id = R.drawable.sms), contentDescription = "SMS") },


        )
}


@Composable
fun SendSingleSMSAlertDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    item: Appointment
) {

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = "Envoyer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        },
        title = { Text("Confirmer") },
        text = {
            Text(
                "Voulez-vous Envoyer le SMS de rendez-vous pour ${item.patientName}?",
                modifier = Modifier.padding(bottom = 20.dp)
            )

        },
        icon = { Icon(painter = painterResource(id = R.drawable.sms), contentDescription = "SMS") },

        )
}

@Preview
@Composable
fun SendSingleSMSAlertDialog(

) {

    AlertDialog(
        onDismissRequest = { },
        confirmButton = {
            TextButton(onClick = {}) {
                Text(text = "Envoyer")
            }
        },
        dismissButton = {
            TextButton(onClick = {}) {
                Text("Annuler")
            }
        },
        title = { Text("Confirmer") },
        text = {
            Text(
                "Voulez-vous Envoyer le SMS de rendez-vous pour Captain Falcon?",
            )

        },
        icon = { Icon(painter = painterResource(id = R.drawable.sms), contentDescription = "SMS") },

        )
}


@Preview
@Composable
fun AppointmentItem() {
    JnGcSMSAppTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(
                    RoundedCornerShape(10.dp)
                )
                .background(MaterialTheme.colorScheme.secondaryContainer)
                .clickable(
                    indication = rememberRipple(bounded = true),
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = {})
                .padding(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp), horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Person, contentDescription = "Patient",
                        modifier = Modifier
                            .padding(end = 10.dp)
                            .size(20.dp),
                    )
                    Text(text = "Captain Falcon", fontSize = TextUnit(13f, TextUnitType.Sp))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.DateRange, contentDescription = "Date",
                        modifier = Modifier
                            .padding(end = 10.dp)
                            .size(20.dp),
                    )
                    Text(
                        text = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("fr", "DZ")).format(
                            Date(

                            )
                        ),
                        fontSize = TextUnit(13f, TextUnitType.Sp)
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Edit, contentDescription = "Patient",
                        modifier = Modifier
                            .padding(end = 10.dp)
                            .size(20.dp),
                    )
                    Text(
                        text = "Exemple note brother",
                        fontSize = TextUnit(13f, TextUnitType.Sp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text(
                    "Envoyé",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    fontStyle = FontStyle.Italic
                )


            }
        }
    }

}




