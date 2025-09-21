package com.example.weight_trackerapp

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.weight_trackerapp.data.PinStore
import com.example.weight_trackerapp.data.WeightStore
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// ---- Model ----
data class WeightEntry(
    val id: Long = System.currentTimeMillis(),
    val date: String,
    val weightKg: Double,
    val note: String? = null
)

class MainActivity : ComponentActivity() {

    // Android 13+ needs runtime permission for notifications
    private val requestNotifPerm = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* we don't need the result here */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= 33) {
            requestNotifPerm.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            MaterialTheme { Surface(Modifier.fillMaxSize()) { AppRoot() } }
        }
    }
}

// ---- Notification helpers ----

fun scheduleDailyReminder(context: Context, hour: Int = 20, minute: Int = 0) {
    val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, ReminderReceiver::class.java)
    val pi = PendingIntent.getBroadcast(
        context,
        1234,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val cal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        if (before(Calendar.getInstance())) add(Calendar.DAY_OF_YEAR, 1)
    }

    am.setRepeating(
        AlarmManager.RTC_WAKEUP,
        cal.timeInMillis,
        AlarmManager.INTERVAL_DAY,
        pi
    )
}

fun cancelDailyReminder(context: Context) {
    val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, ReminderReceiver::class.java)
    val pi = PendingIntent.getBroadcast(
        context,
        1234, // must match scheduleDailyReminder
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    am.cancel(pi)
    // Optional: clear any shown notif IDs you used
    // NotificationManagerCompat.from(context).cancel(1001)
}

fun sendTestNotification(context: Context) {
    val channelId = "weight_reminders"
    val mgr = NotificationManagerCompat.from(context)
    val ch = NotificationChannelCompat.Builder(channelId, NotificationManagerCompat.IMPORTANCE_DEFAULT)
        .setName("Weight Reminders")
        .build()
    mgr.createNotificationChannel(ch)

    val notif = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(android.R.drawable.ic_menu_info_details)
        .setContentTitle("Test notification")
        .setContentText("This is a test — you’ll get a daily reminder at your scheduled time.")
        .setAutoCancel(true)
        .build()

    mgr.notify(2001, notif)
}

// ---- PIN gate then app ----

@Composable
private fun AppRoot() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val pinStore = remember(context) { PinStore(context) }
    val savedPin by pinStore.pinFlow.collectAsState(initial = null)
    val scope = rememberCoroutineScope()
    var unlocked by remember { mutableStateOf(false) }

    when {
        savedPin == null && !unlocked -> SetPinScreen { newPin ->
            scope.launch {
                pinStore.setPin(newPin)
                unlocked = true
            }
        }
        savedPin != null && !unlocked -> EnterPinScreen { entered ->
            if (entered == savedPin) unlocked = true
        }
        else -> WeightTrackerScreen()
    }
}

// ---- Main screen ----

@Composable
fun WeightTrackerScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val store = remember(context) { WeightStore(context) }
    val scope = rememberCoroutineScope()

    val items by store.weights.collectAsState(initial = emptyList())
    var showAdd by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = { FloatingActionButton(onClick = { showAdd = true }) { Text("+") } }
    ) { padding ->
        Column(Modifier.padding(padding)) {
            Text("Weight Tracker", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(16.dp))

            RemindersCard(
                onEnable = { scheduleDailyReminder(context, hour = 20, minute = 0) }, // 8:00 PM
                onTest = { sendTestNotification(context) },
                onDisable = { cancelDailyReminder(context) }
            )

            SummaryCard(items)
            Divider()

            LazyColumn(Modifier.fillMaxSize()) {
                items(items, key = { it.id }) { entry ->
                    WeightRow(
                        entry = entry,
                        onDelete = { scope.launch { store.delete(entry.id) } }
                    )
                    Divider()
                }
            }
        }
    }

    if (showAdd) {
        AddWeightDialog(
            onDismiss = { showAdd = false },
            onSave = { kg, note ->
                val item = WeightEntry(date = today(), weightKg = kg, note = note.ifBlank { null })
                scope.launch { store.add(item) }
                showAdd = false
            }
        )
    }
}

@Composable
private fun RemindersCard(
    onEnable: () -> Unit,
    onTest: () -> Unit,
    onDisable: () -> Unit
) {
    Card(
        Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Reminders", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onEnable,
                    modifier = Modifier.weight(1f)
                ) { Text("Enable daily 8:00 PM") }

                OutlinedButton(
                    onClick = onTest,
                    modifier = Modifier.weight(1f)
                ) { Text("Test now") }

                OutlinedButton(
                    onClick = onDisable,
                    modifier = Modifier.weight(1f)
                ) { Text("Disable") }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "Tap “Enable” to schedule a daily reminder. “Disable” cancels it.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun SummaryCard(items: List<WeightEntry>) {
    val latest = items.firstOrNull()
    val avg = items.takeIf { it.isNotEmpty() }?.map { it.weightKg }?.average()
    Card(Modifier.padding(16.dp).fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Overview", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text("Latest: " + (latest?.let { "${it.weightKg} kg on ${it.date}" } ?: "—"))
            Text("Average: " + (avg?.let { String.format(Locale.US, "%.1f kg", it) } ?: "—"))
        }
    }
}

@Composable
private fun WeightRow(entry: WeightEntry, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { }
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text("${entry.weightKg} kg", fontWeight = FontWeight.SemiBold)
            Text(entry.date, style = MaterialTheme.typography.bodySmall)
            entry.note?.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.height(4.dp))
                Text(it, style = MaterialTheme.typography.bodySmall)
            }
        }
        TextButton(onClick = onDelete) { Text("Delete") }
    }
}

@Composable
private fun AddWeightDialog(onDismiss: () -> Unit, onSave: (kg: Double, note: String) -> Unit) {
    var kgText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    val kg = kgText.toDoubleOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(enabled = kg != null, onClick = { onSave(kg!!, note) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text("Add weight") },
        text = {
            Column {
                OutlinedTextField(
                    value = kgText,
                    onValueChange = { kgText = it },
                    label = { Text("Weight (kg)") },
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (optional)") }
                )
            }
        }
    )
}

private fun today(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())