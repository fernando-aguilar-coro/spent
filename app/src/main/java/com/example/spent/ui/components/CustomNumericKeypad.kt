package com.example.spent.ui.components

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
import androidx.compose.material.icons.filled.Check
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


@Composable
fun CustomNumericKeypad(
    currentExpression: String,
    onExpressionChanged: (String) -> Unit,
    onConfirm: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    fun handleKey(key: String) {
        when (key) {
            "AC" -> onExpressionChanged("")
            "DEL" -> {
                if (currentExpression.isNotEmpty()) {
                    onExpressionChanged(currentExpression.dropLast(1))
                }
            }
            "=" -> {
                val evaluated = evaluateSimpleMath(currentExpression)
                onExpressionChanged(evaluated)
                onConfirm?.invoke()
            }
            "+", "-", "×", "÷" -> {
                if (currentExpression.isNotEmpty()) {
                    val lastChar = currentExpression.last()
                    if (lastChar in listOf('+', '-', '×', '÷')) {
                        onExpressionChanged(currentExpression.dropLast(1) + key)
                    } else {
                        // First evaluate any pending previous calculation to keep it clean
                        val intermediate = evaluateSimpleMath(currentExpression)
                        onExpressionChanged(intermediate + key)
                    }
                }
            }
            "." -> {
                val tokens = currentExpression.split('+', '-', '×', '÷')
                val lastToken = tokens.lastOrNull() ?: ""
                if (!lastToken.contains('.')) {
                    onExpressionChanged(if (lastToken.isEmpty()) currentExpression + "0." else currentExpression + ".")
                }
            }
            else -> {
                // Digits 0-9
                val tokens = currentExpression.split('+', '-', '×', '÷')
                val lastToken = tokens.lastOrNull() ?: ""
                if (lastToken.contains('.')) {
                    val decimals = lastToken.substringAfter('.')
                    if (decimals.length < 2) {
                        onExpressionChanged(currentExpression + key)
                    }
                } else {
                    if (lastToken == "0" && key != "0") {
                        onExpressionChanged(currentExpression.dropLast(1) + key)
                    } else if (lastToken != "0" || key != "0") {
                        onExpressionChanged(currentExpression + key)
                    }
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Row 1: AC, ÷, ×, DEL
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PadKey(
                text = "AC",
                modifier = Modifier.weight(1f),
                textColor = MaterialTheme.colorScheme.error,
                onClick = { handleKey("AC") }
            )
            PadKey(
                text = "÷",
                modifier = Modifier.weight(1f),
                backgroundColor = MaterialTheme.colorScheme.primaryContainer,
                textColor = MaterialTheme.colorScheme.onPrimaryContainer,
                onClick = { handleKey("÷") }
            )
            PadKey(
                text = "×",
                modifier = Modifier.weight(1f),
                backgroundColor = MaterialTheme.colorScheme.primaryContainer,
                textColor = MaterialTheme.colorScheme.onPrimaryContainer,
                onClick = { handleKey("×") }
            )
            PadKey(
                icon = Icons.AutoMirrored.Filled.Backspace,
                modifier = Modifier.weight(1f),
                onClick = { handleKey("DEL") }
            )
        }

        // Row 2: 7, 8, 9, -
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PadKey(text = "7", modifier = Modifier.weight(1f), onClick = { handleKey("7") })
            PadKey(text = "8", modifier = Modifier.weight(1f), onClick = { handleKey("8") })
            PadKey(text = "9", modifier = Modifier.weight(1f), onClick = { handleKey("9") })
            PadKey(
                text = "-",
                modifier = Modifier.weight(1f),
                backgroundColor = MaterialTheme.colorScheme.primaryContainer,
                textColor = MaterialTheme.colorScheme.onPrimaryContainer,
                onClick = { handleKey("-") }
            )
        }

        // Row 3: 4, 5, 6, +
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PadKey(text = "4", modifier = Modifier.weight(1f), onClick = { handleKey("4") })
            PadKey(text = "5", modifier = Modifier.weight(1f), onClick = { handleKey("5") })
            PadKey(text = "6", modifier = Modifier.weight(1f), onClick = { handleKey("6") })
            PadKey(
                text = "+",
                modifier = Modifier.weight(1f),
                backgroundColor = MaterialTheme.colorScheme.primaryContainer,
                textColor = MaterialTheme.colorScheme.onPrimaryContainer,
                onClick = { handleKey("+") }
            )
        }

        // Row 4: 1, 2, 3, ✓ (Evaluate & Confirm)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PadKey(text = "1", modifier = Modifier.weight(1f), onClick = { handleKey("1") })
            PadKey(text = "2", modifier = Modifier.weight(1f), onClick = { handleKey("2") })
            PadKey(text = "3", modifier = Modifier.weight(1f), onClick = { handleKey("3") })
            PadKey(
                icon = Icons.Default.Check,
                modifier = Modifier.weight(1f),
                backgroundColor = MaterialTheme.colorScheme.primary,
                textColor = Color.White,
                onClick = { handleKey("=") }
            )
        }

        // Row 5: 0, . and 00
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PadKey(
                text = "0",
                modifier = Modifier.weight(2f),
                onClick = { handleKey("0") }
            )
            PadKey(
                text = ".",
                modifier = Modifier.weight(1f),
                onClick = { handleKey(".") }
            )
            PadKey(
                text = "00",
                modifier = Modifier.weight(1f),
                onClick = {
                    handleKey("0")
                    handleKey("0")
                }
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
            .height(54.dp)
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

private fun evaluateSimpleMath(expression: String): String {
    if (expression.isBlank()) return ""
    val cleaned = expression.trim()

    // Single number
    if (!cleaned.contains('+') && !cleaned.contains('-') && !cleaned.contains('×') && !cleaned.contains('÷')) {
        val num = cleaned.toDoubleOrNull() ?: return expression
        return formatResult(num)
    }

    // Parse simple binary operator
    val opIndex = cleaned.indexOfAny(charArrayOf('+', '-', '×', '÷'))
    if (opIndex <= 0 || opIndex == cleaned.length - 1) return expression

    val leftStr = cleaned.substring(0, opIndex)
    val op = cleaned[opIndex]
    val rightStr = cleaned.substring(opIndex + 1)

    val left = leftStr.toDoubleOrNull() ?: return expression
    val right = rightStr.toDoubleOrNull() ?: return expression

    val result = when (op) {
        '+' -> left + right
        '-' -> left - right
        '×' -> left * right
        '÷' -> if (right != 0.0) left / right else return expression
        else -> return expression
    }

    return formatResult(result)
}

private fun formatResult(value: Double): String {
    return if (value % 1.0 == 0.0) {
        value.toLong().toString()
    } else {
        String.format(java.util.Locale.US, "%.2f", value)
    }
}
