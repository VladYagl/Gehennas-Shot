package gehenna.ui.panel

import gehenna.ui.*
import gehenna.utils.Dir
import org.w3c.dom.Text
import java.awt.Color

open class MenuPanel(width: Int, height: Int, settings: Settings) : GehennaPanel(width, height, settings, true), InputListener {
    override val keyHandler: InputConverter = MenuInput(this)

    var focusedItem: MenuItem? = null
    private val items = ArrayList<MenuItem>()
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
        @Suppress("UNNECESSARY_SAFE_CALL") // todo because it's repaints before creating
        items?.forEachIndexed { index, item -> if (index < size.height - 1) item.draw(index, this) }
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
                val dir = if (input.dir == Dir.north) -1 else +1
                focusedItem = if (focusedItem == null) {
                    items.first()
                } else {
                    focusedItem?.unfocus()
                    val id = items.indexOf(focusedItem!!)
                    val newId = (items.size + id + dir) % items.size
                    items.getOrNull(newId)
                }
                var cnt = 0
                while (focusedItem?.focus() != true && cnt++ < items.size) {
                    focusedItem?.unfocus()
                    val id = items.indexOf(focusedItem!!)
                    val newId = (items.size + id + dir) % items.size
                    focusedItem = items.getOrNull(newId)
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
        else -> false
    }.also {
        repaint()
    }
}