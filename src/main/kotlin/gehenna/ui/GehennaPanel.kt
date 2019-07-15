package gehenna.ui

import asciiPanel.AsciiFont
import asciiPanel.AsciiPanel
import gehenna.utils.*
import gehenna.utils.Dir.Companion.east
import gehenna.utils.Dir.Companion.north
import gehenna.utils.Dir.Companion.south
import gehenna.utils.Dir.Companion.west
import java.awt.BorderLayout
import java.awt.Color
import javax.swing.BorderFactory
import javax.swing.JPanel

open class GehennaPanel(
        override val size: Size,
        font: AsciiFont,
        final override val fgColor: Color = white,
        final override val bgColor: Color = black,
        private val border: Boolean = false
) : AsciiPanel(size.width, size.height, font), Window {

    constructor(width: Int, height: Int, font: AsciiFont, fg: Color = white, bg: Color = black, border: Boolean = false) :
            this(Size(width, height), font, fg, bg, border)

    constructor(width: Int, height: Int, settings: Settings, border: Boolean = true) :
            this(Size(width, height), settings.font, settings.foregroundColor, settings.backgroundColor, border)

    final override val panel: JPanel
    override val keyHandler: InputConverter? = null

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

    final override fun clear(): AsciiPanel {
        return super.clear()
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

    @Deprecated("Well that's some bullshit dont use it")
    fun forceRepaint() {
        withEachTile { _, _, data -> data.foregroundColor = Color(data.foregroundColor.rgb - 1) }
        repaint()
    }
}

class MenuPanel(width: Int, height: Int, settings: Settings) : GehennaPanel(width, height, settings, true), InputListener {
    override val keyHandler: InputConverter = MenuInput(this)

    var focusedItem: MenuItem? = null
    private val items = ArrayList<MenuItem>()
    private var onCancel = {}
    private var onAccept = {
        focusedItem?.let { it.select() }
    }

    fun addItem(item: MenuItem) {
        items.add(item)
        repaint()
    }

    fun setOnCancel(callback: () -> Unit) {
        onCancel = callback
    }

    fun setOnAccept(callback: () -> Unit) {
        onAccept = callback
    }

    override fun repaint() {
        items?.forEachIndexed { index, item -> item.draw(index, this) }
        super.repaint()
    }

    override fun onInput(input: Input) = when (input) {
        is Input.Char -> {
            focusedItem?.unfocus()
            focusedItem = items.find { it.char == input.char }
            focusedItem?.let {
                it.focus()
                it.select()
            }
            true
        }
        is Input.Direction -> {
            if (input.dir == north || input.dir == south) {
                val current = focusedItem
                if (current == null) {
                    focusedItem = items.first()
                    focusedItem?.focus()
                } else {
                    current.unfocus()
                    val id = items.indexOf(current)
                    val newId = (items.size + id + if (input.dir == north) -1 else +1) % items.size
                    focusedItem = items.getOrNull(newId)
                    focusedItem?.focus()
                }
                true
            } else if (input.dir == west || input.dir == east) {
                focusedItem?.let { it.select() }
                true
            } else {
                false
            }
        }
        is Input.Cancel -> {
            onCancel()
            true
        }
        is Input.Accept -> {
            onAccept()
            true
        }
        else -> false
    }.also {
        repaint()
    }
}