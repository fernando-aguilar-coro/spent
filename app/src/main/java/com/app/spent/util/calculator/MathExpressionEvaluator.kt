package com.app.spent.util.calculator

import java.util.Locale

/**
 * Result of evaluating a mathematical expression.
 */
sealed class EvaluationResult {
    data class Success(val value: Double, val formatted: String) : EvaluationResult()
    data class Error(val message: String) : EvaluationResult()
}

/**
 * Robust mathematical expression parser and evaluator.
 * Supports:
 * - Basic arithmetic (+, -, ×, ÷, *, /)
 * - Arbitrary nested parentheses ( )
 * - Standard operator precedence (* / before + -)
 * - Unary plus and minus (-5, +10, -(2 + 3))
 * - Implicit multiplication (2(3+4), (2)(3))
 * - Auto-balancing of trailing unclosed parentheses
 * - Trimming of trailing operators for live evaluation
 * - Safe division by zero handling
 */
object MathExpressionEvaluator {

    fun evaluate(expression: String): EvaluationResult {
        if (expression.isBlank()) {
            return EvaluationResult.Error("Empty expression")
        }

        val sanitized = sanitize(expression)
        if (sanitized.isBlank()) {
            return EvaluationResult.Error("Empty expression")
        }

        return try {
            val tokens = tokenize(sanitized)
            if (tokens.isEmpty()) {
                return EvaluationResult.Error("No valid tokens")
            }
            val parser = Parser(tokens)
            val result = parser.parse()
            if (result.isNaN() || result.isInfinite()) {
                EvaluationResult.Error("Math domain error (e.g., division by zero)")
            } else {
                EvaluationResult.Success(result, formatResult(result))
            }
        } catch (e: Exception) {
            EvaluationResult.Error(e.message ?: "Invalid expression")
        }
    }

    /**
     * Attempts evaluation, returning double if valid and > 0, otherwise null.
     */
    fun evaluateToPositiveDouble(expression: String): Double? {
        val result = evaluate(expression)
        return if (result is EvaluationResult.Success && result.value > 0) {
            result.value
        } else {
            null
        }
    }

    /**
     * Formats a calculated numeric value cleanly for display and storage.
     */
    fun formatResult(value: Double): String {
        if (value.isNaN() || value.isInfinite()) return ""
        return if (value % 1.0 == 0.0 && Math.abs(value) < 1e15) {
            value.toLong().toString()
        } else {
            // Up to 2 decimal places, trimming trailing zeros if redundant (e.g. 15.50 -> 15.5, but keep clean)
            val formatted = String.format(Locale.US, "%.2f", value)
            if (formatted.endsWith(".00")) {
                formatted.substringBefore(".00")
            } else if (formatted.endsWith("0") && formatted.contains(".")) {
                formatted.dropLast(1)
            } else {
                formatted
            }
        }
    }

    /**
     * Sanitizes input string:
     * - Normalizes commas to decimal points
     * - Normalizes alternate multiplication/division symbols
     * - Trims trailing operators for evaluation
     * - Auto-balances trailing unclosed parentheses
     */
    internal fun sanitize(input: String): String {
        var clean = input.trim()
            .replace(',', '.')
            .replace('*', '×')
            .replace('x', '×')
            .replace('X', '×')
            .replace('/', '÷')
            .replace("\\s+".toRegex(), "")

        // Remove trailing operators so incomplete expressions can evaluate live
        while (clean.isNotEmpty() && clean.last() in listOf('+', '-', '×', '÷', '.')) {
            clean = clean.dropLast(1)
        }

        // Count open and closing parentheses
        val openCount = clean.count { it == '(' }
        val closeCount = clean.count { it == ')' }
        if (openCount > closeCount) {
            clean += ")".repeat(openCount - closeCount)
        }

        return clean
    }

    // -------------------------------------------------------------
    // TOKENIZER
    // -------------------------------------------------------------
    internal enum class TokenType {
        NUMBER, PLUS, MINUS, MULTIPLY, DIVIDE, LPAREN, RPAREN
    }

    internal data class Token(val type: TokenType, val value: Double = 0.0)

