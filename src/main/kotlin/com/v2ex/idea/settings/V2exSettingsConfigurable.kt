package com.v2ex.idea.settings

import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPasswordField
import com.intellij.util.ui.FormBuilder
import javax.swing.JComponent
import javax.swing.JPanel

class V2exSettingsConfigurable : SearchableConfigurable {
    private val apiTokenField = JBPasswordField()
    private val a2TokenField = JBPasswordField()
    private val descLabel = JBLabel("API Token 用于 API 访问；A2 Token 用于登录态评论。")

    override fun getId(): String = "settings.v2ex.reader"

    override fun getDisplayName(): String = "V2EX Reader"

    override fun createComponent(): JComponent {
        apiTokenField.columns = 40
        a2TokenField.columns = 40
        return FormBuilder.createFormBuilder()
            .addLabeledComponent("API Token:", apiTokenField)
            .addLabeledComponent("A2 Token:", a2TokenField)
            .addComponent(descLabel)
            .addComponentFillVertically(JPanel(), 0)
            .panel
    }

    override fun isModified(): Boolean {
        val currentApi = String(apiTokenField.password).trim()
        val currentA2 = String(a2TokenField.password).trim()
        val state = V2exSettingsStateService.getInstance()
        return currentApi != state.getApiToken().orEmpty() || currentA2 != state.getA2Token().orEmpty()
    }

    override fun apply() {
        val state = V2exSettingsStateService.getInstance()
        state.setApiToken(String(apiTokenField.password))
        state.setA2Token(String(a2TokenField.password))
    }

    override fun reset() {
        val state = V2exSettingsStateService.getInstance()
        apiTokenField.text = state.getApiToken().orEmpty()
        a2TokenField.text = state.getA2Token().orEmpty()
    }
}
