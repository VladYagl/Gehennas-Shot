package gehenna.ui

import gehenna.component.Logger
import gehenna.component.behaviour.ThinkUntilSet
import gehenna.core.Action
import gehenna.core.Game

class Context(val game: Game, val ui: UI) {
    val log = game.player[Logger::class]!!

    var action: Action? = null
        set(value) {
            value?.let { action ->
                game.player[ThinkUntilSet::class]?.action = action
            }
        }

    fun newWindow(width: Int, height: Int): Window {
        return ui.newWindow(width, height)
    }

    fun removeWindow(window: Window) {
        ui.removeWindow(window)
    }
}