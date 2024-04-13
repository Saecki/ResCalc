package local.hapra.rescalc

import kotlin.math.pow

enum class ResColor(
    val prefix: Int,
    val tolerance: Double?,
    val tempCoefficient: UInt?,
) {
    Silver(prefix = -2, tolerance = 0.1, tempCoefficient = null),
    Gold(prefix = -1, tolerance = 0.05, tempCoefficient = null),
    Black(prefix = 0, tolerance = null, tempCoefficient = null),
    Brown(prefix = 1, tolerance = 0.01, tempCoefficient = 100U),
    Red(prefix = 2, tolerance = 0.02, tempCoefficient = 50U),
    Orange(prefix = 3, tolerance = null, tempCoefficient = 15U),
    Yellow(prefix = 4, tolerance = null, tempCoefficient = 25U),
    Green(prefix = 5, tolerance = 0.005, tempCoefficient = null),
    Blue(prefix = 6, tolerance = 0.0025, tempCoefficient = 10U),
    Violet(prefix = 7, tolerance = 0.001, tempCoefficient = 5U),
    Gray(prefix = 8, tolerance = 0.0005, tempCoefficient = null),
    White(prefix = 9, tolerance = null, tempCoefficient = null);

    companion object {
        val FOR_FIRST_DIGIT = ResColor.entries.map { col ->
            col to col.firstDigit()
        }

        val FOR_OTHER_DIGIT = ResColor.entries.map { col ->
            col to col.otherDigit()
        }

        val FOR_MULTIPLIER = ResColor.entries.map { col ->
            col to col.prefix
        }

        val FOR_TOLERANCE = ResColor.entries.map { col ->
            col to col.tolerance
        }

        val FOR_TEMP_COEFFICIENT = ResColor.entries.map { col ->
            col to col.tempCoefficient
        }
    }

    fun firstDigit(): UInt? {
        return when {
            prefix > 0 -> prefix.toUInt()
            else -> null
        }
    }

    fun otherDigit(): UInt? {
        return when {
            prefix >= 0 -> prefix.toUInt()
            else -> null
        }
    }
}

data class Resistor(
    val num: UInt,
    val prefix: Int,
    val tolerance: Double,
    val tempCoefficient: UInt,
) {
    companion object {
        fun from(
            digits: Array<UInt>,
            prefix: Int,
            tolerance: Double,
            tempCoefficient: UInt
        ): Resistor {
            assert(digits.size == 3)
            val (digit0, digit1, digit2) = digits
            val num = 100U * digit0 + 10U * digit1 + digit2

            return Resistor(num, prefix, tolerance, tempCoefficient)
        }
    }

    /// In ohm
    fun resistance(): Double {
        return num.toDouble() * 10.0.pow(prefix)
    }
}