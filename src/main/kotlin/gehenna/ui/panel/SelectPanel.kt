package gehenna.ui.panel

import gehenna.ui.*

class SelectPanel<T>(
        context: UIContext,
        items: List<T>,
        onSelect: (T) -> Unit,
        title: String? = null
) : MenuPanel(100, 30, context.settings) {

    init {
        title?.let { addItem(TextItem(it)) }
        items.forEachIndexed { index, item ->
            addItem(ButtonItem(item.toString(), {
                onSelect(item)
                context.removeWindow(this)
            }, 'a' + index))
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
        onSelect: (List<T>) -> Unit,
        title: String? = null
) : MenuPanel(100, 30, context.settings) {

    private val checkBoxes: List<Pair<CheckItem, T>> = items.mapIndexed { index, item ->
        CheckItem(item.toString(), 'a' + index) to item
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
