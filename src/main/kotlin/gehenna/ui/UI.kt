package gehenna.ui

import gehenna.ui.panel.MenuPanel
import gehenna.ui.panel.AsciiPanel
import gehenna.utils.Point
import gehenna.utils.Size
import java.awt.Color
import javax.swing.JPanel

@Suppress("EnumEntryName")
enum class Alignment {
    left, center, right;
}

const val EMPTY_CHAR = 255.toChar()

interface Window {
    val fgColor: Color
    val bgColor: Color
    val size: Size

    val panel: JPanel
    val keyHandler: InputConverter?

    fun clear(char: Char = ' '): Window
    fun clearLine(y: Int)
    fun writeLine(line: String, y: Int, alignment: Alignment = Alignment.left, fg: Color = fgColor, bg: Color = bgColor, wrap: Boolean = false)
    fun putChar(char: Char, x: Int, y: Int, fg: Color = fgColor, bg: Color = bgColor)
    fun changeColors(x: Int, y: Int, fg: Color, bg: Color)
    fun repaint()

    fun forEachTile(transformer: (Int, Int, AsciiPanel.TileData) -> Unit)
}

interface UI {
    fun printException(e: Throwable)
    fun update()
    fun updateLog(messages: List<String>)
    fun addWindow(window: Window)
    suspend fun <T> loadingWindow(text: String, task: () -> T): T
    fun removeWindow(window: Window)

    val worldSize: Size
    val settings: Settings

    val info: MenuPanel
    val world: Window
    val hud: Window

    fun moveFocus(playerPos: Point)
    fun focusPlayer()

    fun putCharOnHUD(char: Char, point: Point, fg: Color? = null, bg: Color? = null)
    fun animateChar(char: Char, point: Point, time: Long = 150, fg: Color? = null, bg: Color? = null)
}

interface InputListener {
    fun onInput(input: Input): Boolean
}