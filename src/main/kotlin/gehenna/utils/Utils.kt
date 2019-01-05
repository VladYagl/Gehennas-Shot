package gehenna.utils

import com.beust.klaxon.JsonReader
import java.awt.Color
import java.io.PrintWriter
import java.io.StringWriter
import javax.swing.JOptionPane.PLAIN_MESSAGE
import javax.swing.JOptionPane.showMessageDialog
import kotlin.math.sign
import kotlin.random.Random

operator fun Color.times(alpha: Double) = Color((red * alpha).toInt(), (green * alpha).toInt(), (blue * alpha).toInt())

fun showError(e: Throwable) {
    val errors = StringWriter()
    e.printStackTrace(PrintWriter(errors))
    e.printStackTrace()
    showMessageDialog(null, errors.toString(), "ERROR", PLAIN_MESSAGE)
}

typealias Point = Pair<Int, Int>

operator fun Point.minus(other: Point) = Point(first - other.first, second - other.second)

operator fun Point.plus(other: Point) = Point(first + other.first, second + other.second)

val Point.x: Int get() = first
val Point.y: Int get() = second

val Point.dir: Point get() = x.sign to y.sign

val random = Random.Default
val directions = listOf(
        1 to 0,
        1 to 1,
        0 to 1,
        -1 to 1,
        -1 to 0,
        -1 to -1,
        0 to -1,
        1 to -1
)

fun turnLeft(dir: Point) = directions[(directions.indexOf(dir) + 1) % directions.size]

fun turnRight(dir: Point) = directions[(directions.indexOf(dir) + directions.size - 1) % directions.size]

operator fun <T, S> Iterable<T>.times(other: Iterable<S>): List<Pair<T, S>> {
    return cartesianProduct(other) { first, second -> first to second }
}

fun <T, S, V> Iterable<T>.cartesianProduct(other: Iterable<S>, transformer: (first: T, second: S) -> V): List<V> {
    return flatMap { first -> other.map { second -> transformer.invoke(first, second) } }
}

infix fun Point.until(to: Point): List<Point> {
    return (x until to.x) * (y until to.y)
}

operator fun Point.rangeTo(to: Point): List<Point> {
    return (x..to.x) * (y..to.y)
}

fun range(end: Point): List<Point> {
    return (0 to 0) until end
}

fun range(x: Int, y: Int): List<Point> {
    return (0 to 0) until (x to y)
}

fun JsonReader.nextStringList() = ArrayList<String>().also { list ->
    beginArray {
        while (hasNext()) {
            list.add(nextString())
        }
    }
}


