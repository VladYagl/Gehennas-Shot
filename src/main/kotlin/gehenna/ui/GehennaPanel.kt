package gehenna.ui

import asciiPanel.AsciiFont
import asciiPanel.AsciiPanel
import java.awt.Color

fun AsciiPanel.clearLine(y: Int) {
    clear(' ', 0, y, widthInCharacters, 1)
}

fun AsciiPanel.writeLine(line: String, y: Int) {
    clearLine(y)
    write(line, 0, y)
}

fun AsciiPanel.writeText(text: String, x: Int, y: Int, color: Color = this.defaultForegroundColor) {
    setCursorPosition(x, y)
    text.forEach {
        if (it == '\n' || cursorX >= widthInCharacters) {
            if (cursorY >= heightInCharacters - 1) return
            setCursorPosition(0, cursorY + 1)
        }
        when (it) {
            '\n', '\r' -> {
            }
            '\t' -> write("   ", color)
            else -> write(it, color)
        }
    }
}

class GehennaPanel(width: Int, height: Int, font: AsciiFont) : AsciiPanel(width, height, font), Window {
    override fun writeLine(line: String, y: Int, alignment: Alignment, fg: Color?, bg: Color?) {
        val text = if (line.length < widthInCharacters) line else line.take(widthInCharacters - 3) + "..."
        clearLine(y)
        when (alignment) {
            Alignment.left -> write(text, 0, y, fg ?: defaultForegroundColor,
                    bg ?: defaultBackgroundColor)
            Alignment.center -> writeCenter(text, y, fg ?: defaultForegroundColor,
                    bg ?: defaultBackgroundColor)
            Alignment.right -> TODO()
        }
    }

    override fun putChar(char: Char, x: Int, y: Int, fg: Color?, bg: Color?) {
        write(char, x, y, fg ?: defaultForegroundColor, bg ?: defaultBackgroundColor)
    }

    fun forceRepaint() {
        withEachTile { _, _, data ->
            data.foregroundColor = Color(data.foregroundColor.rgb - 1)
        }
        repaint()
    }
}