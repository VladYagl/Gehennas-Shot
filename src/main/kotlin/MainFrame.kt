import asciiPanel.AsciiFont
import asciiPanel.AsciiPanel
import java.awt.*
import java.awt.event.KeyEvent
import java.lang.Exception
import javax.swing.JFrame
import javax.swing.JLayeredPane

class MainFrame : JFrame(), KeyEventDispatcher {

    //    private val font = AsciiFont.TAFFER_10x10
    private val font = AsciiFont("Bisasam_16x16.png", 16, 16)

    private val world: AsciiPanel = AsciiPanel(11 * 8, 8 * 8, font)
    private val info: AsciiPanel = AsciiPanel(5 * 8, 9 * 8, font)
    private val log: AsciiPanel = AsciiPanel(11 * 8, 1 * 8, font)

    private val factory = EntityFactory()
    private val game = Game(factory)

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
                    try {
                        game.update()
                    } catch (e: Exception) {
                        info.setCursorPosition(0, info.heightInCharacters - 3)
                        e.message?.forEach {
                            info.write(it, Color.RED)
                            if (info.cursorX == 40) {
                                info.setCursorPosition(0, info.cursorY + 1)
                            }
                        }
                    }
                    info.write("Repaint count = " + count++, 0, 0)
                    if (needRepaint) {
                        repaint()
                        needRepaint = false
                    }
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
        info.write("In game time: " + game.gameTime, 0, 5)
        world.paintImmediately(0, 0, world.width, world.height)
        info.paintImmediately(0, 0, info.width, info.height)
        log.paintImmediately(0, 0, log.width, log.height)
    }

    private var needRepaint = true
    private var state: UiState = UiState.Normal(game)
    override fun dispatchKeyEvent(e: KeyEvent): Boolean {
        needRepaint = true
        info.writeCenter("Last keys", 10, Color.white, Color.darkGray)
        when (e.id) {
            KeyEvent.KEY_TYPED -> {
                info.write("Last typed: ${e.keyChar}", 0, 11)
                state = state.handleChar(e.keyChar)
            }
            KeyEvent.KEY_PRESSED -> {
                info.write("Last pressed: ${e.keyCode}", 0, 12)
                when (e.keyCode) {
                    KeyEvent.VK_ESCAPE -> System.exit(0)
                }
            }
        }
        info.write("Escape code: ${KeyEvent.VK_ESCAPE}", 0, 13)
        return false
    }

    private sealed class UiState(val game: Game) {
        fun getDir(char: Char): Pair<Int, Int>? {
            return when (char) {
                'j' -> Pair(0, +1)
                'k' -> Pair(0, -1)
                'h' -> Pair(-1, 0)
                'l' -> Pair(+1, 0)
                'y' -> Pair(-1, -1)
                'u' -> Pair(+1, -1)
                'n' -> Pair(+1, +1)
                'b' -> Pair(-1, +1)
                '.' -> Pair(0, 0)
                else -> null
            }
        }

        abstract fun handleChar(char: Char): UiState

        class Normal(game: Game) : UiState(game) {
            override fun handleChar(char: Char): UiState {
                val dir = getDir(char)
                if (dir != null) {
                    game.player[ThinkUntilSet::class]?.action = Move(game.player, dir)
                }
                return when (char) {
                    'f' -> Aim(game)
                    else -> this
                }
            }
        }

        class Aim(game: Game) : UiState(game) {
            override fun handleChar(char: Char): UiState {
                val dir = getDir(char)
                if (dir != null) {
                    game.player[ThinkUntilSet::class]?.action = Shoot(game.player, dir)
                    return Normal(game)
                }
                return this
            }
        }
    }
}