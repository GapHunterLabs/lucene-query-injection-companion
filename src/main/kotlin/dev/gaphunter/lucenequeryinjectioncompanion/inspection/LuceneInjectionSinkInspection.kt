package dev.gaphunter.lucenequeryinjectioncompanion.inspection

import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.psi.PsiFile
import dev.gaphunter.lucenequeryinjectioncompanion.detect.JavaLuceneInjectionSinkFinder
import dev.gaphunter.lucenequeryinjectioncompanion.model.LuceneSinkHit
import dev.gaphunter.lucenequeryinjectioncompanion.review.ReviewPrompt

/** Flags a `QueryBuilders.queryStringQuery(...)`/`.simpleQueryStringQuery(...)` call whose argument is tainted by an HTTP endpoint parameter -- CWE-943/CWE-400. See [JavaLuceneInjectionSinkFinder]. */
class LuceneInjectionSinkInspection : LocalInspectionTool() {

    companion object {
        const val MAX_FILE_LENGTH = 500_000
    }

    override fun checkFile(file: PsiFile, manager: InspectionManager, isOnTheFly: Boolean): Array<ProblemDescriptor>? {
        if (file.text.length > MAX_FILE_LENGTH) return null

        val hits = JavaLuceneInjectionSinkFinder.findAll(file)
        if (hits.isEmpty()) return null

        val problems = hits.map { hit ->
            manager.createProblemDescriptor(
                hit.anchor,
                messageFor(hit),
                isOnTheFly,
                emptyArray(),
                ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
            )
        }

        val path = file.virtualFile?.path
        if (path != null) {
            for (hit in hits) {
                val lineNumber = file.viewProvider.document?.getLineNumber(hit.anchor.textRange.startOffset) ?: -1
                ReviewPrompt.recordHit(file.project, "$path:$lineNumber:${hit.taintedParameterName}")
            }
        }

        return problems.toTypedArray()
    }

    private fun messageFor(hit: LuceneSinkHit): String =
        "Lucene/Elasticsearch query built from endpoint parameter '${hit.taintedParameterName}' -- an attacker's raw input " +
            "becomes query SOURCE TEXT re-parsed by Elasticsearch, enabling both filter bypass (CWE-943) and a crafted-query " +
            "DoS (CWE-400, the CVE-2023-31419 mechanism)"
}
