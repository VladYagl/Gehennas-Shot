package gehenna.ui.panel

import gehenna.ui.*
import gehenna.utils.Dir

open class MenuPanel(width: Int, height: Int, settings: Settings) : GehennaPanel(width, height, settings, true), InputListener {
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
                while (focusedItem?.focus() != true) {
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