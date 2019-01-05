package gehenna.ui

import asciiPanel.AsciiFont
import asciiPanel.AsciiPanel
import gehenna.loadSettings
import gehenna.streamResource
import gehenna.utils.Point
import gehenna.utils.range
import gehenna.utils.showError
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.awt.*
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.KeyEvent
import java.io.PrintWriter
import java.io.StringWriter
import javax.swing.*

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

class GehennaPanel(width: Int, height: Int, font: AsciiFont) : AsciiPanel(width, height, font), Window {
    override fun writeLine(line: String, y: Int, alignment: Alignment, fg: Color?, bg: Color?) {
        val text = if (line.length < widthInCharacters) line else line.take(widthInCharacters - 3) + "..."
        clearLine(y)
        when (alignment) {
            Alignment.left -> write(text, 0, y, fg ?: defaultForegroundColor,
                    bg ?: defaultBackgroundColor)
            Alignment.center -> writeCenter(text, y, fg ?: defaultForegroundColor,
                    bg ?: defaultBackgroundColor)
            Alignment.right -> TODO()
        }
    }

    override fun putChar(char: Char, x: Int, y: Int, fg: Color?, bg: Color?) {
        write(char, x, y, fg ?: defaultForegroundColor, bg ?: defaultBackgroundColor)
    }

    fun forceRepaint() {
        withEachTile { _, _, data ->
            data.foregroundColor = Color(data.foregroundColor.rgb - 1)
        }
        repaint()
    }
}

class MainFrame : JFrame(), UI, KeyEventDispatcher {
    private val settings = loadSettings(streamResource("data/settings.json"))!!
    private val worldFont = settings.worldFont
    private val font = settings.font
    private val trueDarkGray = settings.backgroundColor

    private val mainPane = JLayeredPane()
    override lateinit var world: GehennaPanel
    override lateinit var info: GehennaPanel
    private lateinit var log: GehennaPanel
    private val logHeight = settings.logHeight
    private val infoWidth = settings.infoWidth
    override val worldWidth: Int get() = world.widthInCharacters
    override val worldHeight: Int get() = world.heightInCharacters
    private val app: App

    private fun preparePanels() {
        val logWidth = width - (infoWidth + 1) * font.width
        val worldHeight = height - (logHeight + 1) * font.height
        log = GehennaPanel(logWidth / font.width, logHeight, font)
        info = GehennaPanel(infoWidth, height / font.height, font)
        world = GehennaPanel(logWidth / worldFont.width, worldHeight / worldFont.height, worldFont)

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

        app = App(this, settings) // TODO move this out of main frame
        addComponentListener(object : ComponentAdapter() {
            override fun componentShown(e: ComponentEvent?) {
                GlobalScope.launch {
                    app.mainLoop()
                }
            }
        })
    }

    override fun printException(e: Throwable) {
        val errors = StringWriter()
        e.printStackTrace(PrintWriter(errors))
        info.clear(' ', 0, info.heightInCharacters - 10, info.widthInCharacters, 10)
        info.writeText(errors.toString(), 0, info.heightInCharacters - 10, Color.RED)
    }

    private fun JComponent.paintImmediately() {
        paintImmediately(0, 0, width, height)
    }

    override fun update() {
        world.repaint()
        info.repaint()
        log.repaint()
        mainPane.repaint()
    }

    override fun updateLog(messages: ArrayList<String>) {
        log.clear()
        messages.takeLast(logHeight).forEachIndexed { index, s ->
            log.write(s, 0, index)
        }
    }

    override fun endGame() {
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
        range(message.widthInCharacters, message.heightInCharacters).forEach { (x, y) ->
            if (x == 0 || x == message.widthInCharacters - 1 || y == 0 || y == message.heightInCharacters - 1) {
                message.write(254.toChar(), x, y)
            }
        }
        message.writeCenter("You are dead", 2)
        message.writeCenter("RIP", 4)
    }

    override fun newWindow(width: Int, height: Int): Window {
        val x = (mainPane.width - width * font.width) / 2
        val y = (mainPane.height - height * font.height) / 2
        val window = GehennaPanel(width, height, font)
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
        window.defaultBackgroundColor = mainPane.background
        panel.background = mainPane.background
        window.clear()
        return window
    }

    override fun removeWindow(window: Window) {
        if (window is GehennaPanel) {
            mainPane.remove(window.parent)
            mainPane.repaint()
        } else {
            throw Exception("MainFrame can't remove $window")
        }
    }

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
        try {
            info.writeCenter("Last keys", 20, Color.white, Color.darkGray)
            when (e.id) {
                KeyEvent.KEY_TYPED -> {
                    app.onInput(Input.Char(e.keyChar))
                    getDir(e.keyChar)?.let { dir -> app.onInput(Input.Direction(dir)) }
                    when (e.keyChar) {
                        'Q' -> System.exit(0)
                        'r' -> forceRepaint()
                        'f' -> app.onInput(Input.Fire)
                        ',', 'g' -> app.onInput(Input.Pickup)
                        'd' -> app.onInput(Input.Drop)
                        '>', '<' -> app.onInput(Input.ClimbStairs)
                    }
                    info.writeLine("Last typed: ${e.keyChar}", 21)
                }
                KeyEvent.KEY_PRESSED -> {
                    info.writeLine("Last pressed: ${e.keyCode}", 22)
                    when (e.keyCode) {
                        KeyEvent.VK_ESCAPE, KeyEvent.VK_CAPS_LOCK -> app.onInput(Input.Cancel)
                        KeyEvent.VK_ENTER, KeyEvent.VK_SPACE -> app.onInput(Input.Accept)
                    }
                }
            }
        } catch (e: Throwable) {
            showError(e)
            printException(e)
        }
        return false
    }

    private fun forceRepaint() {
        world.forceRepaint()
        info.forceRepaint()
        log.forceRepaint()
        mainPane.repaint()
    }
}
