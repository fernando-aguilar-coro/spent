package com.app.spent.calculator

import com.app.spent.ui.transaction.InputMode
import com.app.spent.ui.transaction.InputModeStateMachine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InputModeStateMachineTest {

    @Test
    fun testTransitions() {
        assertEquals(InputMode.SYSTEM_KEYBOARD, InputModeStateMachine.onAmountFieldTapped(InputMode.IDLE))
        assertEquals(InputMode.SYSTEM_KEYBOARD, InputModeStateMachine.onAmountFieldTapped(InputMode.CUSTOM_KEYPAD))

        assertEquals(InputMode.CUSTOM_KEYPAD, InputModeStateMachine.onCalculatorButtonTapped(InputMode.IDLE))
        assertEquals(InputMode.CUSTOM_KEYPAD, InputModeStateMachine.onCalculatorButtonTapped(InputMode.SYSTEM_KEYBOARD))
        assertEquals(InputMode.SYSTEM_KEYBOARD, InputModeStateMachine.onCalculatorButtonTapped(InputMode.CUSTOM_KEYPAD))

        assertEquals(InputMode.OTHER_FIELD_ACTIVE, InputModeStateMachine.onOtherFieldFocused())

        val (backMode1, consumed1) = InputModeStateMachine.onBackPressed(InputMode.CUSTOM_KEYPAD)
        assertEquals(InputMode.IDLE, backMode1)
        assertTrue(consumed1)

        val (backMode2, consumed2) = InputModeStateMachine.onBackPressed(InputMode.SYSTEM_KEYBOARD)
        assertEquals(InputMode.SYSTEM_KEYBOARD, backMode2)
        assertFalse(consumed2)
    }

    @Test
    fun testInsertAtCursor() {
        // Appending to end
        val r1 = InputModeStateMachine.insertAtCursor("12", 2, 2, "3")
        assertEquals("123", r1.text)
        assertEquals(3, r1.cursorPosition)

        // Inserting in middle
        val r2 = InputModeStateMachine.insertAtCursor("13", 1, 1, "2")
        assertEquals("123", r2.text)
        assertEquals(2, r2.cursorPosition)

        // Replacing selection
        val r3 = InputModeStateMachine.insertAtCursor("100", 1, 3, "25")
        assertEquals("125", r3.text)
        assertEquals(3, r3.cursorPosition)
    }

    @Test
    fun testDeleteAtCursor() {
        // Deleting from end
        val r1 = InputModeStateMachine.deleteAtCursor("123", 3, 3)
        assertEquals("12", r1.text)
        assertEquals(2, r1.cursorPosition)

        // Deleting in middle
        val r2 = InputModeStateMachine.deleteAtCursor("123", 2, 2)
        assertEquals("13", r2.text)
        assertEquals(1, r2.cursorPosition)

        // Deleting selection
        val r3 = InputModeStateMachine.deleteAtCursor("12345", 1, 4)
        assertEquals("15", r3.text)
        assertEquals(1, r3.cursorPosition)

        // Deleting at start (no-op)
        val r4 = InputModeStateMachine.deleteAtCursor("123", 0, 0)
        assertEquals("123", r4.text)
        assertEquals(0, r4.cursorPosition)
    }

    @Test
    fun testInsertOperator() {
        // Normal insertion after digit
        val r1 = InputModeStateMachine.insertOperator("10", 2, 2, "+")
        assertEquals("10+", r1.text)
        assertEquals(3, r1.cursorPosition)

        // Replace operator when typing another operator consecutively
        val r2 = InputModeStateMachine.insertOperator("10+", 3, 3, "×")
        assertEquals("10×", r2.text)
        assertEquals(3, r2.cursorPosition)

        // Unary minus at beginning
        val r3 = InputModeStateMachine.insertOperator("", 0, 0, "-")
        assertEquals("-", r3.text)
        assertEquals(1, r3.cursorPosition)

        // Unary plus at beginning (blocked)
        val r4 = InputModeStateMachine.insertOperator("", 0, 0, "+")
        assertEquals("", r4.text)
        assertEquals(0, r4.cursorPosition)

        // Unary minus after '('
        val r5 = InputModeStateMachine.insertOperator("(", 1, 1, "-")
        assertEquals("(-", r5.text)
        assertEquals(2, r5.cursorPosition)
    }

    @Test
    fun testInsertDot() {
        // Normal dot
        val r1 = InputModeStateMachine.insertDot("12", 2, 2)
        assertEquals("12.", r1.text)
        assertEquals(3, r1.cursorPosition)

        // Duplicate dot in same number segment is blocked
        val r2 = InputModeStateMachine.insertDot("12.3", 4, 4)
        assertEquals("12.3", r2.text)
        assertEquals(4, r2.cursorPosition)

        // Dot after operator prepends "0."
        val r3 = InputModeStateMachine.insertDot("12+", 3, 3)
        assertEquals("12+0.", r3.text)
        assertEquals(5, r3.cursorPosition)
    }

    @Test
    fun testInsertParenthesis() {
        // Open parenthesis at start
        val r1 = InputModeStateMachine.insertParenthesis("", 0, 0, isClosing = false)
        assertEquals("(", r1.text)
        assertEquals(1, r1.cursorPosition)

        // Implicit multiplication when opening after digit: 5( -> 5×(
        val r2 = InputModeStateMachine.insertParenthesis("5", 1, 1, isClosing = false)
        assertEquals("5×(", r2.text)
        assertEquals(3, r2.cursorPosition)

        // Closing parenthesis when balanced (blocked)
        val r3 = InputModeStateMachine.insertParenthesis("5", 1, 1, isClosing = true)
        assertEquals("5", r3.text)
        assertEquals(1, r3.cursorPosition)

        // Closing parenthesis when open '(' exists and preceded by digit
        val r4 = InputModeStateMachine.insertParenthesis("(5 + 3", 6, 6, isClosing = true)
        assertEquals("(5 + 3)", r4.text)
        assertEquals(7, r4.cursorPosition)
    }
}
