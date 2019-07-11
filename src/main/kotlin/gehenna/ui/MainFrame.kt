package gehenna.ui

import gehenna.utils.Point
import gehenna.utils.Size
import gehenna.utils.showError
import kotlinx.coroutines.*
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.KeyboardFocusManager
import javax.swing.*

class MainFrame : JFrame(), UI {
    private val settings = loadSettings(streamResource("data/settings.json"))!!
    private val worldFont = settings.worldFont
    private val font = settings.font

    private val mainPane = JLayeredPane()
    override lateinit var world: GehennaPanel
    override lateinit var info: GehennaPanel
    private lateinit var log: GehennaPanel
    private val logHeight = settings.logHeight
    private val infoWidth = settings.infoWidth
    override val worldSize: Size get() = world.size
    private val app: App

    private fun preparePanels() {
        val logWidth = width - (infoWidth + 1) * font.width
        val worldHeight = height - (logHeight + 1) * font.height
        log = GehennaPanel(
                logWidth / font.width,
                logHeight,
                settings.font,
                settings.foregroundColor,
                settings.backgroundColor
        )
        info = GehennaPanel(
                infoWidth,
                height / font.height,
                settings.font,
                settings.foregroundColor,
                settings.backgroundColor
        )
        world = GehennaPanel(
                logWidth / worldFont.width,
                worldHeight / worldFont.height,
                settings.worldFont,
                settings.foregroundColor,
                settings.backgroundColor
        )

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
        horizontalPane.border = BorderFactory.createEmptyBorder(5, 5, 0, 0)
        mainPane.add(horizontalPane)
        mainPane.preferredSize = horizontalPane.preferredSize
        pack()
    }

    init {
        println("Creating Main Frame...")
        title = "Gehenna's Shot"
        isResizable = false

        size = Dimension(settings.width, settings.height)
        preparePanels()

        contentPane.background = settings.backgroundColor
        world.clear()
        info.clear()
        log.clear()

        println("Creating App...")
        app = App(this, settings)


        val menuWindow = newWindow(15, 3)
        menuWindow.writeLine("n: New game", 0)
        menuWindow.writeLine("l: Load game", 1)

        var inputConverter: InputConverter? = null
        val listener = object : InputListener {
            override fun onInput(input: Input) = when (input) {
                is Input.Char -> {
                    if (input.char == 'n' || input.char == 'l') {
                        println("You pressed \'N\'")
                        KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(inputConverter)
                        removeWindow(menuWindow)
                        println("Starting game...")
                        startGame(input.char == 'l')
                    }
                    true
                }
                else -> false
            }
        }
        inputConverter = TextInput(listener)
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(inputConverter)
    }

    private fun startGame(load: Boolean) {
        app.start(load)
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(GameInput(app))
    }

    override fun printException(e: Throwable) {
        showError(e) // todo: make this switchable
        info.writeLine(e.message ?: "no message", info.heightInCharacters - 3, fg = Color.RED)
        info.writeLine(e.toString(), info.heightInCharacters - 2, fg = Color.RED)
    }

    private fun JComponent.paintImmediately() {
        paintImmediately(0, 0, width, height)
    }

    override fun update() {
        mainPane.paintImmediately()
    }

    override fun updateLog(messages: List<String>) {
        log.clear()
        messages.takeLast(logHeight).forEachIndexed { index, s ->
            log.writeLine(s, index)
        }
    }

    override fun newWindow(size: Size): Window = newWindow(size.width, size.height)

    override fun newWindow(width: Int, height: Int): Window {
        val x = (mainPane.width - width * font.width) / 2
        val y = (mainPane.height - height * font.height) / 2
        val window = GehennaPanel(width, height, settings.font, settings.foregroundColor, settings.backgroundColor)
        val panel = JPanel()
        panel.layout = BorderLayout()
        val empty = BorderFactory.createEmptyBorder(5, 5, 5, 5)
        val line = BorderFactory.createLineBorder(Color.WHITE, 1)
        panel.border = BorderFactory.createCompoundBorder(line, empty)
        panel.size = window.preferredSize
        panel.location = java.awt.Point(x, y)
        panel.add(window, BorderLayout.CENTER)
        mainPane.add(panel)
        mainPane.moveToFront(panel)
        panel.background = mainPane.background
        window.clear()
        return window
    }

    override suspend fun <T> loadingWindow(text: String, task: () -> T): T {
        return withContext(Dispatchers.Default) {
            val window = newWindow(text.length + 3, 2)
            window.writeLine(text, 0)
            val job = async { task() }
            launch {
                var count = 0
                while (!job.isCompleted) {
                    window.putChar(listOf('-', '\\', '|', '/')[count++ % 3], text.length + 1, 0)
                    window.repaint()
                    delay(100)
                }
            }
            val result = job.await()
            removeWindow(window)
            result
        }
    }

    override fun removeWindow(window: Window) {
        if (window is GehennaPanel) {
            mainPane.remove(window.parent)
            mainPane.repaint()
        } else {
            throw Exception("MainFrame can't remove $window")
        }
    }

    private fun forceRepaint() {
        world.forceRepaint()
        info.forceRepaint()
        log.forceRepaint()
        mainPane.repaint()
    }

    override fun showCursor() = app.showCursor()
    override fun hideCursor() = app.hideCursor()
    override fun setCursor(point: Point) = app.setCursor(point)
}
