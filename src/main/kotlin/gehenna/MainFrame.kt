package gehenna

import asciiPanel.AsciiFont
import asciiPanel.AsciiPanel
import gehenna.components.*
import gehenna.utils.*
import java.awt.*
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.KeyEvent
import java.io.InputStream
import java.io.PrintWriter
import java.io.StringWriter
import javax.swing.JFrame
import javax.swing.JLayeredPane

class MainFrame : JFrame(), KeyEventDispatcher {

    //        private val font = AsciiFont.TAFFER_10x10
    //TODO: FONT settings in json
    private val worldFont = AsciiFont("tilesets/Bisasam_16x16.png", 16, 16)
    private val font = AsciiFont("tilesets/Dullard_Exponent_12x12.png", 12, 12)
    private val trueDarkGray = Color(32, 32, 32)

    private val world: AsciiPanel = AsciiPanel(8 * 6 + 1, 5 * 6 + 1, worldFont)
    private val info: AsciiPanel = AsciiPanel(5 * 6, 8 * 6, font)
    private val log: AsciiPanel = AsciiPanel(11 * 6, 1 * 6, font)

    private val factory = EntityFactory()
    private val game = Game(factory)
    private var stop = false

    private val mainPane = JLayeredPane()

    private var camera = 0 to 0

    init {
        title = "Gehenna's Shot"
        isResizable = false
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(this)

        log.size = log.preferredSize
        world.size = world.preferredSize
        info.size = info.preferredSize

        log.location = Point(0, 0)
        world.location = Point(0, log.height + 1)
        info.location = Point(log.width + 1, 0)

        add(mainPane)
        mainPane.layout = null
        mainPane.add(log)
        mainPane.add(world)
        mainPane.add(info)

        world.defaultBackgroundColor = trueDarkGray
        info.defaultBackgroundColor = trueDarkGray
        log.defaultBackgroundColor = trueDarkGray

        world.clear()
        info.clear()
        log.clear()

        mainPane.size = Dimension(log.width + info.width, info.height + 39)
        size = mainPane.preferredSize

        factory.loadJson(streamResource("data/entities.json"))
        factory.loadJson(streamResource("data/items.json"))
        game.init()

        game.player[Logger::class]?.add("Welcome to Gehenna's Shot")
        game.player[Logger::class]?.add("   Suffer bitch,   love you " + 3.toChar())
        addComponentListener(object : ComponentAdapter() {
            override fun componentShown(e: ComponentEvent?) {
                Thread { mainLoop() }.start()
            }
        })

    }

