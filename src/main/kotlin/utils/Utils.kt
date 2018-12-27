package utils

import java.awt.Color
import java.io.PrintWriter
import java.io.StringWriter
import javax.swing.JOptionPane.PLAIN_MESSAGE
import javax.swing.JOptionPane.showMessageDialog

operator fun Color.times(alpha: Double): Color {
    return Color((red * alpha).toInt(), (green * alpha).toInt(), (blue * alpha).toInt())
}

fun showError(e: Throwable) {
    val errors = StringWriter()
    e.printStackTrace(PrintWriter(errors))
    e.printStackTrace()
    showMessageDialog(null, errors.toString(), "ERROR", PLAIN_MESSAGE)
}
