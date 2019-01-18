package gehenna.ui

import asciiPanel.AsciiFont
import asciiPanel.AsciiPanel
import java.awt.Color

class GehennaPanel(
    width: Int,
    height: Int,
    font: AsciiFont,
    override val fgColor: Color = white,
    override val bgColor: Color = black
) : AsciiPanel(width, height, font), Window {

    init {
        defaultBackgroundColor = bgColor
        defaultForegroundColor = fgColor
    }

    override fun clearLine(y: Int) {
        clear(' ', 0, y, widthInCharacters, 1)
    }

    override fun writeLine(line: String, y: Int, alignment: Alignment, fg: Color, bg: Color) {
        val text = if (line.length < widthInCharacters) line else line.take(widthInCharacters - 3) + "..."
        clearLine(y)
        when (alignment) {
            Alignment.left -> write(text, 0, y, fg, bg)
            Alignment.center -> writeCenter(text, y, fg, bg)
            Alignment.right -> TODO()
        }
    }

    override fun putChar(char: Char, x: Int, y: Int, fg: Color, bg: Color) {
        write(char, x, y, fg, bg)
    }

    fun forceRepaint() {
        withEachTile { _, _, data -> data.foregroundColor = Color(data.foregroundColor.rgb - 1) }
        repaint()
    }
}