package com.example.spent.ui.transaction.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.spent.R

@Composable
fun AddCategoryDialog(
    onDismiss: () -> Unit,
    onSaveCategory: (name: String, colorHex: String) -> Unit
) {
    var newCategoryName by remember { mutableStateOf("") }
    var newCategoryColorHex by remember { mutableStateOf("#3B82F6") }

    // 14 rich, modern curated color choices (doubled from original 6)
    val colorPaletteRow1 = listOf(
        "#3B82F6", // Blue
        "#10B981", // Emerald
        "#EF4444", // Red
        "#F59E0B", // Amber
        "#8B5CF6", // Purple
        "#EC4899", // Pink
        "#06B6D4"  // Cyan
    )

    val colorPaletteRow2 = listOf(
        "#14B8A6", // Teal
        "#84CC16", // Lime
        "#F97316", // Orange
        "#6366F1", // Indigo
        "#D946EF", // Fuchsia
        "#0EA5E9", // Sky Blue
        "#64748B"  // Slate Gray
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.create_category_title), fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = newCategoryName,
                    onValueChange = { newCategoryName = it },
                    label = { Text(stringResource(R.string.category_name_label)) },
                    placeholder = { Text(stringResource(R.string.category_name_placeholder)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.select_color),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )

                    // Visual badge indicating currently selected color
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(Color(android.graphics.Color.parseColor(newCategoryColorHex)))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = newCategoryColorHex.uppercase(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Color Palette Grid (Row 1)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    colorPaletteRow1.forEach { colorHex ->
                        val isSelected = newCategoryColorHex.equals(colorHex, ignoreCase = true)
                        val color = Color(android.graphics.Color.parseColor(colorHex))

                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(color)
                                .then(
                                    if (isSelected) Modifier.border(BorderStroke(2.5.dp, MaterialTheme.colorScheme.onSurface), CircleShape)
                                    else Modifier
                                )
                                .clickable { newCategoryColorHex = colorHex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Color Palette Grid (Row 2)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    colorPaletteRow2.forEach { colorHex ->
                        val isSelected = newCategoryColorHex.equals(colorHex, ignoreCase = true)
                        val color = Color(android.graphics.Color.parseColor(colorHex))

                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(color)
                                .then(
                                    if (isSelected) Modifier.border(BorderStroke(2.5.dp, MaterialTheme.colorScheme.onSurface), CircleShape)
                                    else Modifier
                                )
                                .clickable { newCategoryColorHex = colorHex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (newCategoryName.isNotBlank()) {
                        onSaveCategory(newCategoryName.trim(), newCategoryColorHex)
                        onDismiss()
                    }
                },
                enabled = newCategoryName.isNotBlank()
            ) {
                Text(stringResource(R.string.save_category))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_cancel))
            }
        }
    )
}
