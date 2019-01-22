package gehenna.utils

import com.beust.klaxon.JsonReader
import gehenna.utils.Point.Companion.zero
import java.awt.Color
import java.io.PrintWriter
import java.io.StringWriter
import javax.swing.JOptionPane.PLAIN_MESSAGE
import javax.swing.JOptionPane.showMessageDialog
import kotlin.random.Random

operator fun Color.times(alpha: Double) = Color((red * alpha).toInt(), (green * alpha).toInt(), (blue * alpha).toInt())

fun showError(e: Throwable) {
    val errors = StringWriter()
    e.printStackTrace(PrintWriter(errors))
    e.printStackTrace()
    showMessageDialog(null, errors.toString(), "ERROR", PLAIN_MESSAGE)
}

val random = Random.Default
fun Random.nextPoint(size: Size) = nextPoint(0, 0, size.width, size.height)
fun Random.nextPoint(x: Int, y: Int, width: Int, height: Int) = nextInt(x, x + width) at nextInt(y, y + height)
fun Random.next4way(vararg dir: Dir? = emptyArray()): Dir {
    while (true) {
        val rand = Dir[nextInt(4) * 2]
        if (rand !in dir) return rand
    }
}

operator fun <T, S> Iterable<T>.times(other: Iterable<S>): List<Pair<T, S>> {
    return cartesianProduct(other) { first, second -> first to second }
}

fun <T, S, V> Iterable<T>.cartesianProduct(other: Iterable<S>, transformer: (first: T, second: S) -> V): List<V> {
    return flatMap { first -> other.map { second -> transformer.invoke(first, second) } }
}

fun JsonReader.nextStringList() = ArrayList<String>().also { list ->
    beginArray {
        while (hasNext()) {
            list.add(nextString())
        }
    }
}


