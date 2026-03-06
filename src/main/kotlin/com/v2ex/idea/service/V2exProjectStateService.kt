package com.v2ex.idea.service

import com.intellij.openapi.components.Service
import com.v2ex.idea.ui.V2exPanelController

@Service(Service.Level.PROJECT)
class V2exProjectStateService {
    @Volatile
    var controller: V2exPanelController? = null
}
