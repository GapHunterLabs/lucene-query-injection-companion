package dev.gaphunter.lucenequeryinjectioncompanion.lucene

enum class LuceneTokenType {
    IDENTIFIER, STRING, NUMBER, COLON, LPAREN, RPAREN, LBRACKET, RBRACKET, LBRACE, RBRACE,
    CARET, TILDE, PLUS, MINUS, EOF,
}

data class LuceneToken(val type: LuceneTokenType, val text: String)

/**
 * Hand-rolled tokenizer for the Lucene query-string subset
 * [LuceneParser] consumes. Returns null (never a partial token list)
 * on any unrecognized character or unterminated string.
 *
 * **Term/identifier lexing, stated honestly:** a term can contain
 * letters, digits, `_`, `.`, and the wildcard characters `*`/`?` --
 * v0.1 deliberately does NOT include `-` in a term's own character
 * class (a hyphenated term like `field-name` is out of scope), since
 * `-` is also the real "prohibit this clause" prefix operator and
 * disambiguating the two contextually isn't worth the complexity for
 * what this plugin actually needs (confirming a taint skeleton
 * parses).
 */
object LuceneLexer {

    fun tokenize(input: String): List<LuceneToken>? {
        val tokens = mutableListOf<LuceneToken>()
        var i = 0
        while (i < input.length) {
            val c = input[i]
            when {
                c.isWhitespace() -> i++
                c == ':' -> { tokens += LuceneToken(LuceneTokenType.COLON, ":"); i++ }
                c == '(' -> { tokens += LuceneToken(LuceneTokenType.LPAREN, "("); i++ }
                c == ')' -> { tokens += LuceneToken(LuceneTokenType.RPAREN, ")"); i++ }
                c == '[' -> { tokens += LuceneToken(LuceneTokenType.LBRACKET, "["); i++ }
                c == ']' -> { tokens += LuceneToken(LuceneTokenType.RBRACKET, "]"); i++ }
                c == '{' -> { tokens += LuceneToken(LuceneTokenType.LBRACE, "{"); i++ }
                c == '}' -> { tokens += LuceneToken(LuceneTokenType.RBRACE, "}"); i++ }
                c == '^' -> { tokens += LuceneToken(LuceneTokenType.CARET, "^"); i++ }
                c == '~' -> { tokens += LuceneToken(LuceneTokenType.TILDE, "~"); i++ }
                c == '+' -> { tokens += LuceneToken(LuceneTokenType.PLUS, "+"); i++ }
                c == '-' -> { tokens += LuceneToken(LuceneTokenType.MINUS, "-"); i++ }
                c == '"' -> {
                    val result = readString(input, i) ?: return null
                    tokens += LuceneToken(LuceneTokenType.STRING, result.first)
                    i = result.second
                }
                c.isDigit() -> {
                    val end = scanNumber(input, i)
                    tokens += LuceneToken(LuceneTokenType.NUMBER, input.substring(i, end))
                    i = end
                }
                isTermChar(c) -> {
                    val start = i
                    while (i < input.length && isTermChar(input[i])) i++
                    tokens += LuceneToken(LuceneTokenType.IDENTIFIER, input.substring(start, i))
                }
                else -> return null
            }
        }
        tokens += LuceneToken(LuceneTokenType.EOF, "")
        return tokens
    }

    private fun isTermChar(c: Char): Boolean = c.isLetterOrDigit() || c == '_' || c == '.' || c == '*' || c == '?'

    private fun readString(input: String, openIndex: Int): Pair<String, Int>? {
        var i = openIndex + 1
        val sb = StringBuilder()
        while (i < input.length) {
            when (input[i]) {
                '\\' -> {
                    if (i + 1 >= input.length) return null
                    sb.append(input[i + 1])
                    i += 2
                }
                '"' -> return sb.toString() to (i + 1)
                else -> {
                    sb.append(input[i])
                    i++
                }
            }
        }
        return null
    }

    private fun scanNumber(input: String, start: Int): Int {
        var i = start
        while (i < input.length && input[i].isDigit()) i++
        if (i < input.length && input[i] == '.' && i + 1 < input.length && input[i + 1].isDigit()) {
            i++
            while (i < input.length && input[i].isDigit()) i++
        }
        return i
    }
}
