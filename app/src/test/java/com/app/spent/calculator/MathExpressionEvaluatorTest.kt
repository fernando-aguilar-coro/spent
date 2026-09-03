package com.app.spent.calculator

import com.app.spent.util.calculator.EvaluationResult
import com.app.spent.util.calculator.MathExpressionEvaluator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MathExpressionEvaluatorTest {

    @Test
    fun testBasicArithmetic() {
        val r1 = MathExpressionEvaluator.evaluate("2 + 3 * 4")
        assertTrue(r1 is EvaluationResult.Success)
        assertEquals(14.0, (r1 as EvaluationResult.Success).value, 0.001)
        assertEquals("14", r1.formatted)

        val r2 = MathExpressionEvaluator.evaluate("10 - 4 / 2")
        assertTrue(r2 is EvaluationResult.Success)
        assertEquals(8.0, (r2 as EvaluationResult.Success).value, 0.001)
        assertEquals("8", r2.formatted)
    }

    @Test
    fun testParenthesesAndPrecedence() {
        val r1 = MathExpressionEvaluator.evaluate("(2 + 3) * 4")
        assertTrue(r1 is EvaluationResult.Success)
        assertEquals(20.0, (r1 as EvaluationResult.Success).value, 0.001)
        assertEquals("20", r1.formatted)

        val r2 = MathExpressionEvaluator.evaluate("((10 - 2) * (3 + 1)) / 4")
        assertTrue(r2 is EvaluationResult.Success)
        assertEquals(8.0, (r2 as EvaluationResult.Success).value, 0.001)
        assertEquals("8", r2.formatted)
    }

    @Test
    fun testImplicitMultiplication() {
        val r1 = MathExpressionEvaluator.evaluate("2(3 + 4)")
        assertTrue(r1 is EvaluationResult.Success)
        assertEquals(14.0, (r1 as EvaluationResult.Success).value, 0.001)

        val r2 = MathExpressionEvaluator.evaluate("(2 + 3)(4)")
        assertTrue(r2 is EvaluationResult.Success)
        assertEquals(20.0, (r2 as EvaluationResult.Success).value, 0.001)

        val r3 = MathExpressionEvaluator.evaluate("(2)(3)")
        assertTrue(r3 is EvaluationResult.Success)
        assertEquals(6.0, (r3 as EvaluationResult.Success).value, 0.001)
    }

    @Test
    fun testUnaryOperators() {
        val r1 = MathExpressionEvaluator.evaluate("-5 + 10")
        assertTrue(r1 is EvaluationResult.Success)
        assertEquals(5.0, (r1 as EvaluationResult.Success).value, 0.001)

        val r2 = MathExpressionEvaluator.evaluate("10 * (-2)")
        assertTrue(r2 is EvaluationResult.Success)
        assertEquals(-20.0, (r2 as EvaluationResult.Success).value, 0.001)
    }

    @Test
    fun testAutoClosingHangingParentheses() {
        val r1 = MathExpressionEvaluator.evaluate("(10 + 5")
        assertTrue(r1 is EvaluationResult.Success)
        assertEquals(15.0, (r1 as EvaluationResult.Success).value, 0.001)

        val r2 = MathExpressionEvaluator.evaluate("((2 + 3) * (4 + 1")
        assertTrue(r2 is EvaluationResult.Success)
        assertEquals(25.0, (r2 as EvaluationResult.Success).value, 0.001)
    }

    @Test
    fun testTrailingOperators() {
        val r1 = MathExpressionEvaluator.evaluate("10 + 5 * ")
        assertTrue(r1 is EvaluationResult.Success)
        assertEquals(15.0, (r1 as EvaluationResult.Success).value, 0.001)
    }

    @Test
    fun testDecimalAndCommaNormalization() {
        val r1 = MathExpressionEvaluator.evaluate("12,50 + 7,50")
        assertTrue(r1 is EvaluationResult.Success)
        assertEquals(20.0, (r1 as EvaluationResult.Success).value, 0.001)

        val r2 = MathExpressionEvaluator.evaluate("0.1 + 0.2")
        assertTrue(r2 is EvaluationResult.Success)
        assertEquals(0.3, (r2 as EvaluationResult.Success).value, 0.001)
        assertEquals("0.3", r2.formatted)
    }

    @Test
    fun testAlternativeOperatorSymbols() {
        val r1 = MathExpressionEvaluator.evaluate("10 × 5 ÷ 2")
        assertTrue(r1 is EvaluationResult.Success)
        assertEquals(25.0, (r1 as EvaluationResult.Success).value, 0.001)

        val r2 = MathExpressionEvaluator.evaluate("10 * 5 / 2")
        assertTrue(r2 is EvaluationResult.Success)
        assertEquals(25.0, (r2 as EvaluationResult.Success).value, 0.001)
    }

    @Test
    fun testDivisionByZero() {
        val r1 = MathExpressionEvaluator.evaluate("10 / 0")
        assertTrue(r1 is EvaluationResult.Error)

        val r2 = MathExpressionEvaluator.evaluate("10 / (5 - 5)")
        assertTrue(r2 is EvaluationResult.Error)
    }

    @Test
    fun testEvaluateToPositiveDouble() {
        assertEquals(15.0, MathExpressionEvaluator.evaluateToPositiveDouble("(10 + 5)") ?: 0.0, 0.001)
        assertEquals(10.0, MathExpressionEvaluator.evaluateToPositiveDouble("10 - 20") ?: 0.0, 0.001)
        assertEquals(50.0, MathExpressionEvaluator.evaluateToPositiveDouble("-50") ?: 0.0, 0.001)
        assertEquals(null, MathExpressionEvaluator.evaluateToPositiveDouble("10 / 0"))
        assertEquals(null, MathExpressionEvaluator.evaluateToPositiveDouble(""))
    }
}
