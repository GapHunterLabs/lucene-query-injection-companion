package dev.gaphunter.lucenequeryinjectioncompanion.inspection

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class LuceneInjectionSinkInspectionTest : BasePlatformTestCase() {

    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(LuceneInjectionSinkInspection::class.java)
    }

    fun `test a request parameter passed directly as the query is flagged`() {
        myFixture.configureByText(
            "RawController.java",
            """
            import org.elasticsearch.index.query.QueryBuilders;
            import org.springframework.web.bind.annotation.GetMapping;

            class RawController {
                @GetMapping("/search")
                Object run(String userQuery) {
                    return QueryBuilders.queryStringQuery(userQuery);
                }
            }
            """.trimIndent(),
        )
        val highlights = myFixture.doHighlighting()
        assertTrue(highlights.any { it.description?.contains("CWE-943") == true })
    }

    fun `test a request parameter concatenated into the query is flagged`() {
        myFixture.configureByText(
            "SearchController.java",
            """
            import org.elasticsearch.index.query.QueryBuilders;
            import org.springframework.web.bind.annotation.GetMapping;

            class SearchController {
                @GetMapping("/search")
                Object run(String name) {
                    return QueryBuilders.queryStringQuery("username:" + name);
                }
            }
            """.trimIndent(),
        )
        val highlights = myFixture.doHighlighting()
        assertTrue(highlights.any { it.description?.contains("CWE-943") == true })
    }

    fun `test a hardcoded query with no taint is not flagged`() {
        myFixture.configureByText(
            "SafeController.java",
            """
            import org.elasticsearch.index.query.QueryBuilders;
            import org.springframework.web.bind.annotation.GetMapping;

            class SafeController {
                @GetMapping("/safe")
                Object run(String unused) {
                    return QueryBuilders.queryStringQuery("username:admin");
                }
            }
            """.trimIndent(),
        )
        val highlights = myFixture.doHighlighting()
        assertTrue(highlights.none { it.description?.contains("CWE-943") == true })
    }

    fun `test a non-QueryBuilders type with a coincidentally named method is not flagged`() {
        myFixture.configureByText(
            "OtherBuilder.java",
            """
            import org.springframework.web.bind.annotation.GetMapping;

            class MyQueryBuilders {
                static Object queryStringQuery(String q) { return q; }
            }

            class OtherBuilder {
                @GetMapping("/x")
                Object handle(String q) {
                    return MyQueryBuilders.queryStringQuery(q);
                }
            }
            """.trimIndent(),
        )
        val highlights = myFixture.doHighlighting()
        assertTrue(highlights.none { it.description?.contains("CWE-943") == true })
    }

    fun `test a non-endpoint method with the same shape is not flagged`() {
        myFixture.configureByText(
            "Helper.java",
            """
            import org.elasticsearch.index.query.QueryBuilders;

            class Helper {
                Object run(String userQuery) {
                    return QueryBuilders.queryStringQuery(userQuery);
                }
            }
            """.trimIndent(),
        )
        val highlights = myFixture.doHighlighting()
        assertTrue(highlights.none { it.description?.contains("CWE-943") == true })
    }
}
