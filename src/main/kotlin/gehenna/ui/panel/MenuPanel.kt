package gehenna.ui.panel

import gehenna.ui.*
import gehenna.utils.Dir
import org.w3c.dom.Text
import java.awt.Color

open class MenuPanel(width: Int, height: Int, settings: Settings) : GehennaPanel(width, height, settings, true), InputListener {
    override val keyHandler: InputConverter = MenuInput(this)

    val items = ArrayList<MenuItem>()
    private var scroll: Int = 0
    private var focusedItem: MenuItem? = null
    private var onCancel = {}
    private var onAccept = {
        focusedItem?.let { it.select() }
    }

    init {
        repaint()
    }

    fun addItem(item: MenuItem) {
        items.add(item)
        repaint()
    }

    fun addText(text: String, fg: Color? = null, bg: Color? = null, alignment: Alignment = Alignment.left) {
        addItem(TextItem(text, fg, bg))
    }


    fun clearItems() {
        items.clear()
    }

    fun setOnCancel(callback: () -> Unit) {
        onCancel = callback
    }

    fun setOnAccept(callback: () -> Unit) {
        onAccept = callback
    }

    final override fun repaint() {
        update()
        super.repaint()
    }

    fun update() {
        @Suppress("SENSELESS_COMPARISON") // because repaint is called before constructor for some reason
        if (items != null) {
            for (i in scroll until (scroll + size.height - 1)) {
                items.getOrNull(i)?.draw(i - scroll, this)
            }
        }
    }

    private fun moveFocus(fullDir: Dir) {
        val dir = if (fullDir == Dir.north) -1 else +1
        focusedItem = if (focusedItem == null) {
            items.first()
        } else {
            focusedItem?.unfocus()
            val id = items.indexOf(focusedItem!!)
            val newId = (items.size + id + dir) % items.size

            while (newId > scroll + size.height - 1) {
                scroll++
            }

            while (newId < scroll) {
                scroll--
            }

            items.getOrNull(newId)
        }
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
            if (input.dir == Dir.north || input.dir == Dir.south) {
                //todo: i dont like how it looks but whatever
                moveFocus(input.dir)
                var cnt = 0
                while (focusedItem?.focus() != true && cnt++ < items.size) {
                    moveFocus(input.dir)
                }
                true
            } else if (input.dir == Dir.west || input.dir == Dir.east) {
                focusedItem?.let { it.select() }
                true
            } else {
                false
            }
        }
        Input.Cancel -> {
            onCancel()
            true
        }
        Input.Accept -> {
            onAccept()
            true
        }
        Input.ScrollDown -> {
            scroll = kotlin.math.min(scroll + 1, items.size - size.height)
            true
        }
        Input.ScrollUp -> {
            scroll = kotlin.math.max(scroll - 1, 0)
            true
        }
        else -> false
    }.also {
        repaint()
    }
}