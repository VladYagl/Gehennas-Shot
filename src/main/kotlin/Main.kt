import asciiPanel.AsciiFont
import asciiPanel.AsciiPanel
import java.awt.Dimension
import java.awt.KeyEventDispatcher
import java.awt.KeyboardFocusManager
import java.awt.Point
import java.awt.event.KeyEvent
import java.util.*
import javax.swing.JButton
import javax.swing.JFrame
import javax.swing.JLayeredPane

private val FONT = AsciiFont.CP437_12x12

class ApplicationMain : JFrame(), KeyEventDispatcher {
    override fun dispatchKeyEvent(e: KeyEvent): Boolean {
        when (e.id) {
            KeyEvent.KEY_TYPED -> {

            }
        }
        return false
    }

    private val world: AsciiPanel = AsciiPanel(11 * 8, 8 * 8, FONT)
    private val info: AsciiPanel = AsciiPanel(5 * 8, 9 * 8, FONT)
    private val log: AsciiPanel = AsciiPanel(11 * 8, 1 * 8, FONT)

    init {
        isResizable = false
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(this)

        val pane = JLayeredPane()
        add(pane)
        pane.layout = null

        pane.add(log)
        pane.add(world)
        pane.add(info)

        log.size = log.preferredSize
        world.size = world.preferredSize
        info.size = info.preferredSize

        log.location = Point(0, 0)
        world.location = Point(0, log.height + 1)
        info.location = Point(log.width + 1, 0)

        pane.size = Dimension(log.width + info.width, info.height + 39)
        size = pane.preferredSize

        val button = JButton("SHIT")
        val random = Random()
        button.setBounds(50, 50, button.preferredSize.width, button.preferredSize.height)
        pane.add(button)
        pane.moveToFront(button)
        button.addActionListener {
            world.write("@", random.nextInt(world.widthInCharacters), random.nextInt(world.heightInCharacters))
            world.repaint()
//            pane.remove(button)
//            pane.repaint()
        }
    }
}

fun main(args: Array<String>) {
    val app = ApplicationMain()
    app.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
    app.isVisible = true

}
