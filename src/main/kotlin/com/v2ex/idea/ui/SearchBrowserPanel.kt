package com.v2ex.idea.ui

import com.intellij.openapi.application.ApplicationManager
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.jcef.JBCefApp
import com.intellij.ui.jcef.JBCefBrowser
import com.intellij.util.ui.JBUI
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefRequestHandlerAdapter
import org.cef.network.CefRequest
import java.awt.BorderLayout
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.awt.FlowLayout
import javax.swing.JButton
import javax.swing.JPanel

class SearchBrowserPanel : javax.swing.JPanel(BorderLayout(0, 6)) {
    private val statusLabel = JBLabel("在内置浏览器中显示 Google 搜索结果")
    private val backButton = JButton("后退")
    private val forwardButton = JButton("前进")
    private val refreshButton = JButton("刷新")
    private val homeButton = JButton("返回首页")

    private var homeClickCallback: (() -> Unit)? = null
    private var topicClickCallback: ((Long, String) -> Unit)? = null
    private var browser: JBCefBrowser? = null

    val isSupported: Boolean
        get() = browser != null

    init {
        border = JBUI.Borders.empty(6, 8)
        statusLabel.border = JBUI.Borders.empty(0, 4)
        statusLabel.foreground = JBColor.GRAY

        val actions = JPanel(FlowLayout(FlowLayout.LEFT, 6, 0)).apply {
            isOpaque = false
            add(homeButton)
            add(backButton)
            add(forwardButton)
            add(refreshButton)
        }

        val topBar = JPanel(BorderLayout(8, 0)).apply {
            border = JBUI.Borders.empty(0, 2, 4, 2)
            add(actions, BorderLayout.WEST)
            add(statusLabel, BorderLayout.CENTER)
        }
        add(topBar, BorderLayout.NORTH)

        homeButton.addActionListener { homeClickCallback?.invoke() }
        backButton.addActionListener { browser?.cefBrowser?.goBack() }
        forwardButton.addActionListener { browser?.cefBrowser?.goForward() }
        refreshButton.addActionListener { browser?.cefBrowser?.reloadIgnoreCache() }

        if (JBCefApp.isSupported()) {
            browser = JBCefBrowser("about:blank")
            installTopicIntercept(browser!!)
            add(browser!!.component, BorderLayout.CENTER)
        } else {
            statusLabel.text = "当前 IDE 不支持内置浏览器（JCEF），已回退为列表搜索模式"
            backButton.isEnabled = false
            forwardButton.isEnabled = false
            refreshButton.isEnabled = false
        }
    }

    fun onHomeRequested(callback: () -> Unit) {
        homeClickCallback = callback
    }

    fun onTopicLinkClicked(callback: (Long, String) -> Unit) {
        topicClickCallback = callback
    }

    fun loadSearch(keyword: String, forceRefresh: Boolean) {
        val query = java.net.URLEncoder.encode("site:v2ex.com/t $keyword", StandardCharsets.UTF_8)
        val nonce = if (forceRefresh) "&_ts=${System.currentTimeMillis()}" else ""
        val url = "https://www.google.com/search?hl=zh-CN&num=20&q=$query$nonce"
        browser?.loadURL(url)
        statusLabel.text = "Google 搜索：$keyword"
    }

    fun disposeBrowser() {
        browser?.dispose()
        browser = null
    }

    private fun installTopicIntercept(jbBrowser: JBCefBrowser) {
        jbBrowser.jbCefClient.addRequestHandler(object : CefRequestHandlerAdapter() {
            override fun onBeforeBrowse(
                browser: CefBrowser?,
                frame: CefFrame?,
                request: CefRequest?,
                userGesture: Boolean,
                isRedirect: Boolean,
            ): Boolean {
                val url = request?.url ?: return false
                val topic = extractTopic(url) ?: return false
                ApplicationManager.getApplication().invokeLater {
                    topicClickCallback?.invoke(topic.first, topic.second)
                }
                return true
            }
        }, jbBrowser.cefBrowser)
    }

    private fun extractTopic(url: String): Pair<Long, String>? {
        val normalized = decodeGoogleRedirect(url) ?: url
        val match = TOPIC_URL_REGEX.find(normalized.substringBefore('#')) ?: return null
        val id = match.groupValues.getOrNull(1)?.toLongOrNull() ?: return null
        return id to "https://www.v2ex.com/t/$id"
    }

    private fun decodeGoogleRedirect(url: String): String? {
        val uri = runCatching { URI.create(url) }.getOrNull() ?: return null
        val host = uri.host.orEmpty()
        if (!host.contains("google.")) return null
        if (uri.path != "/url") return null

        val query = uri.rawQuery.orEmpty()
        val target = query.split('&')
            .firstOrNull { it.startsWith("q=") || it.startsWith("url=") }
            ?.substringAfter('=')
            ?: return null

        return URLDecoder.decode(target, StandardCharsets.UTF_8)
    }

    private companion object {
        val TOPIC_URL_REGEX = Regex("https?://(?:www\\.)?v2ex\\.com/t/(\\d+)(?:[/?#].*)?")
    }
}
