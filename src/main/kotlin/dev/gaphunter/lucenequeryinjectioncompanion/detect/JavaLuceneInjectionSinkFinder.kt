package dev.gaphunter.lucenequeryinjectioncompanion.detect

import com.intellij.psi.JavaRecursiveElementWalkingVisitor
import com.intellij.psi.JavaTokenType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.PsiPolyadicExpression
import com.intellij.psi.PsiReferenceExpression
import dev.gaphunter.lucenequeryinjectioncompanion.lucene.LuceneParser
import dev.gaphunter.lucenequeryinjectioncompanion.model.LuceneSinkHit

/**
 * Finds `QueryBuilders.queryStringQuery(ARG)`/`.simpleQueryStringQuery(ARG)`
 * call sites whose ARG is built (same method, direct reference or
 * one-hop concatenation) from an HTTP endpoint parameter -- CWE-943
 * (injection) combined with CWE-400 (CVE-2023-31419 confirms a real
 * DoS via a maliciously crafted query string against Elasticsearch's
 * own `_search` API). Same "taint alone is the vulnerability, flagged
 * unconditionally" reasoning as this catalog's other sink finders.
 *
 * **The grammar's job:** a BARE tainted reference (the whole query
 * attacker-controlled, the single most dangerous shape) is flagged
 * unconditionally with no skeleton to validate; a concatenation's
 * static skeleton (tainted operand replaced by a placeholder) must
 * parse as well-formed Lucene query syntax before flagging --
 * noise reduction only, never a security gate (confirmed necessary
 * the hard way while building `ldap-injection-sink-companion`: gating
 * the bare-reference case on skeleton validity produced a false
 * negative on the single most dangerous shape, since a lone
 * placeholder has no structure to validate against).
 *
 * **v0.1 scope, stated honestly:** only `org.elasticsearch.index.query.QueryBuilders`
 * (checked by qualifier reference NAME, never resolved against the
 * real classpath); only same-method taint.
 */
object JavaLuceneInjectionSinkFinder {

    private val SINK_METHOD_NAMES = setOf("queryStringQuery", "simpleQueryStringQuery")

    fun findAll(file: PsiFile): List<LuceneSinkHit> {
        val hits = mutableListOf<LuceneSinkHit>()
        file.accept(object : JavaRecursiveElementWalkingVisitor() {
            override fun visitMethod(method: PsiMethod) {
                super.visitMethod(method)
                if (!ControllerEndpointSignals.isEndpointMethod(method)) return
                hits += hitsForMethod(method)
            }
        })
        return hits
    }

    private fun hitsForMethod(method: PsiMethod): List<LuceneSinkHit> {
        val body = method.body ?: return emptyList()
        val taintedNames = method.parameterList.parameters.map { it.name }.toSet()
        if (taintedNames.isEmpty()) return emptyList()

        val hits = mutableListOf<LuceneSinkHit>()
        body.accept(object : JavaRecursiveElementWalkingVisitor() {
            override fun visitMethodCallExpression(call: PsiMethodCallExpression) {
                super.visitMethodCallExpression(call)
                if (call.methodExpression.referenceName !in SINK_METHOD_NAMES) return
                if (!looksLikeQueryBuilders(call.methodExpression.qualifierExpression)) return
                val argument = call.argumentList.expressions.getOrNull(0) ?: return

                val taintedName = firstTaintedReference(argument, taintedNames) ?: return

                if (argument !is PsiReferenceExpression) {
                    val skeleton = buildTaintSkeleton(argument) ?: return
                    if (LuceneParser.parse(skeleton) == null) return
                }

                val anchor = call.methodExpression.referenceNameElement ?: call.methodExpression
                hits += LuceneSinkHit(anchor, taintedName)
            }
        })
        return hits
    }

    private fun firstTaintedReference(expression: PsiElement, taintedNames: Set<String>): String? {
        var found: String? = null
        expression.accept(object : JavaRecursiveElementWalkingVisitor() {
            override fun visitReferenceExpression(expr: PsiReferenceExpression) {
                if (found != null) return
                super.visitReferenceExpression(expr)
                val name = expr.referenceName
                if (name != null && name in taintedNames) found = name
            }
        })
        return found
    }

    private fun buildTaintSkeleton(expression: PsiExpression): String? = when (expression) {
        is PsiLiteralExpression -> (expression.value as? String) ?: PLACEHOLDER
        is PsiPolyadicExpression -> {
            if (expression.operationTokenType != JavaTokenType.PLUS) {
                null
            } else {
                expression.operands.joinToString("") { operand -> (operand as? PsiLiteralExpression)?.value as? String ?: PLACEHOLDER }
            }
        }
        else -> PLACEHOLDER
    }

    private const val PLACEHOLDER = "PLACEHOLDER"

    /** `QueryBuilders.queryStringQuery(...)` -- checked by the qualifier's own reference TEXT, never resolved against the real Elasticsearch classpath. */
    private fun looksLikeQueryBuilders(qualifier: PsiExpression?): Boolean =
        (qualifier as? PsiReferenceExpression)?.referenceName == "QueryBuilders"
}
