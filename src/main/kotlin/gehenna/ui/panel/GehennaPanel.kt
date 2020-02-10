package gehenna.ui.panel

import gehenna.ui.Alignment
import gehenna.ui.InputConverter
import gehenna.ui.Settings
import gehenna.ui.Window
import asciiPanel.AsciiCharacterData
import asciiPanel.AsciiFont
import asciiPanel.AsciiPanel
import gehenna.utils.Size
import gehenna.utils.toColor
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
        clearLine(y)
        when (alignment) {
            Alignment.left -> writeColoredLine(line, 0, y, fg, bg)
            Alignment.center -> writeColoredLine(line, (widthInCharacters - line.length) / 2, y, fg, bg)
            Alignment.right -> TODO()
        }
    }

    private fun writeColoredLine(line: String, x: Int, y: Int, defaultFG: Color, defaultBG: Color) {
        //TODO : THIS IS SOME KIND OF SHIT!
        var fg = defaultFG
        var bg = defaultBG
        var i : Int = 0
        var shiftLeft : Int = 0
        while (i < line.length) {
            if (line[i] != '$') {
                val pos = x + i - shiftLeft
                if (pos >= widthInCharacters) {
                    write("...", widthInCharacters - 3, y, fg, bg)
                    return
                }
                write(line[i], pos, y, fg, bg)
                i++
            } else {
                val oldI = i
                var color = ""
                val background = if (line[i + 1] == '_') {
                    i++
                    true
                } else {
                    false
                }
                assert(line[i + 1] == '{')
                i += 2
                while (line[i] != '}') {
                    color += line[i]
                    i++
                }
                if (background) {
                    bg = if (color == "normal")  {
                        defaultBG
                    } else {
                        color.toColor()
                    }
                } else {
                    fg = if (color == "normal")  {
                        defaultFG
                    } else {
                        color.toColor()
                    }
                }
                i++
                shiftLeft += i - oldI
            }
        }
    }

    override fun putChar(char: Char, x: Int, y: Int, fg: Color, bg: Color) {
        write(char, x, y, fg, bg)
    }

    override fun changeColors(x: Int, y: Int, fg: Color, bg: Color) {
        changeCharColors(x, y, fg, bg)
    }

    //todo: replace AsciiCharacterData with something else
    override fun forEachTile(transformer: (Int, Int, AsciiCharacterData) -> Unit) {
        withEachTile(transformer)
    }

    @Deprecated("Well that's some bullshit dont use it")
    fun forceRepaint() {
        withEachTile { _, _, data -> data.foregroundColor = Color(data.foregroundColor.rgb - 1) }
        repaint()
    }
}

