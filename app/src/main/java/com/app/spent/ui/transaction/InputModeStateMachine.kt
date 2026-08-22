package com.app.spent.ui.transaction

/**
 * Declares the active input modes for the transaction creation / editing screen.
 */
enum class InputMode {
    IDLE,
    SYSTEM_KEYBOARD,
    CUSTOM_KEYPAD,
    OTHER_FIELD_ACTIVE
}

/**
 * State machine managing input modes and cursor-aware expression edits.
 */
object InputModeStateMachine {

    /**
     * Resolves the next InputMode when the user taps inside the Amount TextField.
     */
    fun onAmountFieldTapped(currentMode: InputMode): InputMode {
        return InputMode.SYSTEM_KEYBOARD
    }

    /**
     * Resolves the next InputMode when the user taps the Calculator toggle button.
     */
    fun onCalculatorButtonTapped(currentMode: InputMode): InputMode {
        return if (currentMode == InputMode.CUSTOM_KEYPAD) {
            InputMode.SYSTEM_KEYBOARD
        } else {
            InputMode.CUSTOM_KEYPAD
        }
    }

    /**
     * Resolves the next InputMode when the user focuses on a secondary field (e.g. Note/Merchant).
     */
    fun onOtherFieldFocused(): InputMode {
        return InputMode.OTHER_FIELD_ACTIVE
    }

    /**
     * Handles hardware / gesture back navigation.
     * Returns Pair(newMode, wasConsumed).
     * If wasConsumed == true, the screen should NOT exit.
     */
    fun onBackPressed(currentMode: InputMode): Pair<InputMode, Boolean> {
        return if (currentMode == InputMode.CUSTOM_KEYPAD) {
            Pair(InputMode.IDLE, true)
        } else {
            Pair(currentMode, false)
        }
    }

    // -------------------------------------------------------------
    // CURSOR-AWARE STRING MANIPULATION
    // -------------------------------------------------------------

    data class TextEditResult(
        val text: String,
        val cursorPosition: Int
    )

    /**
     * Inserts arbitrary text at the current cursor position or replaces active selection.
     */
    fun insertAtCursor(
        text: String,
        selectionStart: Int,
        selectionEnd: Int,
        insertion: String
    ): TextEditResult {
        val start = selectionStart.coerceIn(0, text.length)
        val end = selectionEnd.coerceIn(0, text.length)
        val minPos = minOf(start, end)
        val maxPos = maxOf(start, end)

        val newText = text.substring(0, minPos) + insertion + text.substring(maxPos)
        val newCursor = minPos + insertion.length
        return TextEditResult(newText, newCursor)
    }

    /**
     * Deletes the character preceding the cursor, or deletes the active text selection.
     */
    fun deleteAtCursor(
        text: String,
        selectionStart: Int,
        selectionEnd: Int
    ): TextEditResult {
        if (text.isEmpty()) return TextEditResult("", 0)

        val start = selectionStart.coerceIn(0, text.length)
        val end = selectionEnd.coerceIn(0, text.length)
        val minPos = minOf(start, end)
        val maxPos = maxOf(start, end)

        return if (minPos != maxPos) {
            // Selection deletion
            val newText = text.substring(0, minPos) + text.substring(maxPos)
            TextEditResult(newText, minPos)
        } else if (minPos > 0) {
            // Single character backspace
            val newText = text.substring(0, minPos - 1) + text.substring(minPos)
            TextEditResult(newText, minPos - 1)
        } else {
            TextEditResult(text, 0)
        }
    }

