package com.v2ex.idea.ui

import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.JBUI
import com.v2ex.idea.model.TopicSummary
import java.awt.BorderLayout
import java.awt.Font
import java.awt.GridLayout
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JPanel
import javax.swing.SwingConstants

class V2exSidebarPanel : JPanel(BorderLayout(0, 8)) {
    private val modeLabel = JBLabel("当前：首页")

    private val topicCountValue = statValue("0")
    private val nodeCountValue = statValue("0")
    private val selectedRepliesValue = statValue("0")

    private val topicCountTitle = statTitle("帖子")
    private val nodeCountTitle = statTitle("节点")
    private val selectedRepliesTitle = statTitle("选中回复")

    private val favoriteNodesArea = JBTextArea()
    private val hotTopicsArea = JBTextArea()

    init {
        border = JBUI.Borders.empty(8)
        modeLabel.border = JBUI.Borders.emptyBottom(6)
        modeLabel.foreground = JBColor(0x4B5563, 0xA0A7B0)

        val stack = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            isOpaque = false
            add(statCard())
            add(Box.createVerticalStrut(8))
            add(favoriteNodesCard())
            add(Box.createVerticalStrut(8))
            add(hotTopicsCard())
        }

        add(modeLabel, BorderLayout.NORTH)
        add(stack, BorderLayout.CENTER)
    }

    fun update(mode: String, topics: List<TopicSummary>, selected: TopicSummary?) {
        modeLabel.text = "当前：$mode"
        topicCountValue.text = topics.size.toString()
        nodeCountValue.text = topics.map { it.node }.filter { it.isNotBlank() }.distinct().size.toString()
        selectedRepliesValue.text = (selected?.repliesCount ?: 0).toString()

        val nodeLines = topics
            .filter { it.node.isNotBlank() }
            .groupingBy { it.node }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .take(6)
            .joinToString("\n") { "• ${it.key}  (${it.value})" }
        favoriteNodesArea.text = if (nodeLines.isBlank()) "暂无节点数据" else nodeLines

        val hotLines = topics
            .sortedByDescending { it.repliesCount }
            .take(5)
            .mapIndexed { idx, topic -> "${idx + 1}. ${topic.title}" }
            .joinToString("\n")
        hotTopicsArea.text = if (hotLines.isBlank()) "暂无热议主题" else hotLines
    }

    private fun statCard(): JPanel {
        val values = JPanel(GridLayout(1, 3, 0, 0)).apply {
            add(statUnit(topicCountValue, topicCountTitle))
            add(statUnit(nodeCountValue, nodeCountTitle))
            add(statUnit(selectedRepliesValue, selectedRepliesTitle))
        }

        return card("浏览概览", values)
    }

    private fun favoriteNodesCard(): JPanel = card("我收藏的节点（本次会话）", favoriteNodesArea.apply {
        isEditable = false
        lineWrap = true
        wrapStyleWord = true
        border = JBUI.Borders.empty(4)
        background = JBColor(0xF9FAFB, 0x313335)
    })

    private fun hotTopicsCard(): JPanel = card("今日热议主题", hotTopicsArea.apply {
        isEditable = false
        lineWrap = true
        wrapStyleWord = true
        border = JBUI.Borders.empty(4)
        background = JBColor(0xF9FAFB, 0x313335)
    })

    private fun statUnit(value: JBLabel, title: JBLabel): JPanel = JPanel(BorderLayout()).apply {
        isOpaque = false
        add(value, BorderLayout.CENTER)
        add(title, BorderLayout.SOUTH)
        border = JBUI.Borders.empty(6, 4)
    }

    private fun card(title: String, content: JPanel): JPanel = JPanel(BorderLayout(0, 8)).apply {
        background = JBColor(0xFFFFFF, 0x2B2D30)
        border = JBUI.Borders.compound(
            JBUI.Borders.customLine(JBColor(0xE5E7EB, 0x3C3F41)),
            JBUI.Borders.empty(10),
        )
        add(JBLabel(title).apply { font = font.deriveFont(Font.BOLD, 13f) }, BorderLayout.NORTH)
        add(content, BorderLayout.CENTER)
    }

    private fun card(title: String, content: JBTextArea): JPanel = JPanel(BorderLayout(0, 8)).apply {
        background = JBColor(0xFFFFFF, 0x2B2D30)
        border = JBUI.Borders.compound(
            JBUI.Borders.customLine(JBColor(0xE5E7EB, 0x3C3F41)),
            JBUI.Borders.empty(10),
        )
        add(JBLabel(title).apply { font = font.deriveFont(Font.BOLD, 13f) }, BorderLayout.NORTH)
        add(content, BorderLayout.CENTER)
    }

    private fun statValue(value: String): JBLabel = JBLabel(value, SwingConstants.CENTER).apply {
        font = font.deriveFont(Font.BOLD, 23f)
        foreground = JBColor(0x111827, 0xD0D7E1)
    }

    private fun statTitle(title: String): JBLabel = JBLabel(title, SwingConstants.CENTER).apply {
        foreground = JBColor.GRAY
    }
}
