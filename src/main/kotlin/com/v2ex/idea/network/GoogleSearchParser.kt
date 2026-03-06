package com.v2ex.idea.network

import com.v2ex.idea.model.TopicSummary
import org.jsoup.Jsoup
import java.net.URI
import java.net.URLDecoder.decode
import java.nio.charset.StandardCharsets

class GoogleSearchParser {
    fun parseTopics(html: String): List<TopicSummary> {
        val document = Jsoup.parse(html, "https://www.google.com")
        val seen = mutableSetOf<Long>()
        val topics = mutableListOf<TopicSummary>()

        document.select("div.g a[href], a[href]").forEach { anchor ->
            val titleNode = anchor.selectFirst("h3")
            if (titleNode == null && anchor.parents().none { it.selectFirst("h3") != null }) return@forEach

            val rawHref = anchor.attr("href").trim()
            val topicUrl = extractTopicUrl(rawHref) ?: return@forEach
            val id = TOPIC_ID_REGEX.find(topicUrl)?.groupValues?.get(1)?.toLongOrNull() ?: return@forEach
            if (!seen.add(id)) return@forEach

            val title = titleNode?.text()?.trim()
                ?.ifBlank { null }
                ?: anchor.text().trim().ifBlank { "V2EX 主题 #$id" }

            topics += TopicSummary(
                id = id,
                title = title,
                author = "未知",
                node = "",
                repliesCount = 0,
                createdAt = null,
                lastTouchedAt = null,
                url = topicUrl,
            )
        }

        return topics
    }

    private fun extractTopicUrl(rawHref: String): String? {
        val normalized = when {
            rawHref.startsWith("/url?") -> extractGoogleRedirectTarget(rawHref)
            rawHref.startsWith("http://") || rawHref.startsWith("https://") -> rawHref
            else -> null
        } ?: return null

        val cleaned = normalized.substringBefore('#').trim()
        return if (TOPIC_ID_REGEX.matches(cleaned)) cleaned else null
    }

    private fun extractGoogleRedirectTarget(path: String): String? {
        val uri = URI.create("https://www.google.com$path")
        val query = uri.rawQuery ?: return null
        val target = query.split('&')
            .firstOrNull { it.startsWith("q=") || it.startsWith("url=") }
            ?.substringAfter('=')
            ?: return null
        return decode(target, StandardCharsets.UTF_8)
    }

    private companion object {
        val TOPIC_ID_REGEX = Regex("https?://(?:www\\.)?v2ex\\.com/t/(\\d+)(?:\\?.*)?")
    }
}
