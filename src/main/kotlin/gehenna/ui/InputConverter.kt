package gehenna.ui

import gehenna.utils.Dir
import gehenna.utils.Dir.Companion.east
import gehenna.utils.Dir.Companion.north
import gehenna.utils.Dir.Companion.northeast
import gehenna.utils.Dir.Companion.northwest
import gehenna.utils.Dir.Companion.south
import gehenna.utils.Dir.Companion.southeast
import gehenna.utils.Dir.Companion.southwest
import gehenna.utils.Dir.Companion.west
import gehenna.utils.Dir.Companion.zero
import java.awt.KeyEventDispatcher
import java.awt.event.KeyEvent
import java.awt.event.KeyEvent.*
import java.lang.StringBuilder

//todo exceptions cause it's called from different thread
abstract class InputConverter(private val listener: InputListener) : KeyEventDispatcher {

    protected fun getDir(code: Int): Dir? {
        return when (code) {
            VK_PERIOD, VK_NUMPAD5 -> zero
            VK_DOWN, VK_J, VK_NUMPAD2 -> south
            VK_UP, VK_K, VK_NUMPAD8 -> north
            VK_LEFT, VK_H, VK_NUMPAD4 -> west
            VK_RIGHT, VK_L, VK_NUMPAD6 -> east
            VK_Y, VK_NUMPAD7 -> northwest
            VK_U, VK_NUMPAD9 -> northeast
            VK_N, VK_NUMPAD3 -> southeast
            VK_B, VK_NUMPAD1 -> southwest
            else -> null
        }
    }

    private fun codeName(code: Int): String? {
        return when (code) {
            VK_NUMPAD0 -> "NUM0"
            VK_NUMPAD1 -> "NUM1"
            VK_NUMPAD2 -> "NUM2"
            VK_NUMPAD3 -> "NUM3"
            VK_NUMPAD4 -> "NUM4"
            VK_NUMPAD5 -> "NUM5"
            VK_NUMPAD6 -> "NUM6"
            VK_NUMPAD7 -> "NUM7"
            VK_NUMPAD8 -> "NUM8"
            VK_NUMPAD9 -> "NUM9"

            VK_UP -> "UP"
            VK_DOWN -> "DOWN"
            VK_LEFT -> "LEFT"
            VK_RIGHT -> "RIGHT"

            VK_ESCAPE, VK_CAPS_LOCK -> "ESC"
            VK_BACK_SPACE -> "BACK_SPACE"
            VK_ENTER -> "ENTER"
            VK_SPACE -> "SPACE"

            VK_PAGE_DOWN -> "PAGE_DOWN"
            VK_PAGE_UP -> "PAGE_UP"
            else -> null
        }
    }

    open val keyMap = HashMap<String, Input>()

    override fun dispatchKeyEvent(e: KeyEvent): Boolean {
        return when (e.id) {
            KEY_TYPED -> consumeChar(e.keyChar)?.let { listener.onInput(it) } ?: false
            KEY_PRESSED -> consumeKey(e)?.let { listener.onInput(it) } ?: run {
                val command = StringBuilder()
                        .append(if (e.isAltDown) "!" else "")
                        .append(if (e.isControlDown) "^" else "")
                        .append(if (e.isShiftDown) "+" else "")
                        .append(codeName(e.keyCode) ?: e.keyChar.toLowerCase())
                        .toString()
                println(command)
                keyMap[command]?.let { listener.onInput(it) } ?: false
            }
            else -> false
        }
    }

    open fun consumeKey(e: KeyEvent): Input? = null
    open fun consumeChar(char: Char): Input? = null
}

class GameInput(listener: InputListener) : InputConverter(listener) {

    override val keyMap = hashMapOf(
            "+q" to Input.Quit,
            "f" to Input.Fire,
            "t" to Input.Throw,
            "+<" to Input.ClimbStairs,
            "o" to Input.Open,
            "/" to Input.Wait,
            "+?" to Input.Help,
            "c" to Input.Close,
            "`" to Input.Console,
            "§" to Input.Console,
            ";" to Input.Examine,
            "," to Input.Pickup,
            "g" to Input.Pickup,
            "d" to Input.Drop,
            "e" to Input.Equip,
            "a" to Input.Use,
            "i" to Input.Inventory,
            "r" to Input.Reload,
            "-" to Input.Decrease,
            "=" to Input.Increase,
            "++" to Input.Increase,
            "ESC" to Input.Cancel,
            "ENTER" to Input.Accept,
            "SPACE" to Input.Accept
    )

    override fun consumeKey(e: KeyEvent): Input? {
        return getDir(e.keyCode)?.let {
            if (!e.isShiftDown) Input.Direction(it)
            else Input.Run(it)
        }
    }
}

class TextInput(listener: InputListener) : InputConverter(listener) {
    override val keyMap: HashMap<String, Input> = hashMapOf(
            "BACK_SPACE" to Input.Backspace,
            "ESC" to Input.Cancel,
            "ENTER" to Input.Accept
    )

    override fun consumeChar(char: Char): Input? {
        return if (char.isLetterOrDigit() || char == ' ' || char == '-' || char == '_' || char == '.' || char == ',')
            Input.Char(char)
        else null
    }
}

class MenuInput(listener: InputListener) : InputConverter(listener) {
    override val keyMap: HashMap<String, Input> = hashMapOf(
            "ESC" to Input.Cancel,
            "ENTER" to Input.Accept,
            "SPACE" to Input.Accept,
            "PAGE_UP" to Input.ScrollUp,
            "PAGE_DOWN" to Input.ScrollDown
    )

    override fun consumeKey(e: KeyEvent): Input? {
        return getDir(e.keyCode)?.let {
            if ((it == north || it == south || it == east || it == west) && !e.isShiftDown)
                Input.Direction(it)
            else null
        } ?: if (e.keyChar.isLetter()) {
            Input.Char(e.keyChar)
        } else null
    }
}
