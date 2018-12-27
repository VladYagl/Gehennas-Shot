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

operator fun <A : Comparable<C>, B : Comparable<D>, C : Comparable<A>, D : Comparable<B>> Pair<A, B>.compareTo(other: Pair<C, D>): Int {
    val res = first.compareTo(other.first)
    return if (res == 0) {
        second.compareTo(other.second)
    } else {
        res
    }
}

operator fun Position.compareTo(other: Pair<Int, Int>): Int {
    return other.compareTo(x to y)
}

operator fun Position.compareTo(other: Position): Int {
    return other.compareTo(x to y)
}

