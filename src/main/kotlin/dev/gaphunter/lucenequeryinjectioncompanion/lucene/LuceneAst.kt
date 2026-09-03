package dev.gaphunter.lucenequeryinjectioncompanion.lucene

/**
 * A hand-built AST for a real, useful subset of the Apache
 * Lucene/Elasticsearch `query_string` syntax -- the FIFTH full
 * custom grammar in this catalog (after regex, SpEL, XPath, and
 * LDAP). Introduces operators none of the other four grammars have at
 * all: inclusive/exclusive RANGE queries (`[1 TO 10]`/`{1 TO 10}`),
 * boosting (`term^2.0`), and fuzzy matching (`term~2`) -- genuinely
 * new parsing rules, not a variation on an existing pattern.
 */
sealed class LuceneNode

/** `field:value`, or a bare `value` (default field) when [field] is null. [isPhrase] marks a quoted phrase. */
data class LuceneTerm(val field: String?, val value: String, val isPhrase: Boolean) : LuceneNode()

/** `field:[lower TO upper]` (inclusive, [inclusive] = true) or `field:{lower TO upper}` (exclusive). */
data class LuceneRange(val field: String?, val lower: String, val upper: String, val inclusive: Boolean) : LuceneNode()

data class LuceneGroup(val inner: LuceneNode) : LuceneNode()

/** `left AND right` / `left OR right`. */
data class LuceneBinary(val left: LuceneNode, val operator: String, val right: LuceneNode) : LuceneNode()

/** `NOT x` / `+x` (required) / `-x` (prohibited). */
data class LuceneUnary(val operator: String, val operand: LuceneNode) : LuceneNode()

/** [base] with an optional boost (`^2.0`) and/or fuzzy (`~2`) modifier applied. */
data class LuceneModified(val base: LuceneNode, val boost: String?, val fuzzy: String?) : LuceneNode()
