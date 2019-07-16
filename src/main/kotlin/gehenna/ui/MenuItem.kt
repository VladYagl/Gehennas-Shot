package gehenna.ui

import gehenna.ui.panel.GehennaPanel

abstract class MenuItem {
    protected abstract val line: String
    protected val alignment = Alignment.left
    open fun draw(y: Int, panel: GehennaPanel) {
        panel.writeLine((char?.plus(": ") ?: "") + line, y, alignment)
    }

    open val char: Char? = null
    open val select: () -> Unit = {}
    open fun focus(): Boolean = false
    open fun unfocus(): Boolean = false
}

class TextItem(override val line: String) : MenuItem()

open class ButtonItem(override val line: String, override val select: () -> Unit, override val char: Char? = null) : MenuItem() {
    private var focused = false

    override fun draw(y: Int, panel: GehennaPanel) {
        panel.writeLine(
                (char?.plus(": ") ?: "") + line,
                y,
                alignment,
                if (!focused) panel.fgColor else panel.bgColor, if (!focused) panel.bgColor else panel.fgColor
        )
    }

    override fun focus(): Boolean {
        focused = true
        return true
    }

    override fun unfocus(): Boolean {
        focused = false
        return true
    }
}

class CheckItem(private val name: String, override val char: Char? = null) : ButtonItem("- $name", {}, char) {
    var selected = false
        private set

    override val select = { selected = !selected }
    override val line: String
        get() = "${if (selected) "+" else "-"} $name"
}
