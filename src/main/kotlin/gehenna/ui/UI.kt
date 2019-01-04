package gehenna.ui

import java.awt.Color

enum class Alignment {
    left, center, right;
}

interface Window {
    fun writeLine(line: String, y: Int, alignment: Alignment = Alignment.left, fg: Color? = null, bg: Color? = null)
    fun putChar(char: Char, x: Int, y: Int, fg: Color? = null, bg: Color? = null)
    fun repaint()
}

interface UI {
    fun endGame() // TODO : replace through new window
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