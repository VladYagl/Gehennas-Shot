package gehenna.ui

import gehenna.ui.panel.GehennaPanel
import gehenna.ui.panel.MenuPanel
import gehenna.utils.Point
import gehenna.utils.Size
import gehenna.utils.showError
import kotlinx.coroutines.*
import java.awt.*
import java.awt.event.KeyEvent
import javax.swing.*
import kotlin.system.exitProcess

class MainFrame : JFrame(), UI, KeyEventDispatcher {
    override val settings = loadSettings(streamResource("data/settings.json"))!!
    private val worldFont = settings.worldFont
    private val font = settings.font

    private val mainPane = JLayeredPane()
    override lateinit var world: GehennaPanel
    override lateinit var info: GehennaPanel
    private lateinit var log: GehennaPanel
    private val logHeight = settings.logHeight
    private val infoWidth = settings.infoWidth
    override val worldSize: Size get() = world.size

    private val keyEventHandlers = ArrayList<InputConverter>()
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

        val menuWindow = MenuPanel(12, 4, settings)
        menuWindow.addItem(ButtonItem("Continue", {
            // todo: add this only if save file is present
            startGame(true)
            removeWindow(menuWindow)
        }, 'c'))
        menuWindow.addItem(ButtonItem("New Game", {
            startGame(false)
            removeWindow(menuWindow)
        }, 'n'))
        menuWindow.addItem(ButtonItem("Quit", {
            exitProcess(0)
        }, 'q'))
        addWindow(menuWindow)

        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(this)
    }

    private fun startGame(load: Boolean) {
        app.start(load)
        keyEventHandlers.add(GameInput(app))
    }

    override fun dispatchKeyEvent(e: KeyEvent?): Boolean {
        return e?.let { event -> keyEventHandlers.lastOrNull()?.dispatchKeyEvent(event) } ?: false
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

    override fun addWindow(window: Window) {
        val x = (mainPane.width - window.size.width * font.width) / 2
        val y = (mainPane.height - window.size.height * font.height) / 2
        window.panel.location = java.awt.Point(x, y)

        mainPane.add(window.panel)
        mainPane.moveToFront(window.panel)
        window.keyHandler?.let { keyEventHandlers.add(it) }
    }

    override suspend fun <T> loadingWindow(text: String, task: () -> T): T {
        return withContext(Dispatchers.Default) {
            val window = GehennaPanel(text.length + 3, 2, settings)
            addWindow(window)
            window.writeLine(text, 0)
            val job = async { task() }
            launch {
                var count = 0
                while (!job.isCompleted) {
                    window.putChar(listOf('-', '\\', '|', '/')[count++ % 3], text.length + 1, 0)
                    window.forceRepaint()
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
            window.keyHandler?.let { keyEventHandlers.remove(it) }
            mainPane.repaint()
        } else {
            throw Exception("MainFrame can't remove $window")
        }
    }

    @Deprecated("BULLSHIT")
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
