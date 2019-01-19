package gehenna.ui

import gehenna.utils.*
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

//todo exceptions cause it's called from different thread
class InputConverter(private val listener: InputListener) : KeyEventDispatcher {

    private fun getDir(char: Char): Dir? {
        return when (char) {
            '.', '5' -> zero
            'j', '2' -> south
            'k', '8' -> north
            'h', '4' -> west
            'l', '6' -> east
            'y', '7' -> northwest
            'u', '9' -> northeast
            'n', '3' -> southeast
            'b', '1' -> southwest
            else -> null
        }
    }

    override fun dispatchKeyEvent(e: KeyEvent): Boolean {
        when (e.id) {
            KeyEvent.KEY_TYPED -> {
                if (e.keyChar != '\b') //fixme
                    listener.onInput(Input.Char(e.keyChar))
                getDir(e.keyChar)?.let { dir -> listener.onInput(Input.Direction(dir)) }
                when (e.keyChar) {
                    'Q' -> listener.onInput(Input.Quit)
                    'f' -> listener.onInput(Input.Fire)
                    ',',
                    'g' -> listener.onInput(Input.Pickup)
                    'd' -> listener.onInput(Input.Drop)
                    '>',
                    '<' -> listener.onInput(Input.ClimbStairs)
                    'o' -> listener.onInput(Input.Open)
                    'c' -> listener.onInput(Input.Close)
                    '`' -> listener.onInput(Input.Console)
                }
            }
            KeyEvent.KEY_PRESSED -> {
                when (e.keyCode) {
                    KeyEvent.VK_ESCAPE, KeyEvent.VK_CAPS_LOCK -> listener.onInput(Input.Cancel)
                    KeyEvent.VK_BACK_SPACE -> listener.onInput(Input.Backspace)
                    KeyEvent.VK_ENTER -> listener.onInput(Input.Accept)
                    KeyEvent.VK_UP -> listener.onInput(Input.Direction(0 on -1))
                    KeyEvent.VK_DOWN -> listener.onInput(Input.Direction(0 on 1))
                    KeyEvent.VK_LEFT -> listener.onInput(Input.Direction(-1 on 0))
                    KeyEvent.VK_RIGHT -> listener.onInput(Input.Direction(1 on 0))
                }
            }
        }
        return false
    }
}
