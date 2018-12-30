package gehenna.ui

import asciiPanel.AsciiFont
import asciiPanel.AsciiPanel
import gehenna.Action
import gehenna.Game
import gehenna.components.Logger
import gehenna.components.ThinkUntilSet
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Point
import javax.swing.BorderFactory
import javax.swing.JLayeredPane
import javax.swing.JPanel

class UiContext(val game: Game, val pane: JLayeredPane, val font: AsciiFont) {
    val log = game.player[Logger::class]!!

    fun action(value: Action) {
        game.player[ThinkUntilSet::class]?.action = value
    }

    fun newWindow(width: Int, height: Int, x: Int = (pane.width - width * font.width) / 2, y: Int = (pane.height - height * font.height) / 2): AsciiPanel {
        val window = AsciiPanel(width, height, font)
        val panel = JPanel()
        panel.layout = BorderLayout()
        val empty = BorderFactory.createEmptyBorder(5, 5, 5, 5)
        val line = BorderFactory.createLineBorder(Color.WHITE, 1)
        panel.border = BorderFactory.createCompoundBorder(line, empty)
        panel.size = window.preferredSize
        panel.location = Point(x, y)
        panel.add(window, BorderLayout.CENTER)
        pane.add(panel)
        pane.moveToFront(panel)
        window.defaultBackgroundColor = pane.background
        panel.background = pane.background
        window.clear()
        return window
    }
}