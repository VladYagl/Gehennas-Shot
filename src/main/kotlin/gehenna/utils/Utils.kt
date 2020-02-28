package gehenna.utils

import gehenna.exception.GehennaException
import java.awt.Color
import java.io.PrintWriter
import java.io.StringWriter
import java.util.*
import javax.swing.JOptionPane.PLAIN_MESSAGE
import javax.swing.JOptionPane.showMessageDialog
import kotlin.math.*
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
fun Random.next4way(vararg dir: Dir = emptyArray()): Dir {
    while (true) {
        val rand = Dir[nextInt(4) * 2]
        if (rand !in dir) return rand
    }
}

fun <T> Collection<Pair<T, Double>>.random(random: Random = gehenna.utils.random): T {
    assert(this.sumByDouble { it.second } == 1.0)
    val value = random.nextDouble()
    var sum = 0.0
    this.forEach {
        sum += it.second
        if (sum >= value) {
            return it.first
        }
    }

    throw GehennaException("For some reason random failed: sum = $sum, value = $value, array = $this")
}

val <T : Number> Collection<T>.mean
    get() = sumByDouble { it.toDouble() } / size

val <T : Number> Collection<T>.std
    get() = sqrt(map { it.toDouble().pow(2) }.mean - mean.pow(2))

/**
 * Cartesian product of two collections
 */
operator fun <T, S> Iterable<T>.times(other: Iterable<S>): List<Pair<T, S>> {
    return cartesianProduct(other) { first, second -> first to second }
}

fun <T, S : Comparable<S>> Iterable<T>.minOf(func: (T) -> S): S? {
    return asSequence().map { func(it) }.min()
}

fun <T, S : Comparable<S>> Iterable<T>.maxOf(func: (T) -> S): S? {
    return asSequence().map { func(it) }.max()
}

fun <T, S, V> Iterable<T>.cartesianProduct(other: Iterable<S>, transformer: (first: T, second: S) -> V): List<V> {
    return flatMap { first -> other.map { second -> transformer.invoke(first, second) } }
}

/**
 * Max of two colors by their brightness (sum of RGB)
 */
fun max(a: Color, b: Color): Color {
    return if (a > b) a else b
}

/**
 * Min of two colors by their brightness (sum of RGB)
 */
fun min(a: Color, b: Color): Color {
    return if (a < b) a else b
}

/**
 * Compares angle by sum of RGB values, kinda compares color brightness
 */
operator fun Color.compareTo(other: Color): Int {
    return (this.red + this.blue + this.green).compareTo(other.red + other.green + other.blue)
}

/**
 * Sets variable value through reflection by name, used for setting default values when reading objects from save file
 */
inline fun <reified T> T.setVal(name: String, value: Any?) {
    val field = T::class.java.getDeclaredField(name)
    field.isAccessible = true
    field.set(this, value)
}

fun sign(x: Int): Int {
    return sign(x.toDouble()).toInt()
}

/**
 * Returns normalized angle in (-PI; PI)
 */
fun Double.normalizeAngle(): Double {
    return atan2(sin(this), cos(this))
}

infix fun <A, B, C> Pair<A, B>.to(that: C): Triple<A, B, C> {
    return Triple(this.first, this.second, that)
}

infix fun <A, B, C> A.to(that: Pair<B, C>): Triple<A, B, C> {
    return Triple(this, that.first, that.second)
}

class FixedQueue<T>(val capacity: Int) : Queue<T> by FixedQueueImpl<T>(capacity) {
    fun isFull(): Boolean {
        return size == capacity
    }
}

private open class FixedQueueImpl<T>(private val maxCapacity: Int, private val deq: ArrayDeque<T> = ArrayDeque(maxCapacity)) : Queue<T> by deq {
    override fun offer(a: T): Boolean {
        assert(size < maxCapacity)
        return deq.offer(a)
    }

    override fun add(element: T): Boolean {
        assert(size < maxCapacity)
        return deq.add(element)
    }
}