    internal fun tokenize(input: String): List<Token> {
        val tokens = mutableListOf<Token>()
        var i = 0
        val len = input.length

        fun addImplicitMultiplyIfNeeded() {
            if (tokens.isNotEmpty()) {
                val lastType = tokens.last().type
                if (lastType == TokenType.NUMBER || lastType == TokenType.RPAREN) {
                    tokens.add(Token(TokenType.MULTIPLY))
                }
            }
        }

        while (i < len) {
            val c = input[i]
            when {
                c.isDigit() || c == '.' -> {
                    val sb = StringBuilder()
                    var hasDot = false
                    while (i < len && (input[i].isDigit() || input[i] == '.')) {
                        if (input[i] == '.') {
                            if (hasDot) break // Stop at second dot in a token
                            hasDot = true
                        }
                        sb.append(input[i])
                        i++
                    }
                    val numStr = sb.toString()
                    val num = numStr.toDoubleOrNull() ?: 0.0
                    // Check if previous token was RPAREN -> implicit multiplication (e.g. (2)3)
                    if (tokens.isNotEmpty() && tokens.last().type == TokenType.RPAREN) {
                        tokens.add(Token(TokenType.MULTIPLY))
                    }
                    tokens.add(Token(TokenType.NUMBER, num))
                    continue
                }
                c == '+' -> {
                    tokens.add(Token(TokenType.PLUS))
                }
                c == '-' -> {
                    tokens.add(Token(TokenType.MINUS))
                }
                c == '×' -> {
                    tokens.add(Token(TokenType.MULTIPLY))
                }
                c == '÷' -> {
                    tokens.add(Token(TokenType.DIVIDE))
                }
                c == '(' -> {
                    // Check for implicit multiplication: 2(3) -> 2 * (3)
                    addImplicitMultiplyIfNeeded()
                    tokens.add(Token(TokenType.LPAREN))
                }
                c == ')' -> {
                    tokens.add(Token(TokenType.RPAREN))
                }
            }
            i++
        }
        return tokens
    }

    // -------------------------------------------------------------
    // PARSER (Recursive Descent)
    // Grammar:
    // Expression := Term (('+' | '-') Term)*
    // Term       := Factor (('×' | '÷') Factor)*
    // Factor     := ('+' | '-')? Primary
    // Primary    := NUMBER | '(' Expression ')'
    // -------------------------------------------------------------
    private class Parser(private val tokens: List<Token>) {
        private var pos = 0

        fun parse(): Double {
            val result = parseExpression()
            if (pos < tokens.size) {
                // Ignore unexpected trailing tokens or throw
            }
            return result
        }

        private fun peek(): Token? = if (pos < tokens.size) tokens[pos] else null
        private fun consume(): Token = tokens[pos++]

        private fun parseExpression(): Double {
            var left = parseTerm()
            while (true) {
                val current = peek() ?: break
                if (current.type == TokenType.PLUS) {
                    consume()
                    left += parseTerm()
                } else if (current.type == TokenType.MINUS) {
                    consume()
                    left -= parseTerm()
                } else {
                    break
                }
            }
            return left
        }

        private fun parseTerm(): Double {
            var left = parseFactor()
            while (true) {
                val current = peek() ?: break
                if (current.type == TokenType.MULTIPLY) {
                    consume()
                    left *= parseFactor()
                } else if (current.type == TokenType.DIVIDE) {
                    consume()
                    val divisor = parseFactor()
                    if (divisor == 0.0) {
                        return Double.NaN // Flag division by zero
                    }
                    left /= divisor
                } else {
                    break
                }
            }
            return left
        }

        private fun parseFactor(): Double {
            val current = peek() ?: return 0.0
            if (current.type == TokenType.PLUS) {
                consume()
                return parseFactor()
            }
            if (current.type == TokenType.MINUS) {
                consume()
                return -parseFactor()
            }
            return parsePrimary()
        }

        private fun parsePrimary(): Double {
            val current = peek() ?: return 0.0
            if (current.type == TokenType.NUMBER) {
                consume()
                return current.value
            }
            if (current.type == TokenType.LPAREN) {
                consume() // consume '('
                val inside = parseExpression()
                if (peek()?.type == TokenType.RPAREN) {
                    consume() // consume ')'
                }
                return inside
            }
            // Unexpected token, skip to avoid infinite loop
            consume()
            return 0.0
        }
    }
}
