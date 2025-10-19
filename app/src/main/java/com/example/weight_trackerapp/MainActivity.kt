@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.weight_trackerapp.data.PinStore
import com.example.weight_trackerapp.data.db.WeightDatabase
import com.example.weight_trackerapp.data.db.WeightEntity
import com.example.weight_trackerapp.util.computeStats
import com.example.weight_trackerapp.util.rollingAverage
import com.example.weight_trackerapp.util.trendArrow
import com.example.weight_trackerapp.util.trendSlope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Main activity: asks for notification permission on Android 13+,
 * creates the notification channel, then renders the app.
 */
class MainActivity : ComponentActivity() {

    // Android 13+ runtime permission for notifications
    private val notifPerm = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* nothing to do here */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= 33) {
            notifPerm.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        ReminderReceiver.ensureNotificationChannel(this)

        setContent {
            MaterialTheme {
                Surface(Modifier.fillMaxSize()) {
                    AppRoot()
                }
            }
        }
    }
}

/* ───────────────────────────── PIN gate (simple “auth”) ───────────────────────── */

@Composable
private fun AppRoot() {
    val context = LocalContext.current
    val pinStore = remember { PinStore(context) }
    val savedPin by pinStore.pinFlow.collectAsState(initial = null)
    var unlocked by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    when {
        // No pin yet? Ask the user to create one.
        savedPin == null && !unlocked -> SetPinScreen { newPin ->
            scope.launch { pinStore.setPin(newPin); unlocked = true }
        }
        // Pin exists but we haven't unlocked yet? Ask to enter.
        savedPin != null && !unlocked -> EnterPinScreen { entered ->
            if (entered == savedPin) unlocked = true
        }
        // Past the gate → show the app.
        else -> WeightTrackerScreen()
    }
}

/* ───────────────────────────── Unit helpers (UI conversion only) ───────────────── */

private enum class UnitSystem { KG, LB }
private const val KG_TO_LB = 2.20462262185
private fun kgToLb(kg: Double) = kg * KG_TO_LB
private fun lbToKg(lb: Double) = lb / KG_TO_LB

/* ───────────────────────────────── Main screen ─────────────────────────────────── */

@Composable
fun WeightTrackerScreen() {
    val context = LocalContext.current
    val db = remember { WeightDatabase.getInstance(context) }
    val dao = remember { db.weightDao() }
    val scope = rememberCoroutineScope()

    // Live list from Room; whenever DB changes, UI updates.
    val weights by dao.observeAll().collectAsState(initial = emptyList())

    // Simple unit toggle (we can persist later with DataStore if you want).
    var unit by rememberSaveable { mutableStateOf(UnitSystem.KG) }

    var showAddDialog by remember { mutableStateOf(false) }

    // ---------------- Stats for the overview card ----------------
    // Make small lists the helpers can use: just numbers, or (date, number) pairs.
    val weightsKg = remember(weights) { weights.map { it.weightKg } }
    val datedKg = remember(weights) { weights.map { it.date to it.weightKg } }

    // Crunch the numbers (avg/min/max), 7-day avg, and a tiny trend indicator.
    val allTime = remember(weightsKg) { computeStats(weightsKg) }
    val avg7kg = remember(datedKg) { rollingAverage(datedKg, 7) }
    val slope = remember(datedKg) { trendSlope(datedKg, window = 14) }
    val arrow = remember(slope) { trendArrow(slope) }

    // Format helper that respects the unit switch.
    fun fmt(valueKg: Double?): String =
        valueKg?.let { v ->
            if (unit == UnitSystem.KG)
                String.format(Locale.US, "%.1f kg", v)
            else
                String.format(Locale.US, "%.1f lb", kgToLb(v))
        } ?: "—"

    val latestStr = weights.firstOrNull()?.let {
        if (unit == UnitSystem.KG)
            String.format(Locale.US, "%.1f kg on %s", it.weightKg, it.date)
        else
            String.format(Locale.US, "%.1f lb on %s", kgToLb(it.weightKg), it.date)
    } ?: "—"

    val avgAllStr = fmt(allTime.avg)
    val avg7Str = fmt(avg7kg)
    val minMaxStr = if (allTime.min != null && allTime.max != null)
        "${fmt(allTime.min)} / ${fmt(allTime.max)}" else "—"
    // -------------------------------------------------------------

    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text("Weight Tracker") }) },
        bottomBar = {
            // Big “Add” button stuck to the bottom for easy access.
            Box(Modifier.fillMaxWidth().padding(16.dp)) {
                Button(
                    onClick = { showAddDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large
                ) { Text("Add Weight") }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            /* Unit switch — the DB stays in kg, we only change what we show */
            Text("Display units", style = MaterialTheme.typography.titleSmall)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val chip = Modifier.weight(1f).height(44.dp)
                Button(
                    onClick = { unit = UnitSystem.KG },
                    shape = MaterialTheme.shapes.large,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (unit == UnitSystem.KG)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = chip
                ) { Text("Kilograms") }

                Button(
                    onClick = { unit = UnitSystem.LB },
                    shape = MaterialTheme.shapes.large,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (unit == UnitSystem.LB)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = chip
                ) { Text("Pounds") }
            }

            /* Reminders: enable/disable daily ping to log weight */
            RemindersCard(
                onEnable = { scheduleDailyReminder(context, 20, 0) }, // 8:00 PM daily
                onDisable = { cancelDailyReminder(context) }
            )

            /* Overview: this is your algorithms/data-structures “show me the math” area */
            SummaryCard(
                latest = latestStr,
                avgAll = avgAllStr,
                avg7 = avg7Str,
                minMax = minMaxStr,
                trendArrow = arrow
            )

            /* History list (Room-backed) */
            Text("Your entries", style = MaterialTheme.typography.titleMedium)
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(weights, key = { it.id }) { entry ->
                    val prettyValue =
                        if (unit == UnitSystem.KG)
                            String.format(Locale.US, "%.1f kg", entry.weightKg)
                        else
                            String.format(Locale.US, "%.1f lb", kgToLb(entry.weightKg))

                    WeightRow(
                        date = entry.date,
                        valuePretty = prettyValue,
                        note = entry.note,
                        onDelete = { scope.launch { dao.deleteById(entry.id) } }
                    )
                    Divider()
                }
            }

            Spacer(Modifier.height(56.dp))
        }
    }

    // Add/Save dialog
    if (showAddDialog) {
        AddWeightDialog(
            unit = unit,
            onSave = { displayValue, note ->
                val valueKg = if (unit == UnitSystem.KG) displayValue else lbToKg(displayValue)
                val cleanNote = note.takeIf { it.isNotBlank() }
                val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                scope.launch {
                    dao.insert(WeightEntity(date = today, weightKg = valueKg, note = cleanNote))
                }
                showAddDialog = false
            },
            onCancel = { showAddDialog = false }
        )
    }
}

