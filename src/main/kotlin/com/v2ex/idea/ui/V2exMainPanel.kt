package com.v2ex.idea.ui

import com.intellij.icons.AllIcons
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.EDT
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import com.v2ex.idea.model.TopicSummary
import com.v2ex.idea.network.V2exApiClient
import com.v2ex.idea.repository.V2exRepository
import com.v2ex.idea.repository.V2exRepositoryImpl
import com.v2ex.idea.settings.V2exSettingsStateService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.awt.BorderLayout
import java.awt.CardLayout
import java.awt.Dimension
import java.awt.FlowLayout
import javax.swing.ButtonGroup
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.JToggleButton

class V2exMainPanel(
    @Suppress("UNUSED_PARAMETER") project: Project,
) : JPanel(BorderLayout()), Disposable, V2exPanelController {

    private val settingsService = V2exSettingsStateService.getInstance()
    private val repository: V2exRepository = V2exRepositoryImpl(
        apiClient = V2exApiClient(
            tokenProvider = { settingsService.getApiToken() },
            a2TokenProvider = { settingsService.getA2Token() },
        ),
    )

    private val scope = CoroutineScope(Dispatchers.EDT + SupervisorJob())

    private val cardLayout = CardLayout()
    private val contentCard = JPanel(cardLayout)

    private val homePanel = TopicListPanel("暂无帖子")
    private val detailPanel = TopicDetailPanel()
    private val searchBrowserPanel = SearchBrowserPanel()

    private val searchField = JBTextField()
    private val statusBar = JBLabel("就绪")

    private val primaryNavBar = JPanel(FlowLayout(FlowLayout.LEFT, 8, 4))
    private val primaryButtons = linkedMapOf<String, JToggleButton>()
    private val primaryGroup = ButtonGroup()

    private var selectedTopic: TopicSummary? = null
    private var currentMode = FeedMode.TAB
    private var currentTabKey = "all"
    private var currentSearchKeyword = ""

    private var tabJob: Job? = null
    private var searchJob: Job? = null
    private var detailJob: Job? = null

    private val lastRequestAt = mutableMapOf<String, Long>()

    init {
        layoutUi()
        bindEvents()
        switchToTab("all", forceRefresh = false)
    }

    override fun dispose() {
        searchBrowserPanel.disposeBrowser()
        scope.cancel()
    }

    override fun refreshCurrent() {
        when (currentMode) {
            FeedMode.TAB -> {
                repository.invalidate("tab:$currentTabKey")
                loadTab(currentTabKey, forceRefresh = true)
            }

            FeedMode.SEARCH -> {
                if (currentSearchKeyword.isBlank()) return
                if (searchBrowserPanel.isSupported) {
                    loadSearchInBrowser(currentSearchKeyword, forceRefresh = true)
                } else {
                    repository.invalidate("search:$currentSearchKeyword")
                    performSearchParse(currentSearchKeyword, forceRefresh = true)
                }
            }
        }
    }

    override fun openSelectedInBrowser() {
        selectedTopic?.url?.takeIf { it.isNotBlank() }?.let { BrowserUtil.browse(it) }
    }

    override fun focusSearch() {
        cardLayout.show(contentCard, CARD_HOME)
        searchField.requestFocusInWindow()
    }

    override fun hasSelectedTopic(): Boolean = selectedTopic != null

    private fun layoutUi() {
        background = JBColor(0xF3F4F6, 0x2B2D30)

        val shell = JPanel(BorderLayout(0, 8)).apply {
            border = JBUI.Borders.empty(8)
            add(buildHeader(), BorderLayout.NORTH)
            add(buildHomeView(), BorderLayout.CENTER)
        }

        contentCard.add(shell, CARD_HOME)
        contentCard.add(searchBrowserPanel, CARD_SEARCH)
        contentCard.add(detailPanel, CARD_DETAIL)

        add(contentCard, BorderLayout.CENTER)

        statusBar.border = JBUI.Borders.empty(4, 10)
        statusBar.foreground = JBColor.GRAY
        add(statusBar, BorderLayout.SOUTH)

        detailPanel.showPlaceholder()
    }

    private fun buildHeader(): JPanel {
        val container = JPanel(BorderLayout(10, 0)).apply {
            border = JBUI.Borders.empty(8, 10)
            background = JBColor(0xFFFFFF, 0x2B2D30)
        }

        val homeButton = JButton(AllIcons.Nodes.HomeFolder).apply {
            toolTipText = "返回首页"
            addActionListener {
                cardLayout.show(contentCard, CARD_HOME)
                switchToTab("all", forceRefresh = false)
            }
        }

        searchField.emptyText.text = "搜索 V2EX 帖子（Google）"
        searchField.preferredSize = Dimension(460, 32)

        val searchButton = JButton("搜索").apply {
            addActionListener { triggerSearch() }
        }
        val refreshButton = JButton("刷新").apply {
            addActionListener { refreshCurrent() }
        }

        val rightActions = JPanel(FlowLayout(FlowLayout.RIGHT, 8, 0)).apply {
            isOpaque = false
            add(searchButton)
            add(refreshButton)
        }

        container.add(homeButton, BorderLayout.WEST)
        container.add(searchField, BorderLayout.CENTER)
        container.add(rightActions, BorderLayout.EAST)
        return container
    }

    private fun buildHomeView(): JPanel {
        buildPrimaryNav()

        val topBox = JPanel(BorderLayout()).apply {
            border = JBUI.Borders.compound(
                JBUI.Borders.customLine(JBColor(0xE5E7EB, 0x3C3F41)),
                JBUI.Borders.empty(4),
            )
            background = JBColor(0xFFFFFF, 0x2B2D30)
            add(primaryNavBar, BorderLayout.CENTER)
        }

        return JPanel(BorderLayout(0, 8)).apply {
            isOpaque = false
            add(topBox, BorderLayout.NORTH)
            add(homePanel, BorderLayout.CENTER)
        }
    }

    private fun buildPrimaryNav() {
        primaryNavBar.removeAll()
        primaryButtons.clear()

        primaryNavBar.isOpaque = false

        PRIMARY_TABS.forEach { tab ->
            val button = JToggleButton(tab.label).apply {
                isFocusPainted = false
                margin = JBUI.insets(3, 10)
                addActionListener {
                    switchToTab(tab.key, forceRefresh = false)
                }
            }
            primaryButtons[tab.key] = button
            primaryGroup.add(button)
            primaryNavBar.add(button)
        }
    }

    private fun bindEvents() {
        homePanel.onTopicSelected(::openTopicDetail)

        detailPanel.onBack {
            if (currentMode == FeedMode.SEARCH && searchBrowserPanel.isSupported) {
                cardLayout.show(contentCard, CARD_SEARCH)
                statusBar.text = "已返回搜索页"
            } else {
                cardLayout.show(contentCard, CARD_HOME)
                homePanel.clearSelection()
                statusBar.text = "已返回首页"
            }
        }
        detailPanel.onRefresh {
            selectedTopic?.let { topic ->
                loadTopicDetail(topic, forceRefresh = true)
            }
        }
        detailPanel.onOpenInBrowser { openSelectedInBrowser() }
        detailPanel.onSubmitReply { content ->
            selectedTopic?.let { topic ->
                submitReply(topic.id, content)
            }
        }

        searchBrowserPanel.onTopicLinkClicked { topicId, topicUrl ->
            val topic = TopicSummary(
                id = topicId,
                title = "V2EX 主题 #$topicId",
                author = "未知",
                node = "",
                repliesCount = 0,
                createdAt = null,
                lastTouchedAt = null,
                url = topicUrl,
            )
            openTopicDetail(topic)
        }
        searchBrowserPanel.onHomeRequested {
            switchToTab("all", forceRefresh = false)
            statusBar.text = "已返回首页"
        }

        searchField.addActionListener {
            triggerSearch()
        }
    }

    private fun switchToTab(tabKey: String, forceRefresh: Boolean) {
        currentMode = FeedMode.TAB
        currentTabKey = tabKey
        currentSearchKeyword = ""
        primaryButtons[tabKey]?.isSelected = true
        cardLayout.show(contentCard, CARD_HOME)
        loadTab(tabKey, forceRefresh)
    }

    private fun triggerSearch() {
        val keyword = searchField.text.trim()
        if (keyword.isBlank()) return
        currentMode = FeedMode.SEARCH
        currentSearchKeyword = keyword
        primaryGroup.clearSelection()
        if (searchBrowserPanel.isSupported) {
            cardLayout.show(contentCard, CARD_SEARCH)
            loadSearchInBrowser(keyword, forceRefresh = true)
        } else {
            cardLayout.show(contentCard, CARD_HOME)
            performSearchParse(keyword, forceRefresh = true)
        }
    }

    private fun loadSearchInBrowser(keyword: String, forceRefresh: Boolean) {
        if (!forceRefresh && shouldDebounce("search-browser:$keyword")) return
        tabJob?.cancel()
        searchJob?.cancel()
        searchBrowserPanel.loadSearch(keyword, forceRefresh = forceRefresh)
        statusBar.text = "正在内置浏览器加载搜索：$keyword"
    }

    private fun loadTab(tabKey: String, forceRefresh: Boolean) {
        if (!forceRefresh && shouldDebounce("tab:$tabKey")) return
        val label = PRIMARY_TABS.firstOrNull { it.key == tabKey }?.label ?: tabKey
        statusBar.text = "加载标签：$label"
        homePanel.showLoading()
        searchJob?.cancel()
        tabJob?.cancel()
        val expectedTab = tabKey
        tabJob = scope.launch {
            runCatching { repository.tab(tabKey, forceRefresh = forceRefresh) }
                .onSuccess {
                    if (currentMode != FeedMode.TAB || currentTabKey != expectedTab) return@onSuccess
                    val sorted = sortByLastTouched(it)
                    homePanel.setTopics(sorted, "$label 暂无内容")
                    statusBar.text = if (sorted.isEmpty()) "$label 暂无内容" else "$label：${sorted.size} 条"
                }
                .onFailure {
                    if (currentMode != FeedMode.TAB || currentTabKey != expectedTab) return@onFailure
                    homePanel.showError(it.message ?: "加载失败")
                    statusBar.text = "标签加载失败"
                }
        }
    }

    private fun performSearchParse(keyword: String, forceRefresh: Boolean) {
        if (!forceRefresh && shouldDebounce("search:$keyword")) return
        homePanel.showLoading("正在解析 Google 搜索结果...")
        tabJob?.cancel()
        searchJob?.cancel()
        val expectedKeyword = keyword
        searchJob = scope.launch {
            runCatching { repository.search(keyword, forceRefresh = forceRefresh) }
                .onSuccess {
                    if (currentMode != FeedMode.SEARCH || currentSearchKeyword != expectedKeyword) return@onSuccess
                    val sorted = sortByLastTouched(it)
                    homePanel.setTopics(sorted, "未找到相关帖子")
                    statusBar.text = if (sorted.isEmpty()) "未找到结果" else "搜索结果：${sorted.size} 条"
                }
                .onFailure {
                    if (currentMode != FeedMode.SEARCH || currentSearchKeyword != expectedKeyword) return@onFailure
                    homePanel.showError(it.message ?: "搜索失败")
                    statusBar.text = "搜索失败"
                }
        }
    }

    private fun openTopicDetail(topic: TopicSummary) {
        loadTopicDetail(topic, forceRefresh = false)
    }

    private fun loadTopicDetail(topic: TopicSummary, forceRefresh: Boolean) {
        selectedTopic = topic
        detailPanel.setReplyEnabled(!settingsService.getA2Token().isNullOrBlank())
        detailPanel.showLoading()
        cardLayout.show(contentCard, CARD_DETAIL)
        statusBar.text = "加载帖子 #${topic.id}"

        detailJob?.cancel()
        detailJob = scope.launch {
            runCatching {
                val detail = repository.topicDetail(topic.id, forceRefresh = forceRefresh)
                val replies = repository.replies(topic.id, forceRefresh = forceRefresh)
                detail to replies
            }.onSuccess { (detail, replies) ->
                detailPanel.render(detail, replies)
                statusBar.text = "帖子加载完成：${topic.title}"
            }.onFailure {
                detailPanel.showError(it.message ?: "详情加载失败")
                statusBar.text = "加载帖子失败"
            }
        }
    }

    private fun submitReply(topicId: Long, content: String) {
        if (content.isBlank()) return
        statusBar.text = "正在提交评论..."
        detailJob?.cancel()
        detailJob = scope.launch {
            runCatching {
                repository.postReply(topicId, content)
                val detail = repository.topicDetail(topicId, forceRefresh = true)
                val replies = repository.replies(topicId, forceRefresh = true)
                detail to replies
            }.onSuccess { (detail, replies) ->
                detailPanel.clearReplyInput()
                detailPanel.render(detail, replies)
                statusBar.text = "评论已提交"
            }.onFailure {
                detailPanel.showError(it.message ?: "评论提交失败")
                statusBar.text = "评论提交失败"
            }
        }
    }

    private fun sortByLastTouched(topics: List<TopicSummary>): List<TopicSummary> {
        return topics.sortedWith(
            compareByDescending<TopicSummary> { it.lastTouchedAt ?: it.createdAt ?: 0L }
                .thenByDescending { it.id },
        )
    }

    private fun shouldDebounce(key: String): Boolean {
        val now = System.currentTimeMillis()
        val last = lastRequestAt[key] ?: 0L
        if ((now - last) < 800) return true
        lastRequestAt[key] = now
        return false
    }

    private data class PrimaryTab(val label: String, val key: String)

    private enum class FeedMode {
        TAB,
        SEARCH,
    }

    private companion object {
        const val CARD_HOME = "home"
        const val CARD_DETAIL = "detail"
        const val CARD_SEARCH = "search"

        val PRIMARY_TABS = listOf(
            PrimaryTab("全部", "all"),
            PrimaryTab("最热", "hot"),
            PrimaryTab("技术", "tech"),
            PrimaryTab("创意", "creative"),
            PrimaryTab("好玩", "play"),
            PrimaryTab("Apple", "apple"),
            PrimaryTab("酷工作", "jobs"),
        )
    }
}
