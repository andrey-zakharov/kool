package de.fabmax.kool.modules.ui2

import de.fabmax.kool.input.KeyEvent

interface Focusable : UiScope {
    val isFocused: MutableStateValue<Boolean>

    fun requestFocus() {
        surface.requestFocus(this)
    }

    fun onFocusGain() {
        isFocused.set(true)
    }

    fun onFocusLost() {
        isFocused.set(false)
    }

    fun onKeyEvent(keyEvent: KeyEvent)
}