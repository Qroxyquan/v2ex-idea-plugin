package com.v2ex.idea.network

import com.v2ex.idea.model.TopicSummary
import org.jsoup.Jsoup

class SearchHtmlParser(private val baseUrl: String = "https://www.v2ex.com") {
    fun parseTopics(html: String, allowFallback: Boolean = true, mainOnly: Boolean = false): List<TopicSummary> {
        val document = Jsoup.parse(html, baseUrl)
        val rows = if (mainOnly) {
            document.select("#Main .box .cell.item, #Main .box .cell:has(span.item_title a[href^=/t/])")
        } else {
            document.select("div.cell.item, div.cell")
        }
        val seen = mutableSetOf<Long>()
        val topics = mutableListOf<TopicSummary>()

        rows.forEach { row ->
            val link = row.selectFirst("span.item_title a[href], .item_title a[href], a[href^=/t/]") ?: return@forEach
            val href = link.attr("href")
            val id = TOPIC_ID_REGEX.find(href)?.groupValues?.get(1)?.toLongOrNull() ?: return@forEach
            if (!seen.add(id)) return@forEach

            val url = if (href.startsWith("http")) href else "$baseUrl$href"
            val title = link.text().trim().ifBlank { "(无标题)" }
            val author = row.selectFirst("strong a[href^=/member/]")?.text()?.trim().orEmpty()
            val node = row.selectFirst("a.node")?.text()?.trim().orEmpty()
            val replies = row.selectFirst("a.count_livid, a.count_orange")
                ?.text()
                ?.trim()
                ?.toIntOrNull()
                ?: 0

            topics += TopicSummary(
                id = id,
                title = title,
                author = author.ifBlank { "未知" },
                node = node,
                repliesCount = replies,
                createdAt = null,
                lastTouchedAt = null,
                url = url,
            )
        }

        if (topics.isNotEmpty() || !allowFallback) return topics

        val fallback = mutableListOf<TopicSummary>()
        val fallbackLinks = if (mainOnly) {
            document.select("#Main a[href^=/t/]")
        } else {
            document.select("a[href^=/t/]")
        }
        fallbackLinks.forEach { link ->
            val href = link.attr("href")
            val id = TOPIC_ID_REGEX.find(href)?.groupValues?.get(1)?.toLongOrNull() ?: return@forEach
            if (!seen.add(id)) return@forEach
            val url = if (href.startsWith("http")) href else "$baseUrl$href"
            fallback += TopicSummary(
                id = id,
                title = link.text().trim().ifBlank { "(无标题)" },
                author = "未知",
                node = "",
                repliesCount = 0,
                createdAt = null,
                lastTouchedAt = null,
                url = url,
            )
        }
        return fallback
    }

    private companion object {
        val TOPIC_ID_REGEX = Regex("/t/(\\d+)")
    }
}
