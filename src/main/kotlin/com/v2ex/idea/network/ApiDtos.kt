package com.v2ex.idea.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TopicApiDto(
    val id: Long,
    val title: String = "",
    val url: String = "",
    val replies: Int = 0,
    val created: Long? = null,
    @SerialName("last_touched") val lastTouched: Long? = null,
    val content: String? = null,
    @SerialName("content_rendered") val contentRendered: String? = null,
    val member: MemberDto? = null,
    val node: NodeDto? = null,
)

@Serializable
data class ReplyApiDto(
    val id: Long,
    val content: String? = null,
    @SerialName("content_rendered") val contentRendered: String? = null,
    val created: Long? = null,
    val member: MemberDto? = null,
)

@Serializable
data class MemberDto(
    val username: String = "",
)

@Serializable
data class NodeDto(
    val title: String = "",
)
