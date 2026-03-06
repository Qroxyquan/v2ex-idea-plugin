package com.v2ex.idea.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@State(name = "V2exReaderSettings", storages = [Storage("v2ex-reader.xml")])
@Service(Service.Level.APP)
class V2exSettingsStateService : PersistentStateComponent<V2exSettingsStateService.State> {
    data class State(
        var apiToken: String = "",
        var a2Token: String = "",
    )

    private var state = State()

    override fun getState(): State = state

    override fun loadState(state: State) {
        this.state = state
    }

    fun getApiToken(): String? = state.apiToken.trim().ifBlank { null }

    fun setApiToken(token: String?) {
        state.apiToken = token.orEmpty().trim()
    }

    fun getA2Token(): String? = state.a2Token.trim().ifBlank { null }

    fun setA2Token(token: String?) {
        state.a2Token = token.orEmpty().trim()
    }

    companion object {
        fun getInstance(): V2exSettingsStateService =
            ApplicationManager.getApplication().getService(V2exSettingsStateService::class.java)
    }
}
