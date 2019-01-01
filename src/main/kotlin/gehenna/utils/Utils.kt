package gehenna.utils

import gehenna.components.Position
import java.awt.Color
import java.io.PrintWriter
import java.io.StringWriter
import javax.swing.JOptionPane.PLAIN_MESSAGE
import javax.swing.JOptionPane.showMessageDialog
import kotlin.concurrent.fixedRateTimer
import kotlin.math.sign
import kotlin.random.Random

operator fun Color.times(alpha: Double): Color {
    return Color((red * alpha).toInt(), (green * alpha).toInt(), (blue * alpha).toInt())
}

fun showError(e: Throwable) {
    val errors = StringWriter()
    e.printStackTrace(PrintWriter(errors))
    e.printStackTrace()
    showMessageDialog(null, errors.toString(), "ERROR", PLAIN_MESSAGE)
}

typealias Point = Pair<Int, Int>

operator fun Point.minus(other: Point): Point {
    return Pair(first - other.first, second - other.second)
}

operator fun Point.plus(other: Point): Point {
    return Pair(first + other.first, second + other.second)
}

val Point.x: Int get() = first
val Point.y: Int get() = second

val Point.dir: Point get() = x.sign to y.sign

val random = Random.Default
val directions = listOf(
        -1 to -1,
        -1 to 0,
        -1 to 1,
        0 to 1,
        0 to -1,
        1 to -1,
        1 to 1
)

