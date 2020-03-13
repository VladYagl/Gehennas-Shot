package gehenna.ui.panel

import gehenna.ui.Alignment
import gehenna.ui.InputConverter
import gehenna.ui.Settings
import gehenna.ui.Window
import gehenna.utils.Size
import gehenna.utils.at
import gehenna.utils.toColor
import java.awt.BorderLayout
import java.awt.Color
import javax.swing.BorderFactory
import javax.swing.JPanel

open class GehennaPanel(
        size: Size,
        font: AsciiFont,
        final override val fgColor: Color = white,
        final override val bgColor: Color = black,
        private val border: Boolean = false,
        override val keyHandler: InputConverter? = null
) : AsciiPanel(size, font), Window {

    constructor(width: Int, height: Int, font: AsciiFont, fg: Color = white, bg: Color = black, border: Boolean = false) :
            this(Size(width, height), font, fg, bg, border)

    constructor(width: Int, height: Int, settings: Settings, border: Boolean = true, keyHandler: InputConverter? = null) :
            this(Size(width, height), settings.font, settings.foregroundColor, settings.backgroundColor, border, keyHandler)

    final override val panel: JPanel

    init {
        defaultBg = bgColor
        defaultFg = fgColor

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
        //TODO: weird
        super.clear(char, 0, 0, size.width, size.height, defaultFg, defaultBg)
        return this
    }

    override fun clearLine(y: Int) {
        clear(' ', 0, y, size.width, 1)
    }

    override fun writeLine(line: String, y: Int, alignment: Alignment, fg: Color, bg: Color, wrap: Boolean) {
        clearLine(y)
        when (alignment) {
            Alignment.left -> writeColoredLine(line, 0, y, fg, bg, wrap)
            Alignment.center -> writeColoredLine(line, (size.width - line.length) / 2, y, fg, bg, wrap)
            Alignment.right -> TODO()
        }
    }

    private fun writeColoredLine(line: String, x: Int, y: Int, defaultFG: Color, defaultBG: Color, wrap: Boolean = false) {
        //TODO : THIS IS SOME KIND OF SHIT!
        var fg = defaultFG
        var bg = defaultBG
        var i: Int = 0
        cursor = x at y
        while (i < line.length) {
            if (line[i] != '$') {
                if (cursor.y != y && !wrap) {
                    write("...", size.width - 3, y, fg, bg)
                    return
                }
                write(line[i], fg = fg, bg = bg)
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
                assert(line[i + 1] == '{') { "Wrong color command in text" }
                i += 2
                while (line[i] != '}') {
                    color += line[i]
                    i++
                }
                if (background) {
                    bg = if (color == "normal") {
                        defaultBG
                    } else {
                        color.toColor()
                    }
                } else {
                    fg = if (color == "normal") {
                        defaultFG
                    } else {
                        color.toColor()
                    }
                }
                i++
            }
        }
    }

    override fun putChar(char: Char, x: Int, y: Int, fg: Color, bg: Color) {
        write(char, x, y, fg, bg, false)
    }

    override fun changeColors(x: Int, y: Int, fg: Color, bg: Color) {
        changeCharColors(x, y, fg, bg)
    }

    override fun forEachTile(transformer: (Int, Int, TileData) -> Unit) {
        withEachTile(transformer = transformer)
    }

    @Deprecated("Well that's some bullshit dont use it")
    fun forceRepaint() {
        withEachTile { _, _, data -> data.fgColor = Color(data.fgColor.rgb - 1) }
        repaint()
    }
}

