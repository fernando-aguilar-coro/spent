package com.app.spent.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Modern 5x4 Calculator Keypad with Parentheses, Basic Arithmetic, and Action Controls.
 */
@Composable
fun CustomNumericKeypad(
    onKeypadKey: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Row 1: AC, (, ), DEL
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PadKey(
                text = "AC",
                modifier = Modifier.weight(1f),
                textColor = MaterialTheme.colorScheme.error,
                onClick = { onKeypadKey("AC") }
            )
            PadKey(
                text = "(",
                modifier = Modifier.weight(1f),
                backgroundColor = MaterialTheme.colorScheme.secondaryContainer,
                textColor = MaterialTheme.colorScheme.onSecondaryContainer,
                onClick = { onKeypadKey("(") }
            )
            PadKey(
                text = ")",
                modifier = Modifier.weight(1f),
                backgroundColor = MaterialTheme.colorScheme.secondaryContainer,
                textColor = MaterialTheme.colorScheme.onSecondaryContainer,
                onClick = { onKeypadKey(")") }
            )
            PadKey(
                icon = Icons.AutoMirrored.Filled.Backspace,
                modifier = Modifier.weight(1f),
                onClick = { onKeypadKey("DEL") }
            )
        }

        // Row 2: 7, 8, 9, ÷
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PadKey(text = "7", modifier = Modifier.weight(1f), onClick = { onKeypadKey("7") })
            PadKey(text = "8", modifier = Modifier.weight(1f), onClick = { onKeypadKey("8") })
            PadKey(text = "9", modifier = Modifier.weight(1f), onClick = { onKeypadKey("9") })
            PadKey(
                text = "÷",
                modifier = Modifier.weight(1f),
                backgroundColor = MaterialTheme.colorScheme.primaryContainer,
                textColor = MaterialTheme.colorScheme.onPrimaryContainer,
                onClick = { onKeypadKey("÷") }
            )
        }

        // Row 3: 4, 5, 6, ×
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PadKey(text = "4", modifier = Modifier.weight(1f), onClick = { onKeypadKey("4") })
            PadKey(text = "5", modifier = Modifier.weight(1f), onClick = { onKeypadKey("5") })
            PadKey(text = "6", modifier = Modifier.weight(1f), onClick = { onKeypadKey("6") })
            PadKey(
                text = "×",
                modifier = Modifier.weight(1f),
                backgroundColor = MaterialTheme.colorScheme.primaryContainer,
                textColor = MaterialTheme.colorScheme.onPrimaryContainer,
                onClick = { onKeypadKey("×") }
            )
        }

        // Row 4: 1, 2, 3, -
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PadKey(text = "1", modifier = Modifier.weight(1f), onClick = { onKeypadKey("1") })
            PadKey(text = "2", modifier = Modifier.weight(1f), onClick = { onKeypadKey("2") })
            PadKey(text = "3", modifier = Modifier.weight(1f), onClick = { onKeypadKey("3") })
            PadKey(
                text = "-",
                modifier = Modifier.weight(1f),
                backgroundColor = MaterialTheme.colorScheme.primaryContainer,
                textColor = MaterialTheme.colorScheme.onPrimaryContainer,
                onClick = { onKeypadKey("-") }
            )
        }

        // Row 5: 0, ., =, +
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PadKey(text = "0", modifier = Modifier.weight(1f), onClick = { onKeypadKey("0") })
            PadKey(text = ".", modifier = Modifier.weight(1f), onClick = { onKeypadKey(".") })
            PadKey(
                text = "=",
                modifier = Modifier.weight(1f),
                backgroundColor = MaterialTheme.colorScheme.primary,
                textColor = MaterialTheme.colorScheme.onPrimary,
                onClick = { onKeypadKey("=") }
            )
            PadKey(
                text = "+",
                modifier = Modifier.weight(1f),
                backgroundColor = MaterialTheme.colorScheme.primaryContainer,
                textColor = MaterialTheme.colorScheme.onPrimaryContainer,
                onClick = { onKeypadKey("+") }
            )
        }
    }
}

@Composable
private fun PadKey(
    text: String? = null,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.94f else 1f, label = "KeyScale")

    Box(
        modifier = modifier
            .height(52.dp)
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .clickable(interactionSource = interactionSource, indication = null) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (text != null) {
            Text(
                text = text,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = textColor
            )
        } else if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor
            )
        }
    }
}
