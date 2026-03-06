package com.v2ex.idea.repository

import com.v2ex.idea.model.ReplyItem
import com.v2ex.idea.model.TopicDetail
import com.v2ex.idea.model.TopicSummary
import com.v2ex.idea.network.ReplyApiDto
import com.v2ex.idea.network.SearchHtmlParser
import com.v2ex.idea.network.GoogleSearchParser
import com.v2ex.idea.network.TopicPageParser
import com.v2ex.idea.network.TopicApiDto
import com.v2ex.idea.network.V2exApiClient
import com.v2ex.idea.util.InMemoryCache
import com.v2ex.idea.util.renderContentWithImages
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class V2exRepositoryImpl(
    private val apiClient: V2exApiClient = V2exApiClient(),
    private val searchHtmlParser: SearchHtmlParser = SearchHtmlParser(),
    private val googleSearchParser: GoogleSearchParser = GoogleSearchParser(),
    private val topicPageParser: TopicPageParser = TopicPageParser(),
    private val cache: InMemoryCache<String, Any> = InMemoryCache(maxSize = 256),
    private val json: Json = Json { ignoreUnknownKeys = true },
) : V2exRepository {

    override suspend fun tab(tab: String, forceRefresh: Boolean): List<TopicSummary> {
        val normalized = tab.trim().lowercase()
        require(normalized.isNotEmpty()) { "标签不能为空" }
        if (normalized == "hot") {
            return hot(forceRefresh = forceRefresh)
        }
        return cached("tab:$normalized", TTL_LIST_MS, forceRefresh) {
            val encoded = URLEncoder.encode(normalized, StandardCharsets.UTF_8)
            val body = apiClient.get("/?tab=$encoded", acceptJson = false)
            val parsed = searchHtmlParser.parseTopics(body, allowFallback = false, mainOnly = true)
            if (parsed.isNotEmpty()) {
                enrichSummaries(parsed, forceRefresh = forceRefresh)
            } else {
                when (normalized) {
                    "all" -> latest(forceRefresh = false)
                    "hot" -> hot(forceRefresh = false)
                    else -> emptyList()
                }
            }
        }
    }

    override suspend fun latest(forceRefresh: Boolean): List<TopicSummary> =
        cached("latest", TTL_LIST_MS, forceRefresh) {
            val body = apiClient.get("/api/topics/latest.json")
            val topics = json.decodeFromString<List<TopicApiDto>>(body)
            topics.map { it.toTopicSummary() }
        }

    override suspend fun hot(forceRefresh: Boolean): List<TopicSummary> =
        cached("hot", TTL_LIST_MS, forceRefresh) {
            val body = apiClient.get("/api/topics/hot.json")
            val topics = json.decodeFromString<List<TopicApiDto>>(body)
            topics.map { it.toTopicSummary() }
        }

    override suspend fun search(keyword: String, forceRefresh: Boolean): List<TopicSummary> {
        val normalized = keyword.trim()
        require(normalized.isNotEmpty()) { "搜索关键词不能为空" }

        return cached("search:$normalized", TTL_SEARCH_MS, forceRefresh) {
            val googleQuery = URLEncoder.encode("site:v2ex.com/t $normalized", StandardCharsets.UTF_8)
            val googleResults = runCatching {
                val googleBody = apiClient.get(
                    "https://www.google.com/search?hl=zh-CN&num=20&q=$googleQuery",
                    acceptJson = false,
                )
                googleSearchParser.parseTopics(googleBody)
            }.getOrDefault(emptyList())

            val fromGoogleCandidates = filterByKeyword(googleResults, normalized)
            val fromGoogle = filterByKeyword(
                enrichSummaries(fromGoogleCandidates, forceRefresh = forceRefresh),
                normalized,
            )
            if (fromGoogle.isNotEmpty()) return@cached fromGoogle

            val encoded = URLEncoder.encode(normalized, StandardCharsets.UTF_8)
            val body = apiClient.get("/search?q=$encoded", acceptJson = false)
            val fallbackCandidates = filterByKeyword(
                searchHtmlParser.parseTopics(body, allowFallback = false, mainOnly = true),
                normalized,
            )
            filterByKeyword(
                enrichSummaries(fallbackCandidates, forceRefresh = forceRefresh),
                normalized,
            )
        }
    }

    override suspend fun topicDetail(id: Long, forceRefresh: Boolean): TopicDetail =
        cached("detail:$id", TTL_DETAIL_MS, forceRefresh) {
            val body = apiClient.get("/api/topics/show.json?id=$id")
            val dto = json.decodeFromString<List<TopicApiDto>>(body).firstOrNull()
                ?: throw IllegalStateException("帖子不存在或已被删除")
            dto.toTopicDetail()
        }

    override suspend fun replies(topicId: Long, forceRefresh: Boolean): List<ReplyItem> =
        cached("replies:$topicId", TTL_DETAIL_MS, forceRefresh) {
            val body = apiClient.get("/api/replies/show.json?topic_id=$topicId")
            val replies = json.decodeFromString<List<ReplyApiDto>>(body)
            val mapped = replies.mapIndexed { idx, dto -> dto.toReplyItem(floor = idx + 1) }
            if (mapped.isNotEmpty()) {
                mapped
            } else {
                val topicHtml = apiClient.get("/t/$topicId", acceptJson = false)
                topicPageParser.parseReplies(topicHtml)
            }
        }

    override suspend fun postReply(topicId: Long, content: String) {
        val normalized = content.trim()
        require(normalized.isNotEmpty()) { "评论内容不能为空" }

        val topicPath = "/t/$topicId"
        val topicHtml = apiClient.get(topicPath, acceptJson = false)
        val once = topicPageParser.parseOnce(topicHtml)
            ?: throw IllegalStateException("未获取到回复校验参数（once），请确认已登录且有回复权限")

        val response = apiClient.postForm(
            path = topicPath,
            form = mapOf(
                "once" to once,
                "content" to normalized,
            ),
            refererPath = topicPath,
        )

        if (response.code >= 400) {
            throw IllegalStateException("评论提交失败：HTTP ${response.code}")
        }
        if (response.body.contains("你要查看的页面需要先登录") || response.body.contains("请先登录")) {
            throw IllegalStateException("当前未登录，请在设置中填写 A2 Token")
        }
        if (response.body.contains("你提交的太快了") || response.body.contains("请不要太快")) {
            throw IllegalStateException("提交过快，请稍后再试")
        }

        cache.invalidateByPrefix("replies:$topicId")
        cache.invalidateByPrefix("detail:$topicId")
    }

    override fun invalidate(prefix: String) {
        cache.invalidateByPrefix(prefix)
    }

    private suspend fun enrichSummaries(topics: List<TopicSummary>, forceRefresh: Boolean): List<TopicSummary> = coroutineScope {
        val targetIds = topics.map { it.id }
            .filter { it > 0L }
            .distinct()
            .take(MAX_ENRICH_TOPICS)

        val idToSummary = targetIds
            .map { id ->
                async {
                    id to runCatching {
                        loadTopicSummary(id = id, forceRefresh = forceRefresh)
                    }.getOrNull()
                }
            }
            .awaitAll()
            .toMap()

        topics.map { topic ->
            val enriched = idToSummary[topic.id] ?: return@map topic
            topic.copy(
                title = if (topic.title.isBlank()) enriched.title else topic.title,
                author = if (topic.author == "未知" || topic.author.isBlank()) enriched.author else topic.author,
                node = if (topic.node.isBlank()) enriched.node else topic.node,
                repliesCount = if (enriched.repliesCount > 0) enriched.repliesCount else topic.repliesCount,
                createdAt = enriched.createdAt ?: topic.createdAt,
                lastTouchedAt = enriched.lastTouchedAt ?: topic.lastTouchedAt ?: topic.createdAt,
                url = if (topic.url.contains("/t/${topic.id}")) topic.url else enriched.url,
            )
        }
    }

    private suspend fun loadTopicSummary(id: Long, forceRefresh: Boolean): TopicSummary =
        cached("topic-summary:$id", TTL_DETAIL_MS, forceRefresh) {
            val body = apiClient.get("/api/topics/show.json?id=$id")
            val dto = json.decodeFromString<List<TopicApiDto>>(body).firstOrNull()
                ?: throw IllegalStateException("帖子不存在或已被删除")

            val lastReplyAt = if (dto.replies > 0) {
                runCatching {
                    val repliesBody = apiClient.get("/api/replies/show.json?topic_id=$id")
                    val replies = json.decodeFromString<List<ReplyApiDto>>(repliesBody)
                    replies.maxOfOrNull { it.created ?: 0L }?.takeIf { it > 0L }
                }.getOrNull()
            } else {
                null
            }

            dto.toTopicSummary().copy(
                lastTouchedAt = lastReplyAt ?: dto.lastTouched ?: dto.created,
            )
        }

    private suspend fun <T : Any> cached(
        key: String,
        ttlMs: Long,
        forceRefresh: Boolean,
        fetcher: suspend () -> T,
    ): T = withContext(Dispatchers.IO) {
        if (!forceRefresh) {
            @Suppress("UNCHECKED_CAST")
            cache.get(key)?.let { return@withContext it as T }
        }

        val fresh = fetcher()
        cache.put(key, fresh, ttlMs)
        fresh
    }

    private fun TopicApiDto.toTopicSummary(): TopicSummary = TopicSummary(
        id = id,
        title = title,
        author = member?.username.orEmpty().ifBlank { "未知" },
        node = node?.title.orEmpty(),
        repliesCount = replies,
        createdAt = created,
        lastTouchedAt = lastTouched,
        url = url.ifBlank { "https://www.v2ex.com/t/$id" },
    )

    private fun TopicApiDto.toTopicDetail(): TopicDetail {
        val html = renderContentWithImages(contentRendered, content)
        val text = content.orEmpty().ifBlank { "(无正文)" }

        return TopicDetail(
            id = id,
            title = title,
            contentHtml = html,
            contentText = text,
            author = member?.username.orEmpty().ifBlank { "未知" },
            node = node?.title.orEmpty(),
            createdAt = created,
            url = url.ifBlank { "https://www.v2ex.com/t/$id" },
        )
    }

    private fun ReplyApiDto.toReplyItem(floor: Int): ReplyItem {
        val html = renderContentWithImages(contentRendered, content)

        return ReplyItem(
            id = id,
            author = member?.username.orEmpty().ifBlank { "未知" },
            contentHtml = html,
            contentText = content.orEmpty(),
            createdAt = created,
            floor = floor,
        )
    }

    private fun filterByKeyword(topics: List<TopicSummary>, keyword: String): List<TopicSummary> {
        val normalized = keyword.lowercase()
        val terms = normalized.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (terms.isEmpty()) return topics
        return topics.filter { topic ->
            val title = topic.title.lowercase()
            val node = topic.node.lowercase()
            terms.all { term -> title.contains(term) || node.contains(term) }
        }
    }

    private companion object {
        const val TTL_LIST_MS = 60_000L
        const val TTL_DETAIL_MS = 120_000L
        const val TTL_SEARCH_MS = 30_000L
        const val MAX_ENRICH_TOPICS = 20
    }
}
