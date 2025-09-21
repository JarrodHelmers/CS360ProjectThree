package com.example.weight_trackerapp

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun SetPinScreen(onSet: (String) -> Unit) {
    var pin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    val ok = pin.length == 4 && pin == confirm

    Column(Modifier.padding(24.dp)) {
        Text("Create a 4-digit PIN", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = pin,
            onValueChange = { if (it.length <= 4) pin = it.filter(Char::isDigit) },
            label = { Text("PIN") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = confirm,
            onValueChange = { if (it.length <= 4) confirm = it.filter(Char::isDigit) },
            label = { Text("Confirm PIN") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = { onSet(pin) }, enabled = ok) { Text("Save PIN") }
    }
}

@Composable
fun EnterPinScreen(onEnter: (String) -> Unit) {
    var pin by remember { mutableStateOf("") }
    val ok = pin.length == 4

    Column(Modifier.padding(24.dp)) {
        Text("Enter your PIN", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = pin,
            onValueChange = { if (it.length <= 4) pin = it.filter(Char::isDigit) },
            label = { Text("PIN") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = { onEnter(pin) }, enabled = ok) { Text("Unlock") }
    }
}