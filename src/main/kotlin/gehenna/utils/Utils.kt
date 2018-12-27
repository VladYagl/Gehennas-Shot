package gehenna.utils

import gehenna.components.Position
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

operator fun Pair<Int, Int>.minus(other: Pair<Int, Int>): Pair<Int, Int> {
    return Pair(first - other.first, second - other.second)
}

operator fun Pair<Int, Int>.plus(other: Pair<Int, Int>): Pair<Int, Int> {
    return Pair(first + other.first, second + other.second)
}

val Pair<Int, Int>.x: Int get() = first
val Pair<Int, Int>.y: Int get() = second

