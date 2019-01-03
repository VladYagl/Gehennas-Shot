package gehenna.ui

import gehenna.Action
import gehenna.Game
import gehenna.components.Logger
import gehenna.components.ThinkUntilSet

class Context(val game: Game, val ui: UI) {
    val log = game.player[Logger::class]!!

    fun action(value: Action) {
        game.player[ThinkUntilSet::class]?.action = value
    }

    fun newWindow(width: Int, height: Int): Window {
        return ui.newWindow(width, height)
    }

    fun removeWindow(window: Window) {
        ui.removeWindow(window)
    }
}