package com.app.spent.ui.settings.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.app.spent.R
import com.app.spent.ui.theme.ExpenseRed
@Composable
fun ResetDataButton(
onResetClick: () -> Unit
) {
  var showConfirmDialog by remember { mutableStateOf(false) }

  Button(
  onClick = { showConfirmDialog = true },
  modifier = Modifier
  .fillMaxWidth()
  .height(52.dp),
  contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
  shape = RoundedCornerShape(16.dp),
  colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed)
  ) {
    Icon(Icons.Default.Delete, contentDescription = "Reset Data")
    Spacer(modifier = Modifier.width(8.dp))
    Text(
    text = stringResource(R.string.reset_data_btn),
    fontWeight = FontWeight.Bold,
    maxLines = 1,
    softWrap = false,
    overflow = TextOverflow.Ellipsis
    )
  }

  if (showConfirmDialog) {
    AlertDialog(
    onDismissRequest = { showConfirmDialog = false },
    title = { Text(stringResource(R.string.reset_data_confirm_title), fontWeight = FontWeight.Bold) },
    text = { Text(stringResource(R.string.reset_data_confirm_msg)) },
    confirmButton = {
      Button(
      onClick = {
        onResetClick()
        showConfirmDialog = false
      },
      colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed)
      ) {
        Text(stringResource(R.string.reset_data_btn), fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(onClick = { showConfirmDialog = false }) {
        Text(stringResource(R.string.btn_cancel))
      }
    }
    )
  }
}
