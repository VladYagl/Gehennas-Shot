package gehenna.ui

import gehenna.utils.*
import java.awt.KeyEventDispatcher
import java.awt.event.KeyEvent

//todo exceptions cause it's called from different thread
class InputConverter(private val listener: InputListener) : KeyEventDispatcher {

    private fun getDir(char: Char): Point? {
        return when (char) {
            '.', '5' -> 0 to 0
            'j', '2' -> 0 to +1
            'k', '8' -> 0 to -1
            'h', '4' -> -1 to 0
            'l', '6' -> +1 to 0
            'y', '7' -> -1 to -1
            'u', '9' -> +1 to -1
            'n', '3' -> +1 to +1
            'b', '1' -> -1 to +1
            else -> null
        }
    }

    override fun dispatchKeyEvent(e: KeyEvent): Boolean {
        when (e.id) {
            KeyEvent.KEY_TYPED -> {
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
                }
            }
            KeyEvent.KEY_PRESSED -> {
                when (e.keyCode) {
                    KeyEvent.VK_ESCAPE, KeyEvent.VK_CAPS_LOCK -> listener.onInput(Input.Cancel)
                    KeyEvent.VK_ENTER, KeyEvent.VK_SPACE -> listener.onInput(Input.Accept)
                    KeyEvent.VK_UP -> listener.onInput(Input.Direction(0 to -1))
                    KeyEvent.VK_DOWN -> listener.onInput(Input.Direction(0 to 1))
                    KeyEvent.VK_LEFT -> listener.onInput(Input.Direction(-1 to 0))
                    KeyEvent.VK_RIGHT -> listener.onInput(Input.Direction(1 to 0))
                }
            }
        }
        return false
    }
}
