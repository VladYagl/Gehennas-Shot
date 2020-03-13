package gehenna.ui.panel

import gehenna.ui.*

private fun calcChar(index: Int): Char? {
    val letters = 'z' - 'a'
    return when {
        index < letters -> 'a' + index
        index - letters < letters -> 'A' + index - letters
        else -> null
    }
}

class SelectPanel<T>(
        context: UIContext,
        items: Iterable<T>,
        title: String? = null,
        toString: (T) -> String = { it.toString() },
        onSelect: (T) -> Unit
) : MenuPanel(100, 30, context.settings) {

    init {
        title?.let { addItem(TextItem(it)) }
        items.forEachIndexed { index, item ->
            addItem(ButtonItem(toString(item), {
                onSelect(item)
                context.removeWindow(this)
            }, calcChar(index)))
        }
        setOnCancel {
            context.log.addTemp("Never mind")
            context.removeWindow(this)
        }
    }
}

class MultiSelectPanel<T>(
        context: UIContext,
        items: List<T>,
        title: String? = null,
        toString: (T) -> String = { it.toString() },
        onSelect: (List<T>) -> Unit
) : MenuPanel(100, 30, context.settings) {

    private val checkBoxes: List<Pair<CheckItem, T>> = items.mapIndexed { index, item ->
        CheckItem(toString(item), calcChar(index)) to item
    }

    init {
        title?.let { addItem(TextItem(it)) }
        checkBoxes.forEach { addItem(it.first) }
        setOnAccept {
            onSelect(checkBoxes.filter { it.first.selected }.map { it.second })
            context.removeWindow(this)
        }
        setOnCancel {
            context.log.addTemp("Never mind")
            context.removeWindow(this)
        }
    }
}