    /**
     * Inserts an arithmetic operator (+, -, ×, ÷) at the cursor with smart replacement.
     */
    fun insertOperator(
        text: String,
        selectionStart: Int,
        selectionEnd: Int,
        operator: String
    ): TextEditResult {
        val start = selectionStart.coerceIn(0, text.length)
        val end = selectionEnd.coerceIn(0, text.length)
        val minPos = minOf(start, end)
        val maxPos = maxOf(start, end)

        if (minPos != maxPos) {
            return insertAtCursor(text, minPos, maxPos, operator)
        }

        // If at beginning: only '-' is allowed as unary
        if (minPos == 0) {
            return if (operator == "-") {
                insertAtCursor(text, 0, 0, "-")
            } else {
                TextEditResult(text, 0)
            }
        }

        val charBefore = text[minPos - 1]
        val isOperatorBefore = charBefore in listOf('+', '-', '×', '÷', '*', '/')

        return if (isOperatorBefore) {
            // Replace previous operator with the newly tapped one
            val newText = text.substring(0, minPos - 1) + operator + text.substring(minPos)
            TextEditResult(newText, minPos)
        } else if (charBefore == '(') {
            // Only unary '-' allowed right after '('
            if (operator == "-") {
                insertAtCursor(text, minPos, minPos, "-")
            } else {
                TextEditResult(text, minPos)
            }
        } else {
            insertAtCursor(text, minPos, minPos, operator)
        }
    }

    /**
     * Inserts a decimal dot safely (preventing multiple dots in the same number segment).
     */
    fun insertDot(
        text: String,
        selectionStart: Int,
        selectionEnd: Int
    ): TextEditResult {
        val start = selectionStart.coerceIn(0, text.length)
        val end = selectionEnd.coerceIn(0, text.length)
        val minPos = minOf(start, end)
        val maxPos = maxOf(start, end)

        // Find the active number segment around cursor
        var segmentStart = minPos
        while (segmentStart > 0 && (text[segmentStart - 1].isDigit() || text[segmentStart - 1] == '.')) {
            segmentStart--
        }
        var segmentEnd = maxPos
        while (segmentEnd < text.length && (text[segmentEnd].isDigit() || text[segmentEnd] == '.')) {
            segmentEnd++
        }

        val currentSegment = text.substring(segmentStart, segmentEnd)
        if (currentSegment.contains('.')) {
            // Already has decimal point in this token
            return TextEditResult(text, maxPos)
        }

        // If at start or preceded by non-digit, insert "0."
        val charBefore = if (minPos > 0) text[minPos - 1] else null
        return if (charBefore == null || !charBefore.isDigit()) {
            insertAtCursor(text, minPos, maxPos, "0.")
        } else {
            insertAtCursor(text, minPos, maxPos, ".")
        }
    }

    /**
     * Handles smart insertion of '(' or ')'.
     */
    fun insertParenthesis(
        text: String,
        selectionStart: Int,
        selectionEnd: Int,
        isClosing: Boolean
    ): TextEditResult {
        val start = selectionStart.coerceIn(0, text.length)
        val end = selectionEnd.coerceIn(0, text.length)
        val minPos = minOf(start, end)
        val maxPos = maxOf(start, end)

        if (!isClosing) {
            // Inserting '('
            val charBefore = if (minPos > 0) text[minPos - 1] else null
            return if (charBefore != null && (charBefore.isDigit() || charBefore == ')')) {
                // Implicit multiplication: e.g. 5( -> 5×(
                insertAtCursor(text, minPos, maxPos, "×(")
            } else {
                insertAtCursor(text, minPos, maxPos, "(")
            }
        } else {
            // Inserting ')'
            val openCount = text.substring(0, minPos).count { it == '(' }
            val closeCount = text.substring(0, minPos).count { it == ')' }

            // Can only close if an unmatched '(' precedes
            if (openCount <= closeCount) {
                return TextEditResult(text, maxPos)
            }

            val charBefore = if (minPos > 0) text[minPos - 1] else null
            if (charBefore == '(' || charBefore in listOf('+', '-', '×', '÷', '.')) {
                // Cannot close immediately after operator or '('
                return TextEditResult(text, maxPos)
            }

            return insertAtCursor(text, minPos, maxPos, ")")
        }
    }
}
