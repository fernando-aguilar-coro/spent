package com.example.spent.ui.settings.components

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.spent.R
import com.example.spent.data.repository.SpentRepository
import com.example.spent.data.sync.DriveBackupManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import com.example.spent.data.sync.GoogleDriveRestService
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.common.api.ApiException

@Composable
fun GoogleDriveSyncCard(
    repository: SpentRepository,
    lastSyncTimestamp: Long,
    onSyncComplete: (String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isProcessing by remember { mutableStateOf(false) }

    var signedInAccount by remember { mutableStateOf(GoogleDriveRestService.getSignedInAccount(context)) }

    val formattedSyncTime = remember(lastSyncTimestamp) {
        if (lastSyncTimestamp > 0) {
            SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(lastSyncTimestamp))
        } else null
    }

    // Google Sign In Launcher
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            signedInAccount = account
            onSyncComplete("Google Account connected: ${account?.email}")
        } catch (e: Exception) {
            e.printStackTrace()
            onSyncComplete("Sign in failed: ${e.localizedMessage}")
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CloudSync,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.drive_sync_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.drive_sync_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Last Synced Status Banner
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CloudDone,
                    contentDescription = null,
                    tint = if (lastSyncTimestamp > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (formattedSyncTime != null)
                        stringResource(R.string.drive_last_synced, formattedSyncTime)
                    else
                        stringResource(R.string.drive_not_synced_yet),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isProcessing) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Synchronizing with Google Drive...",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Backup / Sync Button
                    Button(
                        onClick = {
                            val account = signedInAccount
                            if (account == null) {
                                val signInClient = GoogleDriveRestService.getGoogleSignInClient(context)
                                googleSignInLauncher.launch(signInClient.signInIntent)
                            } else {
                                coroutineScope.launch {
                                    isProcessing = true
                                    val json = DriveBackupManager.generateBackupJson(repository)
                                    val result = GoogleDriveRestService.uploadBackupToDrive(context, account, json)
                                    isProcessing = false
                                    if (result.isSuccess) {
                                        repository.setLastDriveSyncTimestamp(System.currentTimeMillis())
                                        onSyncComplete(context.getString(R.string.drive_backup_success))
                                    } else {
                                        val err = result.exceptionOrNull()?.localizedMessage ?: "Unknown error"
                                        onSyncComplete(context.getString(R.string.drive_sync_error, err))
                                    }
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (signedInAccount != null) stringResource(R.string.drive_backup_now) else "Connect Drive")
                    }

                    // Restore Button
                    OutlinedButton(
                        onClick = {
                            val account = signedInAccount
                            if (account == null) {
                                val signInClient = GoogleDriveRestService.getGoogleSignInClient(context)
                                googleSignInLauncher.launch(signInClient.signInIntent)
                            } else {
                                coroutineScope.launch {
                                    isProcessing = true
                                    val result = GoogleDriveRestService.downloadBackupFromDrive(context, account)
                                    if (result.isSuccess) {
                                        val json = result.getOrNull().orEmpty()
                                        val restoreRes = DriveBackupManager.restoreFromJson(json, repository)
                                        isProcessing = false
                                        if (restoreRes.isSuccess) {
                                            onSyncComplete(context.getString(R.string.drive_restore_success))
                                        } else {
                                            val err = restoreRes.exceptionOrNull()?.localizedMessage ?: "Invalid JSON"
                                            onSyncComplete(context.getString(R.string.drive_sync_error, err))
                                        }
                                    } else {
                                        isProcessing = false
                                        val err = result.exceptionOrNull()?.localizedMessage ?: "Backup not found"
                                        onSyncComplete(context.getString(R.string.drive_sync_error, err))
                                    }
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.drive_restore_now))
                    }
                }
            }
        }
    }
}
