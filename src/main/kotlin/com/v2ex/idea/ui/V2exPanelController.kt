package com.v2ex.idea.ui

interface V2exPanelController {
    fun refreshCurrent()
    fun openSelectedInBrowser()
    fun focusSearch()
    fun hasSelectedTopic(): Boolean
}
