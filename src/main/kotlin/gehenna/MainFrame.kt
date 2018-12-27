package gehenna

import asciiPanel.AsciiFont
import asciiPanel.AsciiPanel
import gehenna.components.*
import gehenna.utils.*
import java.awt.*
import java.awt.event.KeyEvent
import java.io.PrintWriter
import javax.swing.JFrame
import javax.swing.JLayeredPane
import java.io.StringWriter


class MainFrame : JFrame(), KeyEventDispatcher {

    //        private val font = AsciiFont.TAFFER_10x10
    private val font = AsciiFont("Bisasam_16x16.png", 16, 16)
    private val trueDarkGray = Color(32, 32, 32)

    private val world: AsciiPanel = AsciiPanel(11 * 8, 8 * 8, font)
    private val info: AsciiPanel = AsciiPanel(5 * 8, 9 * 8, font)
    private val log: AsciiPanel = AsciiPanel(11 * 8, 1 * 8, font)

    private val factory = EntityFactory()
    private val game = Game(factory)
    private var stop = false

    private val mainPane = JLayeredPane()

    init {
        title = "Gehenna's Shot"
        isResizable = false
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(this)

        add(mainPane)
        mainPane.layout = null

        mainPane.add(log)
        mainPane.add(world)
        mainPane.add(info)

        log.size = log.preferredSize
        world.size = world.preferredSize
        info.size = info.preferredSize

        log.location = Point(0, 0)
        world.location = Point(0, log.height + 1)
        info.location = Point(log.width + 1, 0)

        world.defaultBackgroundColor = trueDarkGray
        info.defaultBackgroundColor = trueDarkGray
        log.defaultBackgroundColor = trueDarkGray

        world.clear()
        info.clear()
        log.clear()
        world.repaint()
        info.repaint()
        log.repaint()

        mainPane.size = Dimension(log.width + info.width, info.height + 39)
        size = mainPane.preferredSize

        game.player[Logger::class]?.add("Welcome to Gehenna's Shot")
        game.player[Logger::class]?.add("   Suffer bitch,   love you " + 3.toChar())
        Thread { mainLoop() }.start()
    }

    private fun printException(e: Throwable) {
        val errors = StringWriter()
        e.printStackTrace(PrintWriter(errors))
        info.clear(' ', 0, info.heightInCharacters - 10, info.widthInCharacters, 10)
        info.writeText(errors.toString(), 0, info.heightInCharacters - 10, Color.RED)
        stop = true
    }

    private fun AsciiPanel.clearLine(y: Int) {
        clear(' ', 0, y, widthInCharacters, 1)
    }

    private fun AsciiPanel.writeLine(line: String, y: Int) {
        clearLine(y)
        write(line, 0, y)
    }

    private fun AsciiPanel.writeText(text: String, x: Int, y: Int, color: Color = this.defaultForegroundColor) {
        setCursorPosition(x, y)
        text.forEach {
            if (it == '\n' || cursorX >= widthInCharacters) {
                if (cursorY >= heightInCharacters - 1) return
                setCursorPosition(0, cursorY + 1)
            }
            when (it) {
                '\n', '\r' -> {
                }
                '\t' -> write("   ", color)
                else -> write(it, color)
            }
        }
    }

    private fun writeGlyph(glyph: Glyph, x: Int, y: Int, color: Color = world.defaultForegroundColor) {
        if (glyph.priority >= priority[x, y]) {
            world.write(glyph.char, x, y, color)
            priority[x, y] = glyph.priority
        }
    }

    /** this shit runs in separate thread! */
    private fun mainLoop() {
        try {
            var count = 0
            while (true) {
                if (this.isValid) {
                    game.update()

                    if (!game.player.has(Position::class)) {
                        endGame()
                        return
                    }

                    info.write("Repaint count = " + count++, 0, 0)
                    if (needRepaint) {
                        update()
                        needRepaint = false
                    }
                }
            }
        } catch (e: Throwable) {
            showError(e)
            printException(e)
        }
    }

    private val priority = Array(11 * 8) { Array(8 * 8) { -2 } }
    private fun update() {
        updateLog()
        drawWorld()
        predict()

        info.write("In game time: " + game.gameTime, 0, 12)
        world.paintImmediately(0, 0, world.width, world.height)
        info.paintImmediately(0, 0, info.width, info.height)
        log.paintImmediately(0, 0, log.width, log.height)
    }

    private fun updateLog() {
        log.clear()
        val messages = game.player[Logger::class]?.log?.takeLast(log.heightInCharacters)
        messages?.forEachIndexed { index, s ->
            log.write(s, 0, index)
        }
    }

