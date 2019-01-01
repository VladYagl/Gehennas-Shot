package gehenna.ui

import asciiPanel.AsciiPanel
import gehenna.*
import gehenna.components.*
import gehenna.level.DungeonLevel
import gehenna.utils.*
import gehenna.utils.Point
import java.awt.*
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.KeyEvent
import java.io.InputStream
import java.io.PrintWriter
import java.io.StringWriter
import javax.swing.BoxLayout
import javax.swing.JFrame
import javax.swing.JLayeredPane
import javax.swing.JSeparator

class MainFrame : JFrame(), KeyEventDispatcher {

    private val settings = loadSettings(streamResource("data/settings.json"))!!
    private val worldFont = settings.worldFont
    private val font = settings.font
    private val trueDarkGray = settings.backgroundColor

    private val mainPane = JLayeredPane()
    private lateinit var world: AsciiPanel
    private lateinit var info: AsciiPanel
    private lateinit var log: AsciiPanel
    private val logHeight = settings.logHeight
    private val infoWidth = settings.infoWidth

    private val factory = EntityFactory()
    private val game = Game(factory)
    private var needRepaint = true
    private var stop = false

    private fun preparePanels() {
        val logWidth = width - (infoWidth + 1) * font.width
        val worldHeight = height - (logHeight + 1) * font.height
        log = AsciiPanel(logWidth / font.width, logHeight, font)
        info = AsciiPanel(infoWidth, height / font.height, font)
        world = AsciiPanel(logWidth / worldFont.width, worldHeight / worldFont.height, worldFont)

        add(mainPane)
        mainPane.layout = null
        val verticalPane = JLayeredPane()
        val horizontalPane = JLayeredPane()
        verticalPane.layout = BoxLayout(verticalPane, BoxLayout.PAGE_AXIS)
        horizontalPane.layout = BoxLayout(horizontalPane, BoxLayout.LINE_AXIS)
        horizontalPane.add(verticalPane)
        verticalPane.add(log)
        verticalPane.add(JSeparator())
        verticalPane.add(world)
        horizontalPane.add(JSeparator(JSeparator.VERTICAL))
        horizontalPane.add(info)
        horizontalPane.size = horizontalPane.preferredSize
        horizontalPane.location = java.awt.Point(0, 0)
        mainPane.add(horizontalPane)
        mainPane.preferredSize = horizontalPane.preferredSize
        pack()
    }

    init {
        title = "Gehenna's Shot"
        //isResizable = false
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(this)

        size = Dimension(settings.width, settings.height)
        preparePanels()

        world.defaultBackgroundColor = trueDarkGray
        info.defaultBackgroundColor = trueDarkGray
        log.defaultBackgroundColor = trueDarkGray
        contentPane.background = trueDarkGray
        world.clear()
        info.clear()
        log.clear()

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

    private fun viewPoint(x: Int, y: Int): Point {
        return (x to y) - camera
    }

    private fun levelPos(x: Int, y: Int): Point {
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
    private var time = 0L

    private fun mainLoop() {
        try {
            var count = 0
            var repaintCount = 0
            while (true) {
                game.update()
                needRepaint = needRepaint || game.time > time

                if (!game.player.has(Position::class)) {
                    endGame()
                    return
                }

                count++
                if (needRepaint) {
                    time = game.time
                    info.write("Paint=${repaintCount++} loop=$count", 0, 0)
                    update()
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

        world.repaint()
        info.repaint()
        log.repaint()
        mainPane.repaint()
        needRepaint = false
    }

    private fun updateLog() {
        log.clear()
        val messages = game.player[Logger::class]?.log?.takeLast(log.heightInCharacters)
        messages?.forEachIndexed { index, s ->
            log.write(s, 0, index)
        }
    }

    private var camera = 0 to 0
    private val cameraBound = world.widthInCharacters / 2 - 5 to world.heightInCharacters / 2 - 5
    private fun moveCamera(playerPos: Point) {
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
        //TODO: Try drawing whole level and then moving it
        //TODO: Animations
//        world.clear()
        priority.forEach { it.fill(-2) }
        val playerPos = game.player[Position::class]!!
        val level = playerPos.level
        moveCamera(playerPos.point)
        level.visitVisibleGlyphs(playerPos.x, playerPos.y) { glyph, x, y -> writeGlyph(glyph, x, y) }

        for (x in 0 until world.widthInCharacters) {
            for (y in 0 until world.heightInCharacters) {
                val pos = levelPos(x, y)
                if (priority[x, y] == -2 && pos.x < level.width && pos.y < level.height && pos.x >= 0 && pos.y >= 0) {
                    level.memory(pos.x, pos.y)?.let {
                        writeGlyph(it, pos.x, pos.y, Color(96, 32, 32))
                    } ?: writeGlyph(Glyph(game.player, ' ', -1), pos.x, pos.y)
                } else {
                    writeGlyph(Glyph(game.player, ' ', -1), pos.x, pos.y)
                }
            }
        }
    }

    private fun updateInfo() {
        info.clear(' ', 0, 1, info.widthInCharacters, 19)
        info.write("In game time: " + game.time, 0, 1)
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
        info.writeText("Window size: ${size.width}x${size.height}", 0, 40)
        info.writeText("world size: ${world.width}x${world.height}", 0, 41)
        info.writeText("info size: ${info.width}x${info.height}", 0, 42)
        info.writeText("log size: ${log.width}x${log.height}", 0, 43)
    }

    private fun predict() {
        val playerPos = game.player[Position::class]!!
        val stats = game.player[Stats::class]!!
        val level = playerPos.level
        val behaviours = ArrayList<PredictableBehaviour>()
        level.visitFov(playerPos.x, playerPos.y) { entity, _, _ ->
            entity.all(PredictableBehaviour::class).firstOrNull()?.let { behaviours.add(it) }
        }
        behaviours.forEach {
            val glyph = it.entity[Glyph::class]!!
            var color = world.defaultForegroundColor * 0.5
            val prediction = level.predict(it, stats.speed.toLong())
            prediction.forEach { (x, y) ->
                if (level.isVisible(x, y) && inView(x, y)) {
                    color *= 0.85
                    writeGlyph(glyph, x, y, color)
                }
            }

        }
    }

    private fun endGame() {
        //TODO: move it inside manager???
        val message = AsciiPanel(2 * 8, 1 * 8, font)
        mainPane.add(message)
        mainPane.moveToFront(message)
        message.size = message.preferredSize
        message.location = java.awt.Point(
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
        state = End(context)
        message.writeCenter("You are dead", 2)
        message.writeCenter("RIP", 4)
    }

    private val context = UiContext(game, mainPane, font)
    private var state: UiState = Normal(context)
    override fun dispatchKeyEvent(e: KeyEvent): Boolean {
        try {
            needRepaint = true
            info.writeCenter("Last keys", 20, Color.white, Color.darkGray)
            when (e.id) {
                KeyEvent.KEY_TYPED -> {
                    when (e.keyChar) {
                        'Q' -> System.exit(0)
                    }
                    info.writeLine("Last typed: ${e.keyChar}", 21)
                    state = state.handleChar(e.keyChar)
                }
                KeyEvent.KEY_PRESSED -> {
                    info.writeLine("Last pressed: ${e.keyCode}", 22)
                    when (e.keyCode) {
                        //KeyEvent.VK_ESCAPE -> System.exit(0)
                    }
                    state = state.handleKey(e.keyCode)
                }
            }
            info.write("Escape code: ${KeyEvent.VK_ESCAPE}", 0, 23)
        } catch (e: Throwable) {
            showError(e)
            printException(e)
        }
        return false
    }
}
