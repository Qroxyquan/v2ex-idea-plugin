package com.v2ex.idea.repository

import com.v2ex.idea.network.SearchHtmlParser
import com.v2ex.idea.network.V2exApiClient
import com.v2ex.idea.util.InMemoryCache
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class V2exRepositoryImplTest {
    private lateinit var server: MockWebServer
    private lateinit var repository: V2exRepositoryImpl

    @BeforeTest
    fun setup() {
        server = MockWebServer()
        server.start()

        val base = server.url("/").toString().removeSuffix("/")
        repository = V2exRepositoryImpl(
            apiClient = V2exApiClient(baseUrl = base),
            searchHtmlParser = SearchHtmlParser(baseUrl = base),
            cache = InMemoryCache(maxSize = 64),
            json = Json { ignoreUnknownKeys = true },
        )
    }

    @AfterTest
    fun teardown() {
        server.shutdown()
    }

    @Test
    fun `latest maps fields correctly`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                [
                  {
                    "id": 42,
                    "title": "Hello V2EX",
                    "url": "https://www.v2ex.com/t/42",
                    "replies": 3,
                    "created": 1710000000,
                    "last_touched": 1710001111,
                    "member": {"username": "bob"},
                    "node": {"title": "分享发现"}
                  }
                ]
                """.trimIndent(),
            ),
        )

        val topics = repository.latest()

        assertEquals(1, topics.size)
        val first = topics.first()
        assertEquals(42L, first.id)
        assertEquals("Hello V2EX", first.title)
        assertEquals("bob", first.author)
        assertEquals("分享发现", first.node)
        assertEquals(3, first.repliesCount)
    }

    @Test
    fun `latest uses cache and respects force refresh`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody("[{\"id\":1,\"title\":\"A\",\"url\":\"https://www.v2ex.com/t/1\"}]"))
        server.enqueue(MockResponse().setResponseCode(200).setBody("[{\"id\":2,\"title\":\"B\",\"url\":\"https://www.v2ex.com/t/2\"}]"))

        val first = repository.latest()
        val second = repository.latest()
        val forced = repository.latest(forceRefresh = true)

        assertEquals(1L, first.first().id)
        assertEquals(1L, second.first().id)
        assertEquals(2L, forced.first().id)
        assertEquals(2, server.requestCount)
    }

    @Test
    fun `detail and replies map correctly`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                [
                  {
                    "id": 9,
                    "title": "T9",
                    "url": "https://www.v2ex.com/t/9",
                    "created": 1710000000,
                    "content": "body",
                    "member": {"username": "tom"},
                    "node": {"title": "问与答"}
                  }
                ]
                """.trimIndent(),
            ),
        )
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                [
                  {
                    "id": 100,
                    "content": "r1",
                    "created": 1710002222,
                    "member": {"username": "jerry"}
                  }
                ]
                """.trimIndent(),
            ),
        )

        val detail = repository.topicDetail(9)
        val replies = repository.replies(9)

        assertEquals(9L, detail.id)
        assertEquals("tom", detail.author)
        assertEquals(1, replies.size)
        assertEquals(1, replies.first().floor)
        assertEquals("jerry", replies.first().author)
    }
}
