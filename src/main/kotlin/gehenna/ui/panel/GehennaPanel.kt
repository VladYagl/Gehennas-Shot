package gehenna.ui.panel

import asciiPanel.AsciiCharacterData
import asciiPanel.AsciiFont
import asciiPanel.AsciiPanel
import asciiPanel.TileTransformer
import gehenna.ui.*
import gehenna.utils.*
import java.awt.BorderLayout
import java.awt.Color
import javax.swing.BorderFactory
import javax.swing.JPanel

open class GehennaPanel(
        override val size: Size,
        font: AsciiFont,
        final override val fgColor: Color = white,
        final override val bgColor: Color = black,
        private val border: Boolean = false,
        override val keyHandler: InputConverter? = null
) : AsciiPanel(size.width, size.height, font), Window {

    constructor(width: Int, height: Int, font: AsciiFont, fg: Color = white, bg: Color = black, border: Boolean = false) :
            this(Size(width, height), font, fg, bg, border)

    constructor(width: Int, height: Int, settings: Settings, border: Boolean = true, keyHandler: InputConverter? = null) :
            this(Size(width, height), settings.font, settings.foregroundColor, settings.backgroundColor, border, keyHandler)

    final override val panel: JPanel

    init {
        defaultBackgroundColor = bgColor
        defaultForegroundColor = fgColor

        if (border) {
            panel = JPanel()
            panel.layout = BorderLayout()
            val empty = BorderFactory.createEmptyBorder(5, 5, 5, 5)
            val line = BorderFactory.createLineBorder(Color.WHITE, 1)
            panel.border = BorderFactory.createCompoundBorder(line, empty)
            panel.size = this.preferredSize
            panel.add(this, BorderLayout.CENTER)
            panel.background = bgColor
        } else {
            panel = this
        }
        clear()
    }

    final override fun clear(char: Char): GehennaPanel {
        super.clear(char)
        return this
    }

    override fun clearLine(y: Int) {
        clear(' ', 0, y, widthInCharacters, 1)
    }

    //todo: write text with auto line breaks
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

    override fun forEachTile(transformer: (Int, Int, AsciiCharacterData) -> Unit) {
        withEachTile(transformer)
    }

    @Deprecated("Well that's some bullshit dont use it")
    fun forceRepaint() {
        withEachTile { _, _, data -> data.foregroundColor = Color(data.foregroundColor.rgb - 1) }
        repaint()
    }
}

