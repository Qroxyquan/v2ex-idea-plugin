package com.v2ex.idea.ui

import com.intellij.ide.BrowserUtil
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.JBUI
import com.v2ex.idea.model.ReplyItem
import com.v2ex.idea.model.TopicDetail
import com.v2ex.idea.util.escapeHtml
import com.v2ex.idea.util.formatEpochSeconds
import java.awt.BorderLayout
import java.awt.Font
import javax.swing.JButton
import javax.swing.JEditorPane
import javax.swing.JPanel
import javax.swing.event.HyperlinkEvent

class TopicDetailPanel : JPanel(BorderLayout(0, 8)) {
    private val titleLabel = JBLabel("帖子详情")
    private val metaLabel = JBLabel("")
    private val contentPane = JEditorPane("text/html", "")

    private val backButton = JButton("返回首页")
    private val refreshButton = JButton("刷新详情")
    private val openButton = JButton("浏览器打开")
    private val toggleReplyButton = JButton("写评论")

    private val replyArea = JBTextArea(4, 20)
    private val submitReplyButton = JButton("发表评论")
    private val replyPanel = JPanel(BorderLayout(6, 6))

    private var backCallback: (() -> Unit)? = null
    private var openCallback: (() -> Unit)? = null
    private var refreshCallback: (() -> Unit)? = null
    private var submitReplyCallback: ((String) -> Unit)? = null

    init {
        border = JBUI.Borders.empty(8)

        titleLabel.font = titleLabel.font.deriveFont(Font.BOLD, 18f)
        metaLabel.foreground = JBColor.GRAY

        contentPane.isEditable = false
        contentPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true)
        contentPane.addHyperlinkListener { event ->
            if (event.eventType == HyperlinkEvent.EventType.ACTIVATED && event.url != null) {
                BrowserUtil.browse(event.url)
            }
        }

        replyArea.lineWrap = true
        replyArea.wrapStyleWord = true
        replyArea.emptyText.text = "输入评论内容..."

        backButton.addActionListener { backCallback?.invoke() }
        refreshButton.addActionListener { refreshCallback?.invoke() }
        openButton.addActionListener { openCallback?.invoke() }
        toggleReplyButton.addActionListener {
            if (!replyArea.isEnabled) return@addActionListener
            setReplyPanelVisible(!replyPanel.isVisible)
        }
        submitReplyButton.addActionListener {
            val text = replyArea.text.trim()
            if (text.isNotBlank()) {
                submitReplyCallback?.invoke(text)
            }
        }

        val header = JBPanel<JBPanel<*>>(BorderLayout(8, 6)).apply {
            val titleWrap = JPanel(BorderLayout(0, 4)).apply {
                isOpaque = false
                add(titleLabel, BorderLayout.NORTH)
                add(metaLabel, BorderLayout.SOUTH)
            }
            val actions = JPanel().apply {
                isOpaque = false
                add(backButton)
                add(refreshButton)
                add(openButton)
                add(toggleReplyButton)
            }
            add(titleWrap, BorderLayout.CENTER)
            add(actions, BorderLayout.EAST)
        }

        replyPanel.apply {
            border = JBUI.Borders.customLine(JBColor(0xE5E7EB, 0x3C3F41))
            add(JBScrollPane(replyArea), BorderLayout.CENTER)
            add(JPanel(BorderLayout()).apply {
                isOpaque = false
                add(submitReplyButton, BorderLayout.EAST)
            }, BorderLayout.SOUTH)
            isVisible = false
        }

        add(header, BorderLayout.NORTH)
        add(JBScrollPane(contentPane), BorderLayout.CENTER)
        add(replyPanel, BorderLayout.SOUTH)
    }

    fun onBack(callback: () -> Unit) {
        backCallback = callback
    }

    fun onOpenInBrowser(callback: () -> Unit) {
        openCallback = callback
    }

    fun onRefresh(callback: () -> Unit) {
        refreshCallback = callback
    }

    fun onSubmitReply(callback: (String) -> Unit) {
        submitReplyCallback = callback
    }

    fun setReplyEnabled(enabled: Boolean) {
        replyArea.isEnabled = enabled
        submitReplyButton.isEnabled = enabled
        toggleReplyButton.isEnabled = enabled
        if (!enabled) {
            replyArea.emptyText.text = "配置 A2 Token 后可在插件内评论"
            setReplyPanelVisible(false)
            toggleReplyButton.text = "写评论（需登录）"
        } else {
            replyArea.emptyText.text = "输入评论内容..."
            if (!replyPanel.isVisible) {
                toggleReplyButton.text = "写评论"
            }
        }
    }

    fun clearReplyInput() {
        replyArea.text = ""
    }

    fun showPlaceholder() {
        titleLabel.text = "帖子详情"
        metaLabel.text = "从首页选择一个帖子"
        contentPane.text = htmlShell("<p><font color='#6B7280'>从首页点击帖子后可查看正文和评论。</font></p>")
        contentPane.caretPosition = 0
        setReplyPanelVisible(false)
    }

    fun showLoading() {
        titleLabel.text = "加载详情中..."
        metaLabel.text = ""
        contentPane.text = htmlShell("<p>请稍候...</p>")
        contentPane.caretPosition = 0
        setReplyPanelVisible(false)
    }

    fun showError(message: String) {
        titleLabel.text = "加载失败"
        metaLabel.text = message
        contentPane.text = htmlShell("<p><font color='#CC0000'>${escapeHtml(message)}</font></p>")
        contentPane.caretPosition = 0
    }

    fun render(topic: TopicDetail, replies: List<ReplyItem>) {
        titleLabel.text = topic.title
        metaLabel.text = "作者：${topic.author}  节点：${topic.node.ifBlank { "--" }}  时间：${formatEpochSeconds(topic.createdAt)}"

        val bodyHtml = topic.contentHtml.ifBlank { "<pre>${escapeHtml(topic.contentText)}</pre>" }
        val repliesHtml = if (replies.isEmpty()) {
            "<p><font color='#6B7280'>暂无评论</font></p>"
        } else {
            replies.joinToString("<hr/>") { reply ->
                val content = if (reply.contentHtml.isNotBlank()) {
                    reply.contentHtml
                } else {
                    "<pre>${escapeHtml(reply.contentText.ifBlank { "(空评论)" })}</pre>"
                }
                """
                <p><b>#${reply.floor} ${escapeHtml(reply.author)}</b> · ${escapeHtml(formatEpochSeconds(reply.createdAt))}</p>
                $content
                """.trimIndent()
            }
        }

        val html = """
            <h3>正文</h3>
            $bodyHtml
            <hr/>
            <h3>评论 (${replies.size})</h3>
            $repliesHtml
        """.trimIndent()

        contentPane.text = htmlShell(html)
        contentPane.caretPosition = 0
        setReplyPanelVisible(false)
    }

    private fun setReplyPanelVisible(visible: Boolean) {
        replyPanel.isVisible = visible
        if (replyArea.isEnabled) {
            toggleReplyButton.text = if (visible) "收起评论框" else "写评论"
        }
        revalidate()
        repaint()
    }

    private fun htmlShell(inner: String): String = """
        <html>
          <body>
            $inner
          </body>
        </html>
    """.trimIndent()
}
