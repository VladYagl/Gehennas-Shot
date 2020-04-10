package gehenna

import gehenna.ui.MainFrame
import gehenna.utils.showError
import javax.swing.JFrame
import javax.swing.UIManager

fun main() {
    try {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
        val app = MainFrame()
        app.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        app.isVisible = true
    } catch (e: Throwable) {
        showError(e)
    }

    System.gc()
}
