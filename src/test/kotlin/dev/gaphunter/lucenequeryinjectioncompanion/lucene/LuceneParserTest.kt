package dev.gaphunter.lucenequeryinjectioncompanion.lucene

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LuceneParserTest {

    @Test
    fun `a bare term parses`() {
        val node = LuceneParser.parse("admin") as LuceneTerm
        assertEquals("admin", node.value)
        assertNull(node.field)
    }

    @Test
    fun `a field-qualified term parses`() {
        val node = LuceneParser.parse("username:admin") as LuceneTerm
        assertEquals("username", node.field)
        assertEquals("admin", node.value)
    }

    @Test
    fun `a quoted phrase parses`() {
        val node = LuceneParser.parse("title:\"hello world\"") as LuceneTerm
        assertTrue(node.isPhrase)
        assertEquals("hello world", node.value)
    }

    @Test
    fun `an AND of two terms parses`() {
        val node = LuceneParser.parse("username:admin AND active:true") as LuceneBinary
        assertEquals("AND", node.operator)
    }

    @Test
    fun `an inclusive range query parses`() {
        val node = LuceneParser.parse("age:[18 TO 65]") as LuceneRange
        assertEquals("age", node.field)
        assertEquals("18", node.lower)
        assertEquals("65", node.upper)
        assertTrue(node.inclusive)
    }

    @Test
    fun `an exclusive range query parses`() {
        val node = LuceneParser.parse("age:{18 TO 65}") as LuceneRange
        assertTrue(!node.inclusive)
    }

    @Test
    fun `boost and fuzzy modifiers parse`() {
        val boosted = LuceneParser.parse("title:test^2.0") as LuceneModified
        assertEquals("2.0", boosted.boost)

        val fuzzy = LuceneParser.parse("title:test~2") as LuceneModified
        assertEquals("2", fuzzy.fuzzy)
    }

    @Test
    fun `NOT and prohibit prefix parse`() {
        assertTrue(LuceneParser.parse("NOT active:false") is LuceneUnary)
        val prohibited = LuceneParser.parse("-active:false") as LuceneUnary
        assertEquals("-", prohibited.operator)
    }

    @Test
    fun `a parenthesized group with nested boolean logic parses`() {
        val node = LuceneParser.parse("(username:admin OR username:root) AND active:true")
        assertNotNull(node)
        assertTrue(node is LuceneBinary)
    }

    @Test
    fun `a wildcard term parses as ordinary text`() {
        val node = LuceneParser.parse("username:adm*n") as LuceneTerm
        assertEquals("adm*n", node.value)
    }

    @Test
    fun `an unterminated string fails to parse`() {
        assertNull(LuceneParser.parse("title:\"unterminated"))
    }

    @Test
    fun `a range without TO fails to parse`() {
        assertNull(LuceneParser.parse("age:[18 65]"))
    }

    @Test
    fun `trailing garbage after a valid query fails to parse`() {
        assertNull(LuceneParser.parse("username:admin )"))
    }
}
