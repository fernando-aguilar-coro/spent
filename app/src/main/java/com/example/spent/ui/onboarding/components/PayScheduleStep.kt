package com.app.spent.ui.onboarding.components

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.spent.R
import com.app.spent.ui.onboarding.OnboardingUiState
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PayScheduleStep(
state: OnboardingUiState,
onSelectFrequency: (String) -> Unit,
onSelectStartDate: (Long) -> Unit,
onUpdateSalary: (String) -> Unit,
onFinish: () -> Unit
) {
  var showDatePicker by remember { mutableStateOf(false) }

  val formattedAnchorDate = remember(state.selectedStartDate) {
    SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(state.selectedStartDate))
  }

  val frequencies = listOf(
  "WEEKLY" to stringResource(R.string.frequency_weekly),
  "BIWEEKLY" to stringResource(R.string.frequency_biweekly),
  "SEMIMONTHLY" to stringResource(R.string.frequency_semimonthly),
  "MONTHLY" to stringResource(R.string.frequency_monthly)
  )

  Column(
  modifier = Modifier
  .fillMaxSize()
  .padding(horizontal = 24.dp, vertical = 12.dp)
  .verticalScroll(rememberScrollState()),
  horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Text(
    text = stringResource(R.string.onboarding_schedule_title),
    style = MaterialTheme.typography.headlineMedium,
    fontWeight = FontWeight.Bold,
    textAlign = TextAlign.Center,
    color = MaterialTheme.colorScheme.onBackground
    )

    Spacer(modifier = Modifier.height(6.dp))

    Text(
    text = stringResource(R.string.onboarding_schedule_subtitle),
    style = MaterialTheme.typography.bodyMedium,
    textAlign = TextAlign.Center,
    color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Spacer(modifier = Modifier.height(20.dp))

    // Frequency Selection Card
    Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Text(
        text = stringResource(R.string.onboarding_schedule_frequency),
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        frequencies.forEach { (key, label) ->
          Row(
          modifier = Modifier
          .fillMaxWidth()
          .clickable { onSelectFrequency(key) }
          .padding(vertical = 4.dp),
          verticalAlignment = Alignment.CenterVertically
          ) {
            RadioButton(
            selected = state.selectedFrequency == key,
            onClick = { onSelectFrequency(key) }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (state.selectedFrequency == key) FontWeight.Bold else FontWeight.Normal
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // Calendar Anchor Date Card (Default Today)
    Card(
    modifier = Modifier
    .fillMaxWidth()
    .clickable { showDatePicker = true },
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
      Row(
      modifier = Modifier.padding(18.dp),
      verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(
        imageVector = Icons.Default.CalendarToday,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
          text = stringResource(R.string.onboarding_schedule_payday),
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
          text = formattedAnchorDate,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.primary,
          fontWeight = FontWeight.SemiBold
          )
        }
        TextButton(onClick = { showDatePicker = true }) {
          Text(stringResource(R.string.btn_change))
        }
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // Optional Salary TextField
    Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Text(
        text = stringResource(R.string.onboarding_schedule_salary_optional, state.currencySymbol),
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
        value = state.salaryText,
        onValueChange = onUpdateSalary,
        placeholder = { Text("0.00") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth()
        )
      }
    }

    Spacer(modifier = Modifier.height(24.dp))

    // Finish Button
    Button(
    onClick = onFinish,
    modifier = Modifier
    .fillMaxWidth()
    .height(54.dp),
    shape = RoundedCornerShape(16.dp),
    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
      Text(
      text = stringResource(R.string.onboarding_btn_finish_setup),
      fontWeight = FontWeight.Bold,
      fontSize = 16.sp
      )
    }

    Spacer(modifier = Modifier.height(16.dp))
  }

  if (showDatePicker) {
    val datePickerState = rememberDatePickerState(
    initialSelectedDateMillis = state.selectedStartDate
    )

    DatePickerDialog(
    onDismissRequest = { showDatePicker = false },
    confirmButton = {
      TextButton(
      onClick = {
        datePickerState.selectedDateMillis?.let { utcMillis ->
          val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = utcMillis
          }
          val cal = Calendar.getInstance().apply {
            timeInMillis = state.selectedStartDate
            set(Calendar.YEAR, utcCal.get(Calendar.YEAR))
            set(Calendar.MONTH, utcCal.get(Calendar.MONTH))
            set(Calendar.DAY_OF_MONTH, utcCal.get(Calendar.DAY_OF_MONTH))
          }
          onSelectStartDate(cal.timeInMillis)
        }
        showDatePicker = false
      }
      ) {
        Text(stringResource(R.string.btn_ok), fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(onClick = { showDatePicker = false }) {
        Text(stringResource(R.string.btn_cancel))
      }
    }
    ) {
      DatePicker(state = datePickerState)
    }
  }
}
