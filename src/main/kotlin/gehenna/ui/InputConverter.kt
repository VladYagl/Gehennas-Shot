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
import gehenna.utils.on
import java.awt.KeyEventDispatcher
import java.awt.event.KeyEvent
import java.awt.event.KeyEvent.*

//todo exceptions cause it's called from different thread
class InputConverter(private val listener: InputListener) : KeyEventDispatcher {

    private fun getDir(code: Int): Dir? {
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

    override fun dispatchKeyEvent(e: KeyEvent): Boolean {
        when (e.id) {
            KEY_TYPED -> {
                val consumed = when (e.keyChar) {
                    'Q' -> listener.onInput(Input.Quit)
                    'f' -> listener.onInput(Input.Fire)
                    ',',
                    'g' -> listener.onInput(Input.Pickup)
                    'd' -> listener.onInput(Input.Drop)
                    'e' -> listener.onInput(Input.Equip)
                    '>',
                    '<' -> listener.onInput(Input.ClimbStairs)
                    'o' -> listener.onInput(Input.Open)
                    'c' -> listener.onInput(Input.Close)
                    '`' -> listener.onInput(Input.Console)
                    ';' -> listener.onInput(Input.Examine)
                    else -> false
                }
                if (!consumed && e.keyChar != '\b') //fixme
                    listener.onInput(Input.Char(e.keyChar)) // todo: sometimes it can cause double action
            }
            KEY_PRESSED -> {
                getDir(e.keyCode)?.let { dir ->
                    if (!e.isShiftDown) {
                        listener.onInput(Input.Direction(dir))
                    } else {
                        listener.onInput(Input.Run(dir))
                    }
                }
                when (e.keyCode) {
                    VK_ESCAPE, VK_CAPS_LOCK -> listener.onInput(Input.Cancel)
                    VK_BACK_SPACE -> listener.onInput(Input.Backspace)
                    VK_ENTER -> listener.onInput(Input.Accept)
                    VK_UP -> listener.onInput(Input.Direction(0 on -1))
                    VK_DOWN -> listener.onInput(Input.Direction(0 on 1))
                    VK_LEFT -> listener.onInput(Input.Direction(-1 on 0))
                    VK_RIGHT -> listener.onInput(Input.Direction(1 on 0))
                }
            }
        }
        return false
    }
}
