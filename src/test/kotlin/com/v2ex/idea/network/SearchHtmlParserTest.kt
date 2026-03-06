package com.v2ex.idea.network

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SearchHtmlParserTest {
    private val parser = SearchHtmlParser("https://www.v2ex.com")

    @Test
    fun `parse normal search result`() {
        val html = """
            <html><body>
              <div class="cell item">
                <span class="item_title"><a href="/t/123456">Kotlin in IDEA</a></span>
                <strong><a href="/member/alice">alice</a></strong>
                <a class="node">程序员</a>
                <a class="count_livid">12</a>
              </div>
            </body></html>
        """.trimIndent()

        val result = parser.parseTopics(html)

        assertEquals(1, result.size)
        assertEquals(123456L, result.first().id)
        assertEquals("Kotlin in IDEA", result.first().title)
        assertEquals("alice", result.first().author)
        assertEquals("程序员", result.first().node)
        assertEquals(12, result.first().repliesCount)
        assertEquals("https://www.v2ex.com/t/123456", result.first().url)
    }

    @Test
    fun `fallback parse when structure changed`() {
        val html = """
            <html><body>
              <a href="/t/1001">topic-1</a>
              <a href="/t/1002">topic-2</a>
            </body></html>
        """.trimIndent()

        val result = parser.parseTopics(html)

        assertEquals(2, result.size)
        assertTrue(result.any { it.id == 1001L })
        assertTrue(result.any { it.id == 1002L })
    }
}
