package com.app.spent.ui.transaction.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.spent.R
import com.app.spent.ui.components.CategoryIconHelper
@Composable
fun AddCategoryDialog(
onDismiss: () -> Unit,
onSaveCategory: (name: String, colorHex: String, iconName: String) -> Unit
) {
  var newCategoryName by remember { mutableStateOf("") }
  var newCategoryColorHex by remember { mutableStateOf("#3B82F6") }
  var newCategoryIconName by remember { mutableStateOf("Category") }

  val colorPaletteRow1 = listOf(
  "#3B82F6", "#10B981", "#EF4444", "#F59E0B", "#8B5CF6", "#EC4899", "#06B6D4"
  )

  val colorPaletteRow2 = listOf(
  "#14B8A6", "#84CC16", "#F97316", "#6366F1", "#D946EF", "#0EA5E9", "#64748B"
  )

  val availableIcons = remember { CategoryIconHelper.availableIcons }

  AlertDialog(
  onDismissRequest = onDismiss,
  title = { Text(stringResource(R.string.create_category_title), fontWeight = FontWeight.Bold) },
  text = {
    Column {
      OutlinedTextField(
      value = newCategoryName,
      onValueChange = { newCategoryName = it },
      label = { Text(stringResource(R.string.category_name_label)) },
      placeholder = { Text(stringResource(R.string.category_name_placeholder)) },
      singleLine = true,
      modifier = Modifier.fillMaxWidth()
      )

      Spacer(modifier = Modifier.height(14.dp))

      // Icon Picker Section
      Text(
      text = "Select Icon",
      style = MaterialTheme.typography.bodySmall,
      fontWeight = FontWeight.SemiBold
      )
      Spacer(modifier = Modifier.height(6.dp))

      LazyRow(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier.fillMaxWidth()
      ) {
        items(availableIcons) { iconOption ->
          val isSelected = newCategoryIconName == iconOption.iconName
          val currentColor = Color(android.graphics.Color.parseColor(newCategoryColorHex))

          Box(
          modifier = Modifier
          .size(40.dp)
          .clip(RoundedCornerShape(12.dp))
          .background(if (isSelected) currentColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant)
          .then(
          if (isSelected) Modifier.border(BorderStroke(2.dp, currentColor), RoundedCornerShape(12.dp))
          else Modifier
          )
          .clickable { newCategoryIconName = iconOption.iconName },
          contentAlignment = Alignment.Center
          ) {
            Icon(
            imageVector = iconOption.icon,
            contentDescription = iconOption.label,
            tint = if (isSelected) currentColor else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Text(
        text = stringResource(R.string.select_color),
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.SemiBold
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
          modifier = Modifier
          .size(14.dp)
          .clip(CircleShape)
          .background(Color(android.graphics.Color.parseColor(newCategoryColorHex)))
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
          text = newCategoryColorHex.uppercase(),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          fontWeight = FontWeight.Medium
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Color Palette Grid (Row 1)
      Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween
      ) {
        colorPaletteRow1.forEach { colorHex ->
          val isSelected = newCategoryColorHex.equals(colorHex, ignoreCase = true)
          val color = Color(android.graphics.Color.parseColor(colorHex))

          Box(
          modifier = Modifier
          .size(34.dp)
          .clip(CircleShape)
          .background(color)
          .then(
          if (isSelected) Modifier.border(BorderStroke(2.5.dp, MaterialTheme.colorScheme.onSurface), CircleShape)
          else Modifier
          )
          .clickable { newCategoryColorHex = colorHex },
          contentAlignment = Alignment.Center
          ) {
            if (isSelected) {
              Icon(
              imageVector = Icons.Default.Check,
              contentDescription = "Selected",
              tint = Color.White,
              modifier = Modifier.size(18.dp)
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Color Palette Grid (Row 2)
      Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween
      ) {
        colorPaletteRow2.forEach { colorHex ->
          val isSelected = newCategoryColorHex.equals(colorHex, ignoreCase = true)
          val color = Color(android.graphics.Color.parseColor(colorHex))

          Box(
          modifier = Modifier
          .size(34.dp)
          .clip(CircleShape)
          .background(color)
          .then(
          if (isSelected) Modifier.border(BorderStroke(2.5.dp, MaterialTheme.colorScheme.onSurface), CircleShape)
          else Modifier
          )
          .clickable { newCategoryColorHex = colorHex },
          contentAlignment = Alignment.Center
          ) {
            if (isSelected) {
              Icon(
              imageVector = Icons.Default.Check,
              contentDescription = "Selected",
              tint = Color.White,
              modifier = Modifier.size(18.dp)
              )
            }
          }
        }
      }
    }
  },
  confirmButton = {
    Button(
    onClick = {
      if (newCategoryName.isNotBlank()) {
        onSaveCategory(newCategoryName.trim(), newCategoryColorHex, newCategoryIconName)
        onDismiss()
      }
    },
    enabled = newCategoryName.isNotBlank()
    ) {
      Text(stringResource(R.string.save_category))
    }
  },
  dismissButton = {
    TextButton(onClick = onDismiss) {
      Text(stringResource(R.string.btn_cancel))
    }
  }
  )
}
