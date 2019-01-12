package gehenna.ui

import gehenna.component.Logger
import gehenna.component.behaviour.ThinkUntilSet
import gehenna.core.Action
import gehenna.core.Context

class UIContext(private val context: Context, private val ui: UI) : Context by context {
    val log get() = player[Logger::class]!!

    var action: Action? = null
        set(value) {
            value?.let { action ->
                player[ThinkUntilSet::class]?.action = action
            }
        }

    fun newWindow(width: Int, height: Int): Window {
        return ui.newWindow(width, height)
    }

    fun removeWindow(window: Window) {
        ui.removeWindow(window)
    }
}