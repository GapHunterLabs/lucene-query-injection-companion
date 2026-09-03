package dev.gaphunter.lucenequeryinjectioncompanion.model

import com.intellij.psi.PsiElement

/** A confirmed Lucene/Elasticsearch query injection sink: [taintedParameterName] flows (same method, direct reference or one-hop concatenation) into a `QueryBuilders.queryStringQuery(...)`/`.simpleQueryStringQuery(...)` argument whose static skeleton parses as well-formed Lucene query syntax. */
data class LuceneSinkHit(val anchor: PsiElement, val taintedParameterName: String)
