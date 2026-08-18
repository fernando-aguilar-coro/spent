package com.app.spent.ui.transaction.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.spent.R
import com.app.spent.ui.theme.ExpenseRed
@Composable
fun TransactionTypeSelector(
selectedType: String,
onTypeSelected: (String) -> Unit,
modifier: Modifier = Modifier
) {
  Row(
  modifier = modifier
  .fillMaxWidth()
  .clip(RoundedCornerShape(16.dp))
  .background(MaterialTheme.colorScheme.surfaceVariant)
  .padding(4.dp),
  horizontalArrangement = Arrangement.SpaceEvenly
  ) {
    Box(
    modifier = Modifier
    .weight(1f)
    .clip(RoundedCornerShape(12.dp))
    .background(if (selectedType == "EXPENSE") ExpenseRed else Color.Transparent)
    .clickable { onTypeSelected("EXPENSE") }
    .padding(vertical = 12.dp),
    contentAlignment = Alignment.Center
    ) {
      Text(
      text = stringResource(R.string.type_expense),
      color = if (selectedType == "EXPENSE") Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
      fontWeight = FontWeight.SemiBold,
      fontSize = 14.sp,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
      )
    }

    Box(
    modifier = Modifier
    .weight(1f)
    .clip(RoundedCornerShape(12.dp))
    .background(if (selectedType == "INCOME") MaterialTheme.colorScheme.primary else Color.Transparent)
    .clickable { onTypeSelected("INCOME") }
    .padding(vertical = 12.dp),
    contentAlignment = Alignment.Center
    ) {
      Text(
      text = stringResource(R.string.type_income),
      color = if (selectedType == "INCOME") Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
      fontWeight = FontWeight.SemiBold,
      fontSize = 14.sp,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
      )
    }
  }
}