    private fun streamResource(name: String): InputStream {
        return (Thread::currentThread)().contextClassLoader.getResourceAsStream(name)!!
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

    private fun inView(x: Int, y: Int): Boolean {
        return camera.x <= x && camera.y <= y && camera.x + world.widthInCharacters > x && camera.y + world.heightInCharacters > y
    }

    private fun viewPoint(x: Int, y: Int): Pair<Int, Int> {
        return (x to y) - camera
    }

    private fun levelPos(x: Int, y: Int): Pair<Int, Int> {
        return (x to y) + camera
    }

    private val priority = Array(11 * 8) { Array(8 * 8) { -2 } }
    private fun writeGlyph(glyph: Glyph, x: Int, y: Int, color: Color = world.defaultForegroundColor) {
        if (inView(x, y)) {
            val p = viewPoint(x, y)
            if (glyph.priority > priority[p.x, p.y]) {
                world.write(glyph.char, p.x, p.y, color)
                priority[p.x, p.y] = glyph.priority
            }
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
                    }
                }
            }
        } catch (e: Throwable) {
            showError(e)
            printException(e)
        }
    }

    private fun update() {
        updateLog()
        drawWorld()
        predict()
        updateInfo()

        world.paintImmediately(0, 0, world.width, world.height)
        info.paintImmediately(0, 0, info.width, info.height)
        log.paintImmediately(0, 0, log.width, log.height)
        needRepaint = false
    }

    private fun updateLog() {
        log.clear()
        val messages = game.player[Logger::class]?.log?.takeLast(log.heightInCharacters)
        messages?.forEachIndexed { index, s ->
            log.write(s, 0, index)
        }
    }

    private val cameraBound = world.widthInCharacters / 2 - 5 to world.heightInCharacters / 2 - 5
    private fun moveCamera(playerPos: Pair<Int, Int>) {
        var x = camera.x
        var y = camera.y
        if (playerPos.x < camera.x + cameraBound.x) {
            x = playerPos.x - cameraBound.x
        }
        if (playerPos.y < camera.y + cameraBound.y) {
            y = playerPos.y - cameraBound.y
        }
        val end = camera.x + world.widthInCharacters to camera.y + world.heightInCharacters
        if (playerPos.x > end.x - cameraBound.x) {
            x = playerPos.x + cameraBound.x - world.widthInCharacters
        }
        if (playerPos.y > end.y - cameraBound.y) {
            y = playerPos.y + cameraBound.y - world.heightInCharacters
        }

        camera = x to y
    }

    private fun drawWorld() {
        world.clear()
        priority.forEach { it.fill(-2) }
        val playerPos = game.player[Position::class]!!
        val level = playerPos.level
        moveCamera(playerPos.point)
        level.visitFOV(playerPos.x, playerPos.y) { glyph, x, y -> writeGlyph(glyph, x, y) }

        for (x in 0 until world.widthInCharacters) {
            for (y in 0 until world.heightInCharacters) {
                val pos = levelPos(x, y)
                if (priority[x, y] == -2 && pos.x < level.width && pos.y < level.height && pos.x >= 0 && pos.y >= 0) {
                    level.memory(pos.x, pos.y)?.let {
                        writeGlyph(it, pos.x, pos.y, Color(96, 32, 32))
                    }
                }
            }
        }
    }

    private fun updateInfo() {
        info.clear(' ', 0, 1, info.widthInCharacters, 19)
        info.write("In game time: " + game.gameTime, 0, 1)
        val glyph = game.player[Glyph::class]!!
        val pos = game.player[Position::class]!!
        val storage = game.player[Inventory::class]!!
        info.write("Player glyph = ${glyph.char}|${glyph.priority}", 0, 2)
        info.write("Player pos = ${pos.x}, ${pos.y}", 0, 3)
        info.writeLine("Player hp = " + game.player[Health::class]?.current, 4)
        info.writeText("Effects = " + game.player.all(Effect::class), 0, 5)
        info.writeCenter("Inventory", 8, Color.white, Color.darkGray)
        storage.all().forEachIndexed { index, item ->
            info.write(item.entity.toString(), 0, 9 + index)
        }

        if (pos.level is DungeonLevel) {
            info.writeText("Level: " + pos.level.depth, 0, 19)
        }

        info.clear(' ', 0, 30, info.widthInCharacters, info.heightInCharacters - 30)
        info.writeCenter("Objects", 30, Color.white, Color.darkGray)
        pos.neighbors.forEachIndexed { index, entity ->
            info.writeText(entity.toString(), 0, 31 + index)
        }
    }

    private fun predict() {
        //TODO : PREDICT RELATIVE TO PLAYER SPEED
        for (entity in ComponentManager[BulletBehaviour::class, Glyph::class, Position::class]) {
            val realPos = entity[Position::class]!!
            if (realPos.level != game.player[Position::class]!!.level) continue
            if (!inView(realPos.x, realPos.y)) continue
            val fakeEntity = Entity("Stub")
            val behaviour = entity[BulletBehaviour::class]!!.copy(entity = fakeEntity)
            // FIXME: Now it actually moves this fake entity through real level, and also adds it to component manager
            // FIXME: Though I'm not performing it's actions, so in theory it can't break anything
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
                            writeGlyph(glyph, x, y, color)
                            priority[x, y] = glyph.priority
                        }
                    }
                    time += action.time * 100 / speed // FIXME : COPYPASTA!!!
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
}
