package com.v2ex.idea.ui

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import com.v2ex.idea.service.V2exProjectStateService

class V2exToolWindowFactory : ToolWindowFactory, DumbAware {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel = V2exMainPanel(project)
        val content = ContentFactory.getInstance().createContent(panel, "", false)
        toolWindow.contentManager.addContent(content)

        val stateService = project.getService(V2exProjectStateService::class.java)
        stateService.controller = panel

        Disposer.register(toolWindow.disposable, panel)
        Disposer.register(toolWindow.disposable, Disposable {
            if (stateService.controller === panel) {
                stateService.controller = null
            }
        })
    }

    override fun shouldBeAvailable(project: Project): Boolean = true
}