    private fun drawWorld() {
        world.clear()
        priority.forEach { it.fill(-2) }
        val playerPos = game.player[Position::class]!!
        val level = playerPos.level
        level.updateFOV(playerPos.x, playerPos.y)
        val lowerBound = (playerPos.x - world.widthInCharacters / 2) to (playerPos.y - world.heightInCharacters / 2)
        val upperBound = (playerPos.x + world.widthInCharacters / 2) to (playerPos.y + world.heightInCharacters / 2)
        for (entity in ComponentManager[Glyph::class, Position::class]) {
            val pos = entity[Position::class]!!

            val glyph = entity[Glyph::class]!!

            if (level.isVisible(pos.x, pos.y)) {
                writeGlyph(glyph, pos.x, pos.y)
            } else {
                val mem = level.memory(pos.x, pos.y) ?: Glyph(
                    game.player,
                    ' ',
                    Int.MIN_VALUE
                ) // TODO: It's hack
//                writeGlyph(mem, pos.x, pos.y, world.defaultForegroundColor * 0.25)
                writeGlyph(mem, pos.x, pos.y, Color(96, 32, 32))
            }


            if (entity == game.player) {
                info.write("Player glyph = ${glyph.char}|${glyph.priority}", 0, 1)
                info.write("Player pos = ${pos.x}, ${pos.y}", 0, 2)
                info.write("Player pos priority = ${priority[pos.x, pos.y]}", 0, 3)
                info.writeLine("Player hp = " + entity[Health::class]?.current, 4)
                info.clear(' ', 0, 5, info.widthInCharacters, 4)
                info.writeText("Effects = " + entity.all(Effect::class), 0, 5)
            }
        }
    }

    //TODO : PREDICT RELATIVE TO PLAYER SPEED
    private fun predict() {
        for (entity in ComponentManager[BulletBehaviour::class, Glyph::class, Position::class]) {
            val fakeEntity = Entity("Stub")
            val behaviour = entity[BulletBehaviour::class]!!.copy(entity = fakeEntity)
            val realPos = entity[Position::class]!!
            // TODO: Now it actually moves this fake entity through real level, and also adds it to component manager
            var fakePos = realPos.copy(entity = fakeEntity)
            val glyph = entity[Glyph::class]!!
            val speed = entity[Stats::class]?.speed ?: 100
            var color = world.defaultForegroundColor * 0.5
            fakeEntity.add(fakePos)
            var time = behaviour.time
            if (fakePos.level.isVisible(fakePos.x, fakePos.y))
                while (time < 100) {
                    val action = behaviour.action
                    if (action is Move) {
                        val (x, y) = fakePos + action.dir
                        fakeEntity.remove(fakePos)
                        fakePos = Position(fakeEntity, x, y, fakePos.level)
                        fakeEntity.add(fakePos)
                        if (glyph.priority > priority[x, y] && realPos.level.isVisible(x, y)) {
                            color *= 0.85
                            world.write(glyph.char, x, y, color)
                            priority[x, y] = glyph.priority
                        }
                    }
                    time += action.time * 100 / speed // TODO : COPYPASTA!!!
                }
            fakeEntity.remove(fakePos)
        }
    }

    private fun endGame() {
        val message = AsciiPanel(2 * 8, 1 * 8, font)
        mainPane.add(message)
        mainPane.moveToFront(message)
        message.size = message.preferredSize
        message.location = Point(
            (mainPane.width - message.width) / 2,
            (mainPane.height - message.height) / 2
        )
        message.defaultBackgroundColor = trueDarkGray
        message.clear()
        for (x in 0 until message.widthInCharacters) {
            for (y in 0 until message.heightInCharacters) {
                if (x == 0 || x == message.widthInCharacters - 1 || y == 0 || y == message.heightInCharacters - 1) {
                    message.write(254.toChar(), x, y)
                }
            }
        }
        state = UiState.End(game)
        message.writeCenter("You are dead", 2)
        message.writeCenter("RIP", 4)
    }

    private var needRepaint = true
    private var state: UiState = UiState.Normal(game)
    override fun dispatchKeyEvent(e: KeyEvent): Boolean {
        needRepaint = true
        info.writeCenter("Last keys", 20, Color.white, Color.darkGray)
        when (e.id) {
            KeyEvent.KEY_TYPED -> {
                info.writeLine("Last typed: ${e.keyChar}", 21)
                state = state.handleChar(e.keyChar)
            }
            KeyEvent.KEY_PRESSED -> {
                info.writeLine("Last pressed: ${e.keyCode}", 22)
                when (e.keyCode) {
                    KeyEvent.VK_ESCAPE -> System.exit(0)
                }
            }
        }
        info.write("Escape code: ${KeyEvent.VK_ESCAPE}", 0, 23)
        return false
    }

    private sealed class UiState(val game: Game) {
        fun getDir(char: Char): Pair<Int, Int>? {
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
//                    game.player[gehenna.ThinkUntilSet::class]?.action = gehenna.Shoot(game.player, dir)
                    game.player[ThinkUntilSet::class]?.action =
                            ApplyEffect(
                                game.player,
                                RunAndGun(game.player, dir, 500, time = 10)
                            )
                    return Normal(game)
                }
                return this
            }
        }

        class End(game: Game) : UiState(game) {
            override fun handleChar(char: Char): UiState {
                if (char == ' ') System.exit(0)
                return this
            }
        }
    }
}