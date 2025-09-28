package com.example.weight_trackerapp

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview

// ------------------------------
// Shared helpers
// ------------------------------

/** Very simple rule: PIN must be exactly 4 digits. */
private fun isValidPin(pin: String) = pin.length == 4 && pin.all { it.isDigit() }

/** Reusable card wrapper to center content in a soft, elevated container. */
@Composable
private fun CenterCard(content: @Composable ColumnScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                content = content
            )
        }
    }
}

/** A friendly masked PIN field with a show/hide toggle and inline error text. */
@Composable
private fun PinField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    errorText: String? = null
) {
    var show by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        onValueChange = { text ->
            // keep only digits, max 4 chars
            val cleaned = text.filter { it.isDigit() }.take(4)
            onValueChange(cleaned)
        },
        label = { Text(label) },
        singleLine = true,
        isError = errorText != null,
        supportingText = { if (errorText != null) Text(errorText) },
        visualTransformation = if (show) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            TextButton(
                onClick = { show = !show },
                contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp)
            ) { Text(if (show) "Hide" else "Show") }
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        modifier = Modifier.fillMaxWidth()
    )
}

/** Big, comfy primary button with rounded shape. */
@Composable
private fun PrimaryButton(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
    ) { Text(text) }
}

// ------------------------------
// Set PIN (first run)
// ------------------------------

/**
 * First-time setup: user creates a 4-digit PIN and confirms it.
 * Calls [onPinSet] with the new PIN when successful.
 */
@Composable
fun SetPinScreen(onPinSet: (String) -> Unit) {
    var pin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }

    val pinErr = when {
        pin.isEmpty() -> null
        !isValidPin(pin) -> "PIN must be 4 digits"
        else -> null
    }
    val confirmErr = when {
        confirm.isEmpty() -> null
        pin != confirm -> "PINs don’t match"
        else -> null
    }

    val canSave = isValidPin(pin) && pin == confirm

    CenterCard {
        Text("Set Your PIN", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        Text(
            "Create a 4-digit PIN to protect your entries. You’ll use this to unlock the app.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(4.dp))
        PinField(label = "Enter PIN", value = pin, onValueChange = { pin = it }, errorText = pinErr)
        PinField(label = "Confirm PIN", value = confirm, onValueChange = { confirm = it }, errorText = confirmErr)

        Spacer(Modifier.height(4.dp))
        PrimaryButton(text = "Save PIN", enabled = canSave) {
            onPinSet(pin)
        }
    }
}

// ------------------------------
// Enter PIN (unlock)
// ------------------------------

/**
 * Unlock screen: user enters the existing 4-digit PIN.
 * Calls [onPinEntered] with whatever they typed (caller verifies).
 */
@Composable
fun EnterPinScreen(onPinEntered: (String) -> Unit) {
    var pin by remember { mutableStateOf("") }
    var tried by remember { mutableStateOf(false) }

    val pinErr = when {
        !tried -> null
        !isValidPin(pin) -> "PIN must be 4 digits"
        else -> null
    }

    CenterCard {
        Text("Welcome Back", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        Text(
            "Enter your 4-digit PIN to unlock your weight tracker.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(4.dp))
        PinField(label = "PIN", value = pin, onValueChange = { pin = it }, errorText = pinErr)

        Spacer(Modifier.height(4.dp))
        PrimaryButton(text = "Unlock", enabled = pin.length == 4) {
            tried = true
            onPinEntered(pin)
        }
    }
}

// ------------------------------
// Previews
// ------------------------------
@Preview(showBackground = true)
@Composable
private fun PreviewSetPin() {
    MaterialTheme { SetPinScreen { } }
}

@Preview(showBackground = true)
@Composable
private fun PreviewEnterPin() {
    MaterialTheme { EnterPinScreen { } }
}