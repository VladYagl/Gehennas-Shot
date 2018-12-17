import asciiPanel.AsciiFont
import asciiPanel.AsciiPanel
import java.awt.*
import java.awt.event.KeyEvent
import javax.swing.JFrame
import javax.swing.JLayeredPane

//private val FONT = AsciiFont.TALRYTH_15_15
//private val FONT = AsciiFont.TAFFER_10x10
private val FONT = AsciiFont("Bisasam_16x16.png", 16, 16)

class ApplicationMain : JFrame(), KeyEventDispatcher {
    private val world: AsciiPanel = AsciiPanel(11 * 8, 8 * 8, FONT)
    private val info: AsciiPanel = AsciiPanel(5 * 8, 9 * 8, FONT)
    private val log: AsciiPanel = AsciiPanel(11 * 8, 1 * 8, FONT)

    private val game = Game()

    init {
        title = "Gehenna's Shot"
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

        val trueDarkGray = Color(32, 32, 32)
        world.defaultBackgroundColor = trueDarkGray
        info.defaultBackgroundColor = trueDarkGray
        log.defaultBackgroundColor = trueDarkGray

        world.clear()
        info.clear()
        log.clear()
        world.repaint()
        info.repaint()
        log.repaint()

        pane.size = Dimension(log.width + info.width, info.height + 39)
        size = pane.preferredSize

        Thread {
            var count = 0
            while (true) {
                if (this.isValid) {
                    game.update()
                    info.write("Repaint count = " + count++, 0, 0)
                    repaint()
                }
            }
        }.start()
    }

    private val priority = Array(11 * 8) { Array(8 * 8) { -2 } }
    override fun repaint() {
        priority.forEach { it.fill(-2) }
        for (entity in ComponentManager[Glyph::class, Position::class]) {
            val pos = entity[Position::class]!!
            val glyph = entity[Glyph::class]!!

            if (entity == game.player) {
                info.write("Player glyph = ${glyph.char}|${glyph.priority}", 0, 1)
                info.write("Player pos = ${pos.x}, ${pos.y}", 0, 2)
                info.write("Player pos priority = ${priority[pos.x, pos.y]}", 0, 3)
            }

            if (glyph.priority >= priority[pos.x, pos.y]) {
                world.write(glyph.char, pos.x, pos.y)
                priority[pos.x, pos.y] = glyph.priority
            }
        }
        world.paintImmediately(0, 0, world.width, world.height)
        info.paintImmediately(0, 0, info.width, info.height)
        log.paintImmediately(0, 0, log.width, log.height)
    }

    override fun dispatchKeyEvent(e: KeyEvent): Boolean {
        when (e.id) {
            KeyEvent.KEY_TYPED -> {
                when (e.keyChar) {
                    'j' -> game.player[ThinkUntilSet::class]?.action = Move(game.player, Pair(0, +1))
                    'k' -> game.player[ThinkUntilSet::class]?.action = Move(game.player, Pair(0, -1))
                    'h' -> game.player[ThinkUntilSet::class]?.action = Move(game.player, Pair(-1, 0))
                    'l' -> game.player[ThinkUntilSet::class]?.action = Move(game.player, Pair(+1, 0))
                }
            }
        }
        return false
    }
}

fun main(args: Array<String>) {
    val app = ApplicationMain()
    app.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
    app.isVisible = true

}
