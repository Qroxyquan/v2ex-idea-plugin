package com.v2ex.idea.repository

import com.v2ex.idea.model.ReplyItem
import com.v2ex.idea.model.TopicDetail
import com.v2ex.idea.model.TopicSummary

interface V2exRepository {
    suspend fun tab(tab: String, forceRefresh: Boolean = false): List<TopicSummary>
    suspend fun latest(forceRefresh: Boolean = false): List<TopicSummary>
    suspend fun hot(forceRefresh: Boolean = false): List<TopicSummary>
    suspend fun search(keyword: String, forceRefresh: Boolean = false): List<TopicSummary>
    suspend fun topicDetail(id: Long, forceRefresh: Boolean = false): TopicDetail
    suspend fun replies(topicId: Long, forceRefresh: Boolean = false): List<ReplyItem>
    suspend fun postReply(topicId: Long, content: String)
    fun invalidate(prefix: String)
}
