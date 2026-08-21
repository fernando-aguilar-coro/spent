package com.app.spent.ui.settings.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.app.spent.R
import com.app.spent.util.ImageStorageHelper

@Composable
fun ImageStorageLocationCard(
    currentLocation: String,
    onSelectLocation: (location: String) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }

    val (currentLabel, currentDesc) = when (currentLocation) {
        ImageStorageHelper.DESTINATION_IN_APP -> Pair(
            stringResource(R.string.storage_option_in_app),
            stringResource(R.string.storage_option_in_app_desc)
        )
        ImageStorageHelper.DESTINATION_GOOGLE_DRIVE -> Pair(
            stringResource(R.string.storage_option_google_drive),
            stringResource(R.string.storage_option_google_drive_desc)
        )
        else -> Pair(
            stringResource(R.string.storage_option_device),
            stringResource(R.string.storage_option_device_desc)
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showDialog = true },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.AddPhotoAlternate,
                contentDescription = stringResource(R.string.image_storage_title),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.image_storage_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = currentLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.image_storage_title),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = stringResource(R.string.image_storage_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val options = listOf(
                        Triple(
                            ImageStorageHelper.DESTINATION_GOOGLE_DRIVE,
                            stringResource(R.string.storage_option_google_drive),
                            stringResource(R.string.storage_option_google_drive_desc)
                        ),
                        Triple(
                            ImageStorageHelper.DESTINATION_IN_APP,
                            stringResource(R.string.storage_option_in_app),
                            stringResource(R.string.storage_option_in_app_desc)
                        ),
                        Triple(
                            ImageStorageHelper.DESTINATION_DEVICE,
                            stringResource(R.string.storage_option_device),
                            stringResource(R.string.storage_option_device_desc)
                        )
                    )

                    options.forEach { (locValue, locTitle, locSubtitle) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelectLocation(locValue)
                                    showDialog = false
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = currentLocation == locValue,
                                onClick = {
                                    onSelectLocation(locValue)
                                    showDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = locTitle,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = locSubtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(stringResource(R.string.btn_close))
                }
            }
        )
    }
}
