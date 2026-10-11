package com.app.spent.ui.portfolio.components

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType
import com.app.spent.ui.theme.SageGreenPrimary

@Composable
fun EditQuantityDialog(
    symbol: String,
    currentQuantity: Double,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var text by remember { mutableStateOf(currentQuantity.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Update $symbol Holdings") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Units Owned") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    val newQty = text.toDoubleOrNull()
                    if (newQty != null && newQty >= 0) {
                        onConfirm(newQty)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = SageGreenPrimary)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
