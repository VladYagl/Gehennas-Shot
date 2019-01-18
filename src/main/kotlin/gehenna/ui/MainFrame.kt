package gehenna.ui

import gehenna.utils.Point
import gehenna.utils.showError
import java.awt.*
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.KeyEvent
import java.io.PrintWriter
import java.io.StringWriter
import javax.swing.*

class MainFrame : JFrame(), UI {
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
        isResizable = false

        size = Dimension(settings.width, settings.height)
        preparePanels()

        world.defaultBackgroundColor = trueDarkGray
        info.defaultBackgroundColor = trueDarkGray
        log.defaultBackgroundColor = trueDarkGray
        contentPane.background = trueDarkGray
        world.clear()
        info.clear()
        log.clear()

        app = App(this, settings)
        addComponentListener(object : ComponentAdapter() {
            override fun componentShown(e: ComponentEvent?) {
                app.start()
            }
        })

        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(InputConverter(app))
    }

    override fun printException(e: Throwable) {
        info.writeLine(e.message ?: "no message", info.heightInCharacters - 3, fg = Color.RED)
        info.writeLine(e.toString(), info.heightInCharacters - 2, fg = Color.RED)
    }

    private fun JComponent.paintImmediately() {
        paintImmediately(0, 0, width, height)
    }

    override fun update() {
        mainPane.paintImmediately()
    }

    override fun updateLog(messages: ArrayList<String>) {
        log.clear()
        messages.takeLast(logHeight).forEachIndexed { index, s ->
            log.write(s, 0, index)
        }
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

    private fun forceRepaint() {
        world.forceRepaint()
        info.forceRepaint()
        log.forceRepaint()
        mainPane.repaint()
    }
}
