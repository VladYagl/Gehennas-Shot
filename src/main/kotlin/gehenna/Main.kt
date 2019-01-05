package gehenna

import gehenna.ui.MainFrame
import gehenna.utils.showError
import kotlinx.coroutines.ObsoleteCoroutinesApi
import javax.swing.JFrame
import javax.swing.UIManager

fun main(args: Array<String>) {
    try {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
        val app = MainFrame()
        app.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        app.isVisible = true
    } catch (e: Throwable) {
        showError(e)
    }
}
