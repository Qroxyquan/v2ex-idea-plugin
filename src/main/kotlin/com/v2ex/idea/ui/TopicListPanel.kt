package com.v2ex.idea.ui

import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import com.v2ex.idea.model.TopicSummary
import com.v2ex.idea.util.formatEpochSeconds
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.Dimension
import java.awt.Font
import javax.swing.DefaultListModel
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.ListCellRenderer
import javax.swing.ListSelectionModel
import javax.swing.SwingConstants

class TopicListPanel(private val defaultEmptyText: String) : JPanel(BorderLayout()) {
    private val listModel = DefaultListModel<TopicSummary>()
    private val topicList = JBList(listModel)
    private val statusLabel = JBLabel(defaultEmptyText)

    private var selectionCallback: ((TopicSummary) -> Unit)? = null

    init {
        border = JBUI.Borders.empty(6, 8)

        statusLabel.border = JBUI.Borders.empty(0, 2, 6, 2)
        statusLabel.foreground = JBColor.GRAY

        topicList.selectionMode = ListSelectionModel.SINGLE_SELECTION
        topicList.cellRenderer = TopicCellRenderer()
        topicList.fixedCellHeight = 86
        topicList.addListSelectionListener {
            if (!it.valueIsAdjusting) {
                topicList.selectedValue?.let { topic -> selectionCallback?.invoke(topic) }
            }
        }
        topicList.addMouseListener(object : java.awt.event.MouseAdapter() {
            override fun mouseClicked(e: java.awt.event.MouseEvent) {
                val index = topicList.locationToIndex(e.point)
                if (index >= 0) {
                    topicList.selectedIndex = index
                    topicList.model.getElementAt(index)?.let { topic -> selectionCallback?.invoke(topic) }
                }
            }
        })

        val scrollPane = JBScrollPane(topicList).apply {
            border = JBUI.Borders.customLine(JBColor(0xE5E7EB, 0x3C3F41))
        }

        add(statusLabel, BorderLayout.NORTH)
        add(scrollPane, BorderLayout.CENTER)
    }

    fun onTopicSelected(callback: (TopicSummary) -> Unit) {
        selectionCallback = callback
    }

    fun showLoading(text: String = "加载中...") {
        statusLabel.text = text
    }

    fun showError(message: String) {
        statusLabel.text = "错误：$message"
    }

    fun setTopics(topics: List<TopicSummary>, emptyText: String = defaultEmptyText) {
        listModel.clear()
        topics.forEach { listModel.addElement(it) }
        statusLabel.text = if (topics.isEmpty()) emptyText else "共 ${topics.size} 条"
    }

    fun selectedTopic(): TopicSummary? = topicList.selectedValue

    fun clearSelection() {
        topicList.clearSelection()
    }

    fun selectFirst() {
        if (listModel.size() > 0 && topicList.selectedIndex < 0) {
            topicList.selectedIndex = 0
        }
    }

    fun isEmpty(): Boolean = listModel.isEmpty

    private class TopicCellRenderer : JPanel(BorderLayout(12, 0)), ListCellRenderer<TopicSummary> {
        private val avatar = JLabel("V", SwingConstants.CENTER)
        private val title = JLabel()
        private val meta = JLabel()
        private val replies = JLabel("0", SwingConstants.CENTER)

        private val center = JPanel(BorderLayout(0, 6))

        init {
            border = JBUI.Borders.compound(
                JBUI.Borders.customLineBottom(JBColor(0xECEEF2, 0x3B3F45)),
                JBUI.Borders.empty(10, 12),
            )

            avatar.preferredSize = Dimension(42, 42)
            avatar.minimumSize = Dimension(42, 42)
            avatar.maximumSize = Dimension(42, 42)
            avatar.font = avatar.font.deriveFont(Font.BOLD, 15f)
            avatar.foreground = JBColor.WHITE
            avatar.isOpaque = true

            title.font = title.font.deriveFont(Font.BOLD, 14f)

            meta.foreground = JBColor.GRAY
            meta.font = meta.font.deriveFont(12f)

            replies.preferredSize = Dimension(42, 26)
            replies.minimumSize = Dimension(42, 26)
            replies.maximumSize = Dimension(42, 26)
            replies.isOpaque = true
            replies.background = JBColor(0xEFF3F8, 0x2F3640)
            replies.foreground = JBColor(0x4B5563, 0xC2C8D0)
            replies.border = JBUI.Borders.empty(2, 6)

            center.isOpaque = false
            center.add(title, BorderLayout.NORTH)
            center.add(meta, BorderLayout.CENTER)

            add(avatar, BorderLayout.WEST)
            add(center, BorderLayout.CENTER)
            add(replies, BorderLayout.EAST)
        }

        override fun getListCellRendererComponent(
            list: JList<out TopicSummary>,
            value: TopicSummary,
            index: Int,
            isSelected: Boolean,
            cellHasFocus: Boolean,
        ): Component {
            title.text = value.title
            meta.text = buildString {
                append(value.node.ifBlank { "未分类" })
                append("  ·  ")
                append(value.author)
                append("  ·  ")
                append("最后更新 ")
                append(formatEpochSeconds(value.lastTouchedAt ?: value.createdAt))
                append("  ·  评论 ")
                append(value.repliesCount)
            }
            replies.text = value.repliesCount.toString()
            avatar.text = value.author.take(1).uppercase()
            avatar.background = avatarPalette(value.author)

            val selectedBg = JBColor(0xEAF2FF, 0x2D3A4A)
            background = if (isSelected) selectedBg else list.background
            center.background = background

            return this
        }

        private fun avatarPalette(seed: String): Color {
            val colors = arrayOf(
                Color(0xEF4444),
                Color(0xF59E0B),
                Color(0x10B981),
                Color(0x3B82F6),
                Color(0x8B5CF6),
                Color(0xEC4899),
            )
            val idx = kotlin.math.abs(seed.hashCode()) % colors.size
            return colors[idx]
        }
    }
}
