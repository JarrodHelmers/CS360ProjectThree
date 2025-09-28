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
import androidx.core.app.NotificationManagerCompat
import com.example.weight_trackerapp.data.PinStore
import com.example.weight_trackerapp.data.UnitStore
import com.example.weight_trackerapp.data.WeightStore
import com.example.weight_trackerapp.util.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// -----------------------------
// Data model for a weight entry
// -----------------------------
data class WeightEntry(
    val id: Long = System.currentTimeMillis(),
    val date: String,
    val weightKg: Double,        // NOTE: always stored in KG internally
    val note: String? = null
)

class MainActivity : ComponentActivity() {

    private val requestNotifPerm = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* no-op */ }

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

// ---------------------------------------------------
// Reminder helpers (schedule / cancel)
// ---------------------------------------------------
fun scheduleDailyReminder(context: Context, hour: Int = 20, minute: Int = 0) {
    val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, ReminderReceiver::class.java)
    val pi = PendingIntent.getBroadcast(
        context, 1234, intent,
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
        context, 1234, intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    am.cancel(pi)
}

// -----------------------------
// App Root: PIN vs Main app
// -----------------------------
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

// -----------------------------
// Main Screen (polish + stats + unit switch)
// -----------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeightTrackerScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val unitStore = remember(context) { UnitStore(context) }
    val store = remember(context) { WeightStore(context) }
    val scope = rememberCoroutineScope()

    // Persisted unit choice (KG default)
    val unit by unitStore.unitFlow.collectAsState(initial = UnitSystem.KG)

    // All entries from persistent storage (stored in KG)
    val items by store.weights.collectAsState(initial = emptyList())

    // Dialog state
    var showAdd by remember { mutableStateOf(false) }

    // Derived stats (computed in KG, converted for display if needed)
    val weightsKg = remember(items) { items.map { it.weightKg } }
    val datedKg = remember(items) { items.map { it.date to it.weightKg } }

    val allTime = remember(weightsKg) { computeStats(weightsKg) }
    val avg7kg = remember(datedKg) { rollingAverage(datedKg, days = 7) }

    // Optional trend uses kg internally (unitless slope per day)
    val slope = remember(datedKg) { trendSlope(datedKg, window = 14) }
    val arrow = remember(slope) { trendArrow(slope) }

    // Convert numbers for display depending on unit
    val displayLatest = items.firstOrNull()?.let { entry ->
        val value = if (unit == UnitSystem.KG) entry.weightKg else kgToLb(entry.weightKg)
        String.format(Locale.US, "%.1f %s on %s", value, if (unit == UnitSystem.KG) "kg" else "lb", entry.date)
    }

    val displayAvgAll = allTime.avg?.let {
        val v = if (unit == UnitSystem.KG) it else kgToLb(it)
        String.format(Locale.US, "%.1f %s", v, if (unit == UnitSystem.KG) "kg" else "lb")
    }

    val displayAvg7 = avg7kg?.let {
        val v = if (unit == UnitSystem.KG) it else kgToLb(it)
        String.format(Locale.US, "%.1f %s", v, if (unit == UnitSystem.KG) "kg" else "lb")
    }

    val displayMinMax = if (allTime.min != null && allTime.max != null) {
        val min = if (unit == UnitSystem.KG) allTime.min!! else kgToLb(allTime.min!!)
        val max = if (unit == UnitSystem.KG) allTime.max!! else kgToLb(allTime.max!!)
        String.format(
            Locale.US,
            "%.1f %s / %.1f %s",
            min, if (unit == UnitSystem.KG) "kg" else "lb",
            max, if (unit == UnitSystem.KG) "kg" else "lb"
        )
    } else null

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Weight Tracker", style = MaterialTheme.typography.titleLarge) }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = { showAdd = true }) { Text("＋ Add") }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // New: Settings section with unit switch (KG / LB)
            SettingsCard(
                current = unit,
                onSelect = { chosen -> scope.launch { unitStore.setUnit(chosen) } }
            )

            RemindersCard(
                onEnable = { scheduleDailyReminder(context, 20, 0) },
                onDisable = { cancelDailyReminder(context) }
            )

            SummaryCard(
                latest = displayLatest ?: "—",
                avgAll = displayAvgAll ?: "—",
                avg7 = displayAvg7 ?: "—",
                minMax = displayMinMax ?: "—",
                trendArrow = arrow
            )

            SectionCard {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(items, key = { it.id }) { entry ->
                        val shown = if (unit == UnitSystem.KG) entry.weightKg else kgToLb(entry.weightKg)
                        WeightRow(
                            entryDate = entry.date,
                            shownValue = String.format(Locale.US, "%.1f %s", shown, if (unit == UnitSystem.KG) "kg" else "lb"),
                            note = entry.note,
                            onDelete = { scope.launch { store.delete(entry.id) } }
                        )
                        Divider()
                    }
                }
            }
            Spacer(Modifier.height(72.dp))
        }
    }

    if (showAdd) {
        AddWeightDialog(
            unit = unit,
            onDismiss = { showAdd = false },
            onSave = { displayValue, note ->
                // Convert to KG for storage if user entered LB
                val valueKg = if (unit == UnitSystem.KG) displayValue else lbToKg(displayValue)
                val noteOrNull = note.takeIf { it.isNotBlank() }
                val item = WeightEntry(date = today(), weightKg = valueKg, note = noteOrNull)
                scope.launch { store.add(item) }
                showAdd = false
            }
        )
    }
}

