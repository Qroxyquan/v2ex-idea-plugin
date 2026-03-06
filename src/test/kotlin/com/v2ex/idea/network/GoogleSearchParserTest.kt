package com.v2ex.idea.network

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GoogleSearchParserTest {
    private val parser = GoogleSearchParser()

    @Test
    fun `parse google redirect links to v2ex topics`() {
        val html = """
            <html><body>
              <a href="/url?q=https%3A%2F%2Fwww.v2ex.com%2Ft%2F12345%23reply1&sa=U&ved=0"><h3>帖子 A</h3></a>
              <a href="https://www.v2ex.com/t/98765"><h3>帖子 B</h3></a>
            </body></html>
        """.trimIndent()

        val result = parser.parseTopics(html)

        assertEquals(2, result.size)
        assertTrue(result.any { it.id == 12345L && it.title == "帖子 A" })
        assertTrue(result.any { it.id == 98765L && it.title == "帖子 B" })
    }
}
