package com.app.spent.ui.settings.components

import java.util.Currency
import java.util.Locale
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.spent.R
import com.app.spent.util.LocaleHelper

data class CurrencyOption(
  val code: String,
  val symbol: String,
  val name: String
)

@Composable
fun CurrencySelectionCard(
  currentCurrencySymbol: String,
  onSelectCurrencySymbol: (symbol: String) -> Unit
) {
  var showDialog by remember { mutableStateOf(false) }
  var searchQuery by remember { mutableStateOf("") }
  val context = LocalContext.current

  val currencyList = remember {
    try {
      val sysLocale = LocaleHelper.getSystemPreferredLocale(context)
      val popularCodes = listOf(
        "USD", "EUR", "INR", "JPY", "GBP", "BRL", "CAD", "CHF", "AUD",
        "MXN", "COP", "PEN", "CLP", "ARS", "BOB", "VES", "PYG", "CNY",
        "KRW", "RUB", "TRY", "SAR", "AED", "ZAR", "SGD", "NZD", "HKD",
        "SEK", "NOK", "DKK", "PLN", "THB", "IDR", "MYR", "PHP", "VND"
      )

      val popularSet = popularCodes.toSet()
      val available = Currency.getAvailableCurrencies()
        .filter { it.currencyCode != null && it.currencyCode.length == 3 }
        .map { cur ->
          val code = cur.currencyCode
          val symbol = try {
            val s = cur.getSymbol(sysLocale)
            if (s.isNotBlank()) s else cur.symbol
          } catch (e: Exception) {
            cur.symbol ?: code
          }
          val displayName = try {
            cur.getDisplayName(sysLocale)
          } catch (e: Exception) {
            code
          }
          CurrencyOption(
            code = code,
            symbol = symbol,
            name = "$displayName ($symbol • $code)"
          )
        }
        .distinctBy { it.code }
        .sortedWith(
          compareBy<CurrencyOption> { opt ->
            val idx = popularCodes.indexOf(opt.code)
            if (idx >= 0) idx else 1000
          }.thenBy { it.name }
        )

      if (available.isNotEmpty()) available else getFallbackCurrencies()
    } catch (e: Exception) {
      getFallbackCurrencies()
    }
  }

  val selectedLabel = remember(currentCurrencySymbol, currencyList) {
    currencyList.find { it.symbol == currentCurrencySymbol }?.name ?: currentCurrencySymbol
  }

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .clickable {
        searchQuery = ""
        showDialog = true
      },
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
    val filteredList = remember(searchQuery, currencyList) {
      if (searchQuery.isBlank()) {
        currencyList
      } else {
        val q = searchQuery.trim().lowercase()
        currencyList.filter {
          it.code.lowercase().contains(q) ||
            it.name.lowercase().contains(q) ||
            it.symbol.lowercase().contains(q)
        }
      }
    }

    AlertDialog(
      onDismissRequest = { showDialog = false },
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
            placeholder = { Text("Search currency, code or symbol…", fontSize = 13.sp) },
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
            if (filteredList.isEmpty()) {
              item {
                Text(
                  text = "No currencies match your search.",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.padding(16.dp)
                )
              }
            } else {
              items(filteredList, key = { it.code }) { item ->
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                      onSelectCurrencySymbol(item.symbol)
                      showDialog = false
                    }
                    .padding(vertical = 8.dp, horizontal = 4.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Box(
                    modifier = Modifier
                      .size(32.dp)
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
                  Spacer(modifier = Modifier.width(10.dp))
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = item.name,
                      style = MaterialTheme.typography.bodyMedium,
                      fontWeight = FontWeight.Medium
                    )
                  }
                  RadioButton(
                    selected = currentCurrencySymbol == item.symbol,
                    onClick = {
                      onSelectCurrencySymbol(item.symbol)
                      showDialog = false
                    }
                  )
                }
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

private fun getFallbackCurrencies(): List<CurrencyOption> = listOf(
  CurrencyOption("USD", "$", "US Dollar ($ • USD)"),
  CurrencyOption("EUR", "€", "Euro (€ • EUR)"),
  CurrencyOption("INR", "₹", "Indian Rupee (₹ • INR)"),
  CurrencyOption("JPY", "¥", "Japanese Yen (¥ • JPY)"),
  CurrencyOption("GBP", "£", "British Pound (£ • GBP)"),
  CurrencyOption("BRL", "R$", "Brazilian Real (R$ • BRL)"),
  CurrencyOption("CAD", "CA$", "Canadian Dollar (CA$ • CAD)"),
  CurrencyOption("CHF", "CHF", "Swiss Franc (CHF • CHF)"),
  CurrencyOption("AUD", "A$", "Australian Dollar (A$ • AUD)"),
  CurrencyOption("MXN", "MXN $", "Mexican Peso (MXN $ • MXN)"),
  CurrencyOption("COP", "COP $", "Colombian Peso (COP $ • COP)"),
  CurrencyOption("PEN", "S/", "Peruvian Sol (S/ • PEN)"),
  CurrencyOption("CLP", "CLP $", "Chilean Peso (CLP $ • CLP)"),
  CurrencyOption("ARS", "ARS $", "Argentine Peso (ARS $ • ARS)"),
  CurrencyOption("BOB", "Bs.", "Bolivian Boliviano (Bs. • BOB)"),
  CurrencyOption("VES", "Bs.", "Venezuelan Bolívar (Bs. • VES)"),
  CurrencyOption("PYG", "₲", "Paraguayan Guaraní (₲ • PYG)")
)
