package gehenna.ui

import gehenna.utils.Point
import gehenna.utils.Size
import java.awt.Color

enum class Alignment {
    left, center, right;
}

interface Window {
    val fgColor: Color
    val bgColor: Color

    fun clearLine(y: Int)
    fun writeLine(line: String, y: Int, alignment: Alignment = Alignment.left, fg: Color = fgColor, bg: Color = bgColor)
    fun putChar(char: Char, x: Int, y: Int, fg: Color = fgColor, bg: Color = bgColor)
    fun repaint()
}

interface UI {
    fun printException(e: Throwable)
    fun update()
    fun updateLog(messages: ArrayList<String>)
    fun newWindow(size: Size): Window
    fun newWindow(width: Int, height: Int): Window
    fun removeWindow(window: Window)

    val worldSize: Size

    val info: Window
    val world: Window

    fun showCursor()
    fun hideCursor()
    fun setCursor(point: Point)
}

interface InputListener {
    fun onInput(input: Input)
}