import java.io.PrintWriter
import java.io.StringWriter
import javax.swing.JFrame
import javax.swing.JOptionPane
import javax.swing.JOptionPane.*
import javax.swing.JRootPane

fun main(args: Array<String>) {
    try {
        val app = MainFrame()
        app.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        app.isVisible = true
        //TODO: I DONT KNOW WHY, BUT IT DOESN'T WORK WITH OUT THIS ONE
        val pane = JOptionPane("asdf", INFORMATION_MESSAGE, DEFAULT_OPTION, null, null, null)
    } catch (e: Throwable) {
        val errors = StringWriter()
        e.printStackTrace(PrintWriter(errors))
        e.printStackTrace()
        showMessageDialog(null, errors.toString())
    }
}
