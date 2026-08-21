package com.app.spent.ui.onboarding.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.spent.R
import com.app.spent.ui.settings.components.ImageStorageLocationCard

@Composable
fun WelcomeStep(
isRestoring: Boolean,
isConnected: Boolean,
accountEmail: String?,
imageStorageLocation: String,
onSelectImageStorageLocation: (String) -> Unit,
onConnectDrive: () -> Unit,
onContinue: () -> Unit
) {
  Column(
  modifier = Modifier
  .fillMaxSize()
  .padding(horizontal = 24.dp, vertical = 12.dp)
  .verticalScroll(rememberScrollState()),
  horizontalAlignment = Alignment.CenterHorizontally,
  verticalArrangement = Arrangement.SpaceBetween
  ) {
    Spacer(modifier = Modifier.height(8.dp))

    // Hero Icon & Illustration
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Box(
      modifier = Modifier
      .size(96.dp)
      .clip(CircleShape)
      .background(
      Brush.linearGradient(
      listOf(
      MaterialTheme.colorScheme.primary,
      MaterialTheme.colorScheme.tertiary
      )
      )
      ),
      contentAlignment = Alignment.Center
      ) {
        Icon(
        imageVector = Icons.Default.AccountBalance,
        contentDescription = null,
        tint = Color.White,
        modifier = Modifier.size(52.dp)
        )
      }

      Spacer(modifier = Modifier.height(24.dp))

      Text(
      text = stringResource(R.string.onboarding_welcome_title),
      style = MaterialTheme.typography.headlineLarge,
      fontWeight = FontWeight.ExtraBold,
      color = MaterialTheme.colorScheme.onBackground,
      textAlign = TextAlign.Center
      )

      Spacer(modifier = Modifier.height(12.dp))

      Text(
      text = stringResource(R.string.onboarding_welcome_desc),
      style = MaterialTheme.typography.bodyLarge,
      textAlign = TextAlign.Center,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      lineHeight = 24.sp
      )

      Spacer(modifier = Modifier.height(24.dp))

      // Value Props Badges
      Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        FeatureBadge(
        icon = Icons.Default.Security,
        label = "100% Offline",
        modifier = Modifier.weight(1f)
        )
        FeatureBadge(
        icon = Icons.Default.Speed,
        label = "Quick Entry",
        modifier = Modifier.weight(1f)
        )
        FeatureBadge(
        icon = Icons.Default.CloudSync,
        label = "Drive Sync",
        modifier = Modifier.weight(1f)
        )
      }

      Spacer(modifier = Modifier.height(20.dp))

      // Image & Receipt Storage Destination Selector
      ImageStorageLocationCard(
          currentLocation = imageStorageLocation,
          onSelectLocation = onSelectImageStorageLocation
      )
    }

    Spacer(modifier = Modifier.height(40.dp))

    // Actions
    Column(
    modifier = Modifier.fillMaxWidth(),
    horizontalAlignment = Alignment.CenterHorizontally
    ) {
      if (isRestoring) {
        CircularProgressIndicator(modifier = Modifier.size(32.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text(
        text = stringResource(R.string.onboarding_restoring_drive),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.primary,
        textAlign = TextAlign.Center
        )
      } else if (isConnected) {
        // Connected status card
        Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
        modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 16.dp)
        ) {
          Row(
          modifier = Modifier.padding(16.dp),
          verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text(
              text = stringResource(R.string.drive_status_connected),
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onPrimaryContainer
              )
              if (!accountEmail.isNullOrBlank()) {
                Text(
                text = accountEmail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
              }
            }
          }
        }

        Button(
        onClick = onContinue,
        modifier = Modifier
        .fillMaxWidth()
        .height(54.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.primary
        )
        ) {
          Text(
          text = stringResource(R.string.onboarding_btn_continue),
          fontWeight = FontWeight.ExtraBold,
          textAlign = TextAlign.Center
          )
        }
      } else {
        // Single button to connect with Google Drive
        Button(
        onClick = onConnectDrive,
        modifier = Modifier
        .fillMaxWidth()
        .height(54.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.primary
        )
        ) {
          Icon(
          imageVector = Icons.Default.CloudSync,
          contentDescription = null,
          modifier = Modifier.size(22.dp)
          )
          Spacer(modifier = Modifier.width(10.dp))
          Text(
          text = stringResource(R.string.onboarding_btn_connect_drive),
          fontWeight = FontWeight.Bold,
          textAlign = TextAlign.Center
          )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Continue without connecting option
        OutlinedButton(
        onClick = onContinue,
        modifier = Modifier
        .fillMaxWidth()
        .height(54.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.outlinedButtonColors(
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
        ) {
          Text(
          text = stringResource(R.string.onboarding_btn_continue_no_account),
          fontWeight = FontWeight.SemiBold,
          textAlign = TextAlign.Center
          )
        }
      }

      Spacer(modifier = Modifier.height(16.dp))
    }
  }
}

@Composable
private fun FeatureBadge(
icon: ImageVector,
label: String,
modifier: Modifier = Modifier
) {
  Surface(
  shape = RoundedCornerShape(14.dp),
  color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
  modifier = modifier
  ) {
    Column(
    modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
    horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Icon(
      imageVector = icon,
      contentDescription = null,
      tint = MaterialTheme.colorScheme.primary,
      modifier = Modifier.size(22.dp)
      )
      Spacer(modifier = Modifier.height(6.dp))
      Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      fontWeight = FontWeight.Medium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = TextAlign.Center
      )
    }
  }
}
