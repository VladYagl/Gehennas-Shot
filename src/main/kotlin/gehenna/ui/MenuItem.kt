package gehenna.ui

import java.awt.Color

abstract class MenuItem {
    abstract val line: String
    protected open val fg: Color? = null
    protected open val bg: Color? = null
    protected open val alignment = Alignment.left
    open fun draw(y: Int, window: Window) {
        window.writeLine(
                (char?.plus(": ") ?: "") + line,
                y,
                alignment,
                fg ?: window.fgColor,
                bg ?: window.bgColor
        )
    }

    open val char: Char? = null
    open val select: () -> Unit = {}
    open fun focus(): Boolean = false
    open fun unfocus(): Boolean = false
}

class TextItem(
        override var line: String = "",
        override val fg: Color? = null,
        override val bg: Color? = null,
        override val alignment: Alignment = Alignment.left
) : MenuItem()

open class ButtonItem(override val line: String, override val select: () -> Unit, override val char: Char? = null) : MenuItem() {
    private var focused = false

    override fun draw(y: Int, window: Window) {
        window.writeLine(
                (char?.plus(": ") ?: "") + line,
                y,
                alignment,
                if (!focused) window.fgColor else window.bgColor, if (!focused) window.bgColor else window.fgColor
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