/* ───────────────────────────── Reusable UI bits ──────────────────────────────── */

@Composable
private fun RemindersCard(onEnable: () -> Unit, onDisable: () -> Unit) {
    ElevatedCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Reminders", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onEnable, shape = MaterialTheme.shapes.large) {
                    Text("Enable daily 8:00 PM")
                }
                OutlinedButton(onClick = onDisable, shape = MaterialTheme.shapes.large) {
                    Text("Disable")
                }
            }
            Text(
                "We’ll nudge you once a day to log your weight. Disable to stop reminders.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SummaryCard(
    latest: String,
    avgAll: String,
    avg7: String,
    minMax: String,
    trendArrow: String
) {
    ElevatedCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Overview", style = MaterialTheme.typography.titleMedium)
            Text("Latest: $latest")
            Text("Average (all time): $avgAll")
            Text("7-day average: $avg7")
            Text("Min / Max: $minMax")
            Text("Trend: $trendArrow")
        }
    }
}

@Composable
private fun WeightRow(
    date: String,
    valuePretty: String,
    note: String?,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(Modifier.weight(1f)) {
            Text(valuePretty, style = MaterialTheme.typography.bodyLarge)
            Text(date, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (!note.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(note, style = MaterialTheme.typography.bodySmall)
            }
        }
        OutlinedButton(onClick = onDelete, shape = MaterialTheme.shapes.large) { Text("Delete") }
    }
}

@Composable
private fun AddWeightDialog(
    unit: UnitSystem,
    onSave: (displayValue: Double, note: String) -> Unit,
    onCancel: () -> Unit
) {
    var valueText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    val parsed = valueText.toDoubleOrNull()

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Add weight") },
        text = {
            Column {
                OutlinedTextField(
                    value = valueText,
                    onValueChange = { valueText = it },
                    singleLine = true,
                    label = { Text("Weight (${if (unit == UnitSystem.KG) "kg" else "lb"})") }
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (optional)") }
                )
            }
        },
        confirmButton = {
            TextButton(enabled = parsed != null, onClick = { onSave(parsed!!, note) }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onCancel) { Text("Cancel") } }
    )
}

/* ────────────────────────────── Reminders helpers ─────────────────────────────── */

private fun scheduleDailyReminder(context: Context, hour: Int, minute: Int) {
    val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, ReminderReceiver::class.java)
    val pi = PendingIntent.getBroadcast(
        context, 1001, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
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

private fun cancelDailyReminder(context: Context) {
    val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, ReminderReceiver::class.java)
    val pi = PendingIntent.getBroadcast(
        context, 1001, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    am.cancel(pi)
}
