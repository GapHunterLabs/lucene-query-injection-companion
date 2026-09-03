package dev.gaphunter.lucenequeryinjectioncompanion.lucene

/**
 * Recursive-descent parser over [LuceneLexer]'s tokens. Flat on
 * `AND`/`OR` (no precedence climbing) -- same reasoning as this
 * catalog's other grammars: this plugin never evaluates a query, only
 * confirms it parses as well-formed Lucene syntax (used to validate a
 * taint skeleton, see the finder), so operand precedence doesn't
 * change that answer.
 *
 * Every `parseXxx` returns null (never a best-effort partial tree) on
 * a mismatch; [parse] treats that, or trailing unconsumed input, as
 * unparseable.
 */
class LuceneParser private constructor(private val tokens: List<LuceneToken>) {

    private var pos = 0

    companion object {
        private val BOOL_KEYWORDS = setOf("AND", "OR")

        fun parse(text: String): LuceneNode? {
            val tokens = LuceneLexer.tokenize(text) ?: return null
            val parser = LuceneParser(tokens)
            val node = parser.parseQuery() ?: return null
            return if (parser.check(LuceneTokenType.EOF)) node else null
        }
    }

    private fun peek(): LuceneToken = tokens[pos]
    private fun advance(): LuceneToken = tokens[pos++]
    private fun check(type: LuceneTokenType): Boolean = peek().type == type
    private fun match(type: LuceneTokenType): LuceneToken? = if (check(type)) advance() else null

    private fun parseQuery(): LuceneNode? {
        var left = parseClause() ?: return null
        while (check(LuceneTokenType.IDENTIFIER) && peek().text in BOOL_KEYWORDS) {
            val operator = advance().text
            val right = parseClause() ?: return null
            left = LuceneBinary(left, operator, right)
        }
        return left
    }

    private fun parseClause(): LuceneNode? {
        val prefixOperator = when {
            check(LuceneTokenType.IDENTIFIER) && peek().text == "NOT" -> advance().text
            check(LuceneTokenType.PLUS) -> advance().text
            check(LuceneTokenType.MINUS) -> advance().text
            else -> null
        }
        val primary = parsePrimary() ?: return null
        val modified = parseModifier(primary)
        return if (prefixOperator != null) LuceneUnary(prefixOperator, modified) else modified
    }

    private fun parseModifier(base: LuceneNode): LuceneNode {
        var boost: String? = null
        var fuzzy: String? = null
        if (match(LuceneTokenType.CARET) != null) {
            boost = match(LuceneTokenType.NUMBER)?.text ?: "1"
        }
        if (match(LuceneTokenType.TILDE) != null) {
            fuzzy = match(LuceneTokenType.NUMBER)?.text ?: "1"
        }
        return if (boost != null || fuzzy != null) LuceneModified(base, boost, fuzzy) else base
    }

    private fun parsePrimary(): LuceneNode? {
        if (check(LuceneTokenType.LPAREN)) {
            advance()
            val inner = parseQuery() ?: return null
            return if (match(LuceneTokenType.RPAREN) != null) LuceneGroup(inner) else null
        }

        val field = parseOptionalField()

        if (check(LuceneTokenType.LBRACKET) || check(LuceneTokenType.LBRACE)) {
            val inclusive = check(LuceneTokenType.LBRACKET)
            advance()
            val lower = parseRangeSide() ?: return null
            if (!(check(LuceneTokenType.IDENTIFIER) && peek().text == "TO")) return null
            advance()
            val upper = parseRangeSide() ?: return null
            val closeType = if (inclusive) LuceneTokenType.RBRACKET else LuceneTokenType.RBRACE
            if (match(closeType) == null) return null
            return LuceneRange(field, lower, upper, inclusive)
        }

        return when {
            check(LuceneTokenType.STRING) -> LuceneTerm(field, advance().text, isPhrase = true)
            check(LuceneTokenType.IDENTIFIER) -> LuceneTerm(field, advance().text, isPhrase = false)
            check(LuceneTokenType.NUMBER) -> LuceneTerm(field, advance().text, isPhrase = false)
            else -> null
        }
    }

    /** Consumes `IDENTIFIER ':'` if present (one token of lookahead), returning the field name -- null when this primary has no field prefix. */
    private fun parseOptionalField(): String? {
        if (check(LuceneTokenType.IDENTIFIER) && tokens.getOrNull(pos + 1)?.type == LuceneTokenType.COLON) {
            val name = advance().text
            advance() // ':'
            return name
        }
        return null
    }

    private fun parseRangeSide(): String? = when {
        check(LuceneTokenType.STRING) -> advance().text
        check(LuceneTokenType.IDENTIFIER) -> advance().text
        check(LuceneTokenType.NUMBER) -> advance().text
        else -> null
    }
}
