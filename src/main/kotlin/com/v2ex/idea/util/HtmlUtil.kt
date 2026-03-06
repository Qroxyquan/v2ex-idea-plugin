package com.v2ex.idea.util

import org.jsoup.Jsoup
import org.jsoup.safety.Safelist

fun sanitizeHtml(rawHtml: String?): String {
    if (rawHtml.isNullOrBlank()) return ""
    val safeList = Safelist.basicWithImages()
        .addTags("pre", "code", "blockquote")
        .addAttributes("a", "target", "rel")
    return Jsoup.clean(rawHtml, safeList)
}

fun escapeHtml(raw: String): String = buildString(raw.length + 16) {
    raw.forEach { ch ->
        when (ch) {
            '&' -> append("&amp;")
            '<' -> append("&lt;")
            '>' -> append("&gt;")
            '"' -> append("&quot;")
            '\'' -> append("&#39;")
            else -> append(ch)
        }
    }
}

fun renderContentWithImages(renderedHtml: String?, rawText: String?): String {
    if (!renderedHtml.isNullOrBlank()) {
        return sanitizeHtml(renderedHtml)
    }
    if (rawText.isNullOrBlank()) {
        return ""
    }
    return textToHtmlWithImages(rawText)
}

fun toHtmlParagraph(text: String): String = textToHtmlWithImages(text)

private fun textToHtmlWithImages(text: String): String {
    val lines = text.split('\n')
    val htmlParts = mutableListOf<String>()

    for (line in lines) {
        val trimmed = line.trim()
        when {
            trimmed.isBlank() -> htmlParts += "<br/>"
            else -> {
                val markdownImage = MARKDOWN_IMAGE_REGEX.find(trimmed)?.groupValues?.get(1)
                val directImage = IMAGE_URL_REGEX.matchEntire(trimmed)?.groupValues?.get(1)
                val imageUrl = markdownImage ?: directImage
                if (imageUrl != null) {
                    htmlParts += """<p><img src="${escapeHtml(imageUrl)}"/></p>"""
                } else {
                    htmlParts += "<p>${escapeHtml(line)}</p>"
                }
            }
        }
    }

    return htmlParts.joinToString("")
}

private val MARKDOWN_IMAGE_REGEX = Regex("""!\[[^\]]*]\((https?://[^\s)]+)\)""")
private val IMAGE_URL_REGEX = Regex("""(https?://\S+\.(?:png|jpg|jpeg|gif|webp|bmp|svg)(?:\?\S*)?)""", RegexOption.IGNORE_CASE)
