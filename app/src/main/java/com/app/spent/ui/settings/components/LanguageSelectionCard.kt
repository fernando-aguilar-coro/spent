package com.app.spent.ui.settings.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
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
@Composable
fun LanguageSelectionCard(
currentLanguageCode: String?,
onSelectLanguage: (String?) -> Unit
) {
  var showDialog by remember { mutableStateOf(false) }

  val currentLabel = when (currentLanguageCode) {
    "en" -> stringResource(R.string.language_en)
    "es" -> stringResource(R.string.language_es)
    "pt" -> stringResource(R.string.language_pt)
    else -> stringResource(R.string.language_system)
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
      imageVector = Icons.Default.Language,
      contentDescription = "Language",
      tint = MaterialTheme.colorScheme.primary
      )
      Spacer(modifier = Modifier.width(12.dp))
      Column {
        Text(
        text = stringResource(R.string.language_title),
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
    title = { Text(stringResource(R.string.language_title), fontWeight = FontWeight.Bold) },
    text = {
      Column {
        val options = listOf(
        null to stringResource(R.string.language_system),
        "en" to stringResource(R.string.language_en),
        "es" to stringResource(R.string.language_es),
        "pt" to stringResource(R.string.language_pt)
        )

        options.forEach { (codeValue, label) ->
          Row(
          modifier = Modifier
          .fillMaxWidth()
          .clickable {
            onSelectLanguage(codeValue)
            showDialog = false
          }
          .padding(vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically
          ) {
            RadioButton(
            selected = currentLanguageCode == codeValue,
            onClick = {
              onSelectLanguage(codeValue)
              showDialog = false
            }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(label, style = MaterialTheme.typography.bodyMedium)
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
