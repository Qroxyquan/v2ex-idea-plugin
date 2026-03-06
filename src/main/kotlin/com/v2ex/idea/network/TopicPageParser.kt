package com.v2ex.idea.network

import com.v2ex.idea.model.ReplyItem
import com.v2ex.idea.util.renderContentWithImages
import org.jsoup.Jsoup

class TopicPageParser(private val baseUrl: String = "https://www.v2ex.com") {
    fun parseOnce(html: String): String? {
        val document = Jsoup.parse(html, baseUrl)
        return document.selectFirst("input[name=once]")?.attr("value")?.trim()?.ifBlank { null }
    }

    fun parseReplies(html: String): List<ReplyItem> {
        val document = Jsoup.parse(html, baseUrl)
        val rows = document.select("#Main .cell[id^=r_], #Main div[id^=r_].cell")
        return rows.mapIndexedNotNull { idx, row ->
            val id = row.id().removePrefix("r_").toLongOrNull() ?: (idx + 1).toLong()
            val author = row.selectFirst("strong a[href^=/member/]")?.text()?.trim().orEmpty().ifBlank { "未知" }
            val contentRendered = row.selectFirst("div.reply_content")?.html().orEmpty()
            val contentText = row.selectFirst("div.reply_content")?.text().orEmpty()
            val created = row.selectFirst("span.ago")?.attr("data-ts")?.toLongOrNull()

            ReplyItem(
                id = id,
                author = author,
                contentHtml = renderContentWithImages(contentRendered, contentText),
                contentText = contentText,
                createdAt = created,
                floor = idx + 1,
            )
        }
    }
}
