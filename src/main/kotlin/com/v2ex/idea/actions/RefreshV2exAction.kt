package com.v2ex.idea.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.v2ex.idea.service.V2exProjectStateService

class RefreshV2exAction : DumbAwareAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        project.getService(V2exProjectStateService::class.java).controller?.refreshCurrent()
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = e.project
            ?.getService(V2exProjectStateService::class.java)
            ?.controller != null
    }
}
