package com.v2ex.idea.model

data class TopicSummary(
    val id: Long,
    val title: String,
    val author: String,
    val node: String,
    val repliesCount: Int,
    val createdAt: Long?,
    val lastTouchedAt: Long?,
    val url: String,
)

data class TopicDetail(
    val id: Long,
    val title: String,
    val contentHtml: String,
    val contentText: String,
    val author: String,
    val node: String,
    val createdAt: Long?,
    val url: String,
)

data class ReplyItem(
    val id: Long,
    val author: String,
    val contentHtml: String,
    val contentText: String,
    val createdAt: Long?,
    val floor: Int,
)
