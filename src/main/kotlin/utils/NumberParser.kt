package utils

/**
 * Universal number parser from "dirty" text (prices/amounts).
 * ----------------------------------------------------------------------------
 * EN:
 * Extracts the first numeric value from an arbitrary string and converts it to Double.
 * Useful for prices/amounts that may include non-breaking or thin spaces,
 * currency symbols, apostrophes, mixed thousand separators, and decimal separators.
 *
 * What it does:
 *  - Normalizes "special spaces" (NBSP, thin space, etc.) into a regular space
 *  - Extracts the first numeric token, discarding currency/text tails
 *  - Heuristically determines the decimal separator (comma or dot)
 *  - Removes thousand separators (spaces, apostrophes, extra dots/commas)
 *
 * Separator heuristics:
 *  - If both a comma and a dot are present — the rightmost one is considered decimal.
 *    The other is treated as a thousand separator and removed.
 *  - If only one of {',' '.'} is present:
 *      * if the last "chunk" after the separator has length 3 and there are other groups of 3,
 *        the separator is likely a thousand separator → removed;
 *      * otherwise — considered decimal.
 *
 * Examples:
 *  - "€1 234,56"        → 1234.56
 *  - "USD 12,345.70"    → 12345.70
 *  - "1.234,56 ₽"       → 1234.56
 *  - "- 3’141’592,65"   → -3141592.65
 *  - "≈2.000"           → 2000.0
 *  - "2,000"            → 2000.0   (interpreted as thousand separator)
 *  - "0,99 kg"          → 0.99
 *  - "abc"              → null
 *
 * Limitations:
 *  - Only the first numeric occurrence is parsed.
 *  - Scientific notation (e.g., "1e3") is not supported.
 *  - Exotic groupings other than space/apostrophe/dot/comma are not considered.
 *  - Local formatting rules may differ from the applied heuristics.
 *
 * Complexity:
 *  - Time — O(n) in length of the input string; additional memory — O(n).
 *
 * Example usage (console output in English):
 * ```
 * fun main() {
 *     val samples = listOf("€1 234,56", "USD 12,345.70", "abc")
 *     for (s in samples) {
 *         println("Input: \"$s\" -> ${NumberParser.parseNumber(s)}")
 *     }
 * }
 * // Output:
 * // Input: "€1 234,56" -> 1234.56
 * // Input: "USD 12,345.70" -> 12345.7
 * // Input: "abc" -> null
 * ```
 */
object NumberParser {
    // Special spaces normalized into a regular space
    private val spaceChars = "\u00A0\u202F\u2009\u2007\u2060\u2002\u2003\u2004\u2005\u2006\u2008\u3000"

    /**
     * Parses a Double from any string containing a number.
     * Returns null if no number is found or parsing fails.
     *
     * @param text Input string (may be null)
     * @return Parsed Double value or null
     */
    fun parseNumber(text: String?): Double? {
        if (text == null) return null
        if (text.isBlank()) return null

        // Normalize Unicode spaces into regular space
        val normalized = text.map { ch -> if (spaceChars.contains(ch)) ' ' else ch }.joinToString("")

        // Find the first numeric token with possible thousand/decimal separators
        val regex = Regex("[-+]?\\d[\\d\\s'.,]*")
        val match = regex.find(normalized) ?: return null
        var token = match.value.trim()
        if (token.isEmpty()) return null

        // Remove leading plus
        token = token.removePrefix("+")

        // Collapse multiple spaces
        token = token.replace(Regex("\\s+"), " ")

        // Heuristically determine decimal separator and parse
        return parseTokenToDouble(token)
    }

    /**
     * Converts the extracted numeric token to Double, applying heuristics
     * for decimal separator selection and removing thousand separators.
     *
     * Details:
     *  - If both ',' and '.' exist, the rightmost one is considered decimal.
     *  - If only one exists, groups between separators are analyzed:
     *      * if "middle" groups have length 3 and the last fragment is also 3 digits —
     *        treat it as thousands grouping;
     *      * otherwise — treat it as decimal separator.
     */
    private fun parseTokenToDouble(raw: String): Double? {
        var token = raw
        val hasComma = token.contains(',')
        val hasDot = token.contains('.')

        // Remove spaces and apostrophes as potential thousand separators
        token = token.replace(" ", "").replace("'", "")

        var decimalSep: Char? = null
        if (hasComma && hasDot) {
            // Both present: the rightmost is decimal, the other is thousands
            val lastComma = token.lastIndexOf(',')
            val lastDot = token.lastIndexOf('.')
            decimalSep = if (lastComma > lastDot) ',' else '.'
        } else if (hasComma xor hasDot) {
            val sep = if (hasComma) ',' else '.'
            val parts = token.split(sep)
            if (parts.size == 1) {
                decimalSep = null
            } else {
                val lastPart = parts.last()
                // If >1 separators and all middle groups are length 3 → likely thousands
                val middleGroups = parts.drop(1).dropLast(1)
                val allMiddleAre3 = middleGroups.isNotEmpty() && middleGroups.all { it.length == 3 }
                decimalSep = when {
                    // Prefer thousands interpretation when last fragment length is 3
                    // and there are other groups/middle groups of 3 digits
                    lastPart.length == 3 && (parts.size > 2 || allMiddleAre3) -> null
                    // otherwise — decimal
                    else -> sep
                }
            }
        }

        // Remove all non-decimal separators (dots/commas used as thousands)
        token = when (decimalSep) {
            ',' -> token.replace(".", "")
            '.' -> token.replace(",", "")
            else -> token.replace(",", "").replace(".", "")
        }

        // If decimal separator determined, keep only the last and replace with dot
        if (decimalSep != null) {
            val sep = decimalSep!!
            val lastIndex = token.lastIndexOf(sep)
            if (lastIndex >= 0) {
                val before = token.substring(0, lastIndex).replace(sep.toString(), "")
                val after = token.substring(lastIndex + 1)
                token = before + '.' + after
            } else {
                // Fallback (if separator not found)
                token = token.replace(sep, '.')
            }
        }

        // Expected result: -?\d+(\.\d+)?
        return token.toDoubleOrNull()
    }
}