// -----------------------------
// Settings: Unit switch (KG / LB)
// -----------------------------
@Composable
private fun SettingsCard(
    current: UnitSystem,
    onSelect: (UnitSystem) -> Unit
) {
    SectionCard {
        Text("Settings", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))

        // Simple segmented control look using 2 buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val selColor = MaterialTheme.colorScheme.primary
            val unSel = MaterialTheme.colorScheme.surfaceVariant

            val common = Modifier
                .weight(1f)
                .height(44.dp)

            Button(
                onClick = { onSelect(UnitSystem.KG) },
                shape = MaterialTheme.shapes.large,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (current == UnitSystem.KG) selColor else unSel
                ),
                modifier = common
            ) { Text("Kilograms") }

            Button(
                onClick = { onSelect(UnitSystem.LB) },
                shape = MaterialTheme.shapes.large,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (current == UnitSystem.LB) selColor else unSel
                ),
                modifier = common
            ) { Text("Pounds") }
        }

        Spacer(Modifier.height(6.dp))
        Text(
            "Choose how weights are displayed. Your data is stored internally in kilograms.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// -----------------------------
// Section container with a soft look
// -----------------------------
@Composable
private fun SectionCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) { Column(Modifier.padding(16.dp), content = content) }
}

// -----------------------------
// Label/value row for Overview stats
// -----------------------------
@Composable
private fun StatRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

// -----------------------------
// Reminders (Enable / Disable only)
// -----------------------------
@Composable
private fun RemindersCard(
    onEnable: () -> Unit,
    onDisable: () -> Unit
) {
    SectionCard {
        Text("Reminders", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = onEnable,
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.shapes.large
            ) { Text("Enable daily 8:00 PM") }

            OutlinedButton(
                onClick = onDisable,
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.shapes.large
            ) { Text("Disable") }
        }

        Spacer(Modifier.height(10.dp))
        Text(
            "Enable schedules a daily reminder. Disable cancels it.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// -----------------------------
// Overview card (latest, averages, trend)
// -----------------------------
@Composable
private fun SummaryCard(
    latest: String,
    avgAll: String,
    avg7: String,
    minMax: String,
    trendArrow: String
) {
    SectionCard {
        Text("Overview", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))

        StatRow("Latest", latest)
        Spacer(Modifier.height(6.dp))
        StatRow("Average (all time)", avgAll)
        Spacer(Modifier.height(6.dp))
        StatRow("7-day average", avg7)
        Spacer(Modifier.height(6.dp))
        StatRow("Min / Max", minMax)
        Spacer(Modifier.height(6.dp))
        StatRow("Trend", trendArrow)
    }
}

// -----------------------------
// One row in the history list (unit-aware)
// -----------------------------
@Composable
private fun WeightRow(
    entryDate: String,
    shownValue: String,
    note: String?,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp, horizontal = 4.dp)
            .clickable { /* could expand to edit later */ },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(shownValue, fontWeight = FontWeight.SemiBold)
            Text(
                entryDate,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            note?.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.height(2.dp))
                Text(it, style = MaterialTheme.typography.bodySmall)
            }
        }
        TextButton(onClick = onDelete) { Text("Delete") }
    }
}

// ---------------------------------------------------
// “Add weight” dialog (unit-aware) with validation
// ---------------------------------------------------
@Composable
private fun AddWeightDialog(
    unit: UnitSystem,
    onDismiss: () -> Unit,
    onSave: (displayValue: Double, note: String) -> Unit
) {
    var valueText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    val parsed = valueText.toDoubleOrNull()
    val error = validateWeight(parsed, unit) // unit-aware validation
    val canSave = error == null

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(enabled = canSave, onClick = { onSave(parsed!!, note) }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text("Add weight") },
        text = {
            Column {
                OutlinedTextField(
                    value = valueText,
                    onValueChange = { valueText = it },
                    label = { Text("Weight (${if (unit == UnitSystem.KG) "kg" else "lb"})") },
                    isError = error != null,
                    supportingText = { if (error != null) Text(error) },
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

// -----------------------------
// Utility: today’s date as yyyy-MM-dd
// -----------------------------
private fun today(): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())