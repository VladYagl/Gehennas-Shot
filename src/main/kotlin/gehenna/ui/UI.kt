package gehenna.ui

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
    fun newWindow(width: Int, height: Int): Window
    fun removeWindow(window: Window)

    val worldWidth: Int
    val worldHeight: Int

    val info: Window
    val world: Window
}

interface InputListener {
    fun onInput(input: Input)
}