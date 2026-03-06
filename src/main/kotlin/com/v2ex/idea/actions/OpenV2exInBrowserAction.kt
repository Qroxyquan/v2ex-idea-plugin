package com.v2ex.idea.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.v2ex.idea.service.V2exProjectStateService

class OpenV2exInBrowserAction : DumbAwareAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        project.getService(V2exProjectStateService::class.java).controller?.openSelectedInBrowser()
    }

    override fun update(e: AnActionEvent) {
        val controller = e.project
            ?.getService(V2exProjectStateService::class.java)
            ?.controller
        e.presentation.isEnabled = controller?.hasSelectedTopic() == true
    }
}
