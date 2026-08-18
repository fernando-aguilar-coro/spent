package com.app.spent.ui.settings.components

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.app.spent.R
@Composable
fun GoogleDriveSyncCard(
isConnected: Boolean,
accountEmail: String?,
lastSyncTimestamp: Long,
isSyncing: Boolean,
onConnectClick: () -> Unit,
onDisconnectClick: () -> Unit,
onSyncNowClick: () -> Unit
) {
  val formattedSyncTime = remember(lastSyncTimestamp) {
    if (lastSyncTimestamp > 0) {
      SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(lastSyncTimestamp))
    } else null
  }

  Card(
  modifier = Modifier.fillMaxWidth(),
  shape = RoundedCornerShape(16.dp),
  colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      // Header
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

      Spacer(modifier = Modifier.height(14.dp))

      if (isSyncing) {
        Row(
        modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
        ) {
          CircularProgressIndicator(modifier = Modifier.size(22.dp))
          Spacer(modifier = Modifier.width(10.dp))
          Text(
          text = stringResource(R.string.drive_syncing),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.primary,
          fontWeight = FontWeight.Medium
          )
        }
      } else if (isConnected) {
        // Connected State Banner
        Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
        modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
              imageVector = Icons.Default.CheckCircle,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
              text = stringResource(R.string.drive_status_connected),
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onPrimaryContainer
              )
            }

            if (!accountEmail.isNullOrBlank()) {
              Spacer(modifier = Modifier.height(4.dp))
              Text(
              text = stringResource(R.string.drive_connected_account, accountEmail),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
              )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
              imageVector = Icons.Default.CloudDone,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(14.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
              text = if (formattedSyncTime != null)
              stringResource(R.string.drive_last_synced, formattedSyncTime)
              else
              stringResource(R.string.drive_not_synced_yet),
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Actions: Sync Now & Disconnect
        Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Button(
          onClick = onSyncNowClick,
          modifier = Modifier.weight(1f),
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
          ) {
            Icon(
            imageVector = Icons.Default.Sync,
            contentDescription = null,
            modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
            text = stringResource(R.string.drive_btn_sync_now),
            style = MaterialTheme.typography.labelLarge
            )
          }

          OutlinedButton(
          onClick = onDisconnectClick,
          modifier = Modifier.weight(1f),
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.outlinedButtonColors(
          contentColor = MaterialTheme.colorScheme.error
          )
          ) {
            Icon(
            imageVector = Icons.Default.LinkOff,
            contentDescription = null,
            modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
            text = stringResource(R.string.drive_btn_disconnect),
            style = MaterialTheme.typography.labelLarge
            )
          }
        }
      } else {
        // Disconnected State
        Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 4.dp)
        ) {
          Icon(
          imageVector = Icons.Default.CloudOff,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.outline,
          modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
          text = stringResource(R.string.drive_status_disconnected),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Single Connect Button
        Button(
        onClick = onConnectClick,
        modifier = Modifier
        .fillMaxWidth()
        .height(48.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
          Icon(
          imageVector = Icons.Default.CloudSync,
          contentDescription = null,
          modifier = Modifier.size(20.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
          text = stringResource(R.string.drive_btn_connect),
          fontWeight = FontWeight.Bold
          )
        }
      }
    }
  }
}
