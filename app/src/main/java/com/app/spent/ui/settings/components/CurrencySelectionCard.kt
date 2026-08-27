package com.app.spent.ui.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.unit.sp
import com.app.spent.R
import com.app.spent.util.LocaleHelper

@Composable
fun CurrencySelectionCard(
  currentCurrencySymbol: String,
  onSelectCurrencySymbol: (symbol: String) -> Unit,
  modifier: Modifier = Modifier
) {
  var showDialog by remember { mutableStateOf(false) }
  val selectedLabel = remember(currentCurrencySymbol) {
    LocaleHelper.getCurrencyItemForSymbol(currentCurrencySymbol)?.name ?: currentCurrencySymbol
  }

  Card(
    modifier = modifier
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
        imageVector = Icons.Default.AttachMoney,
        contentDescription = "Currency",
        tint = MaterialTheme.colorScheme.primary
      )
      Spacer(modifier = Modifier.width(12.dp))
      Column {
        Text(
          text = stringResource(R.string.currency_title),
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold
        )
        Text(
          text = selectedLabel,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }
  }

  if (showDialog) {
    CurrencyPickerDialog(
      currentCurrencySymbol = currentCurrencySymbol,
      onSelectCurrencySymbol = {
        onSelectCurrencySymbol(it)
        showDialog = false
      },
      onDismiss = { showDialog = false }
    )
  }
}

@Composable
fun CurrencyPickerDialog(
  currentCurrencySymbol: String,
  onSelectCurrencySymbol: (symbol: String) -> Unit,
  onDismiss: () -> Unit
) {
  var searchQuery by remember { mutableStateOf("") }
  val filteredList = remember(searchQuery) {
    if (searchQuery.isBlank()) {
      LocaleHelper.SUPPORTED_CURRENCIES
    } else {
      val q = searchQuery.trim().lowercase()
      LocaleHelper.SUPPORTED_CURRENCIES.filter {
        it.code.lowercase().contains(q) ||
          it.name.lowercase().contains(q) ||
          it.symbol.lowercase().contains(q)
      }
    }
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = stringResource(R.string.currency_title),
        fontWeight = FontWeight.Bold
      )
    },
    text = {
      Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
          placeholder = { Text("Search currency…", fontSize = 13.sp) },
          leadingIcon = {
            Icon(
              imageVector = Icons.Default.Search,
              contentDescription = "Search",
              modifier = Modifier.size(18.dp)
            )
          },
          trailingIcon = {
            if (searchQuery.isNotEmpty()) {
              IconButton(onClick = { searchQuery = "" }) {
                Icon(
                  imageVector = Icons.Default.Clear,
                  contentDescription = "Clear",
                  modifier = Modifier.size(18.dp)
                )
              }
            }
          },
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
          )
        )

        LazyColumn(
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 380.dp)
        ) {
          items(filteredList, key = { it.code }) { item ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clickable { onSelectCurrencySymbol(item.symbol) }
                .padding(vertical = 8.dp, horizontal = 4.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Box(
                modifier = Modifier
                  .size(36.dp)
                  .background(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    shape = CircleShape
                  ),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = item.symbol,
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.primary
                )
              }
              Spacer(modifier = Modifier.width(12.dp))
              Text(
                text = item.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
              )
              RadioButton(
                selected = currentCurrencySymbol == item.symbol,
                onClick = { onSelectCurrencySymbol(item.symbol) }
              )
            }
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) {
        Text(stringResource(R.string.btn_close))
      }
    }
  )
}
