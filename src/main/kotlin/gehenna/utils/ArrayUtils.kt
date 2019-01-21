@file:Suppress("FunctionName")

package gehenna.utils

inline fun <reified T> Array(width: Int, height: Int, init: () -> T) = Array(width) { Array(height) { init() } }

inline fun DoubleArray(width: Int, height: Int, init: () -> Double) = Array(width) { DoubleArray(height) { init() } }

inline fun BooleanArray(width: Int, height: Int, init: () -> Boolean = { false }) =
    Array(width) { BooleanArray(height) { init() } }

fun Array<BooleanArray>.fill(element: Boolean = false) = forEach { it.fill(element) }

fun Array<DoubleArray>.fill(element: Double = 0.0) = forEach { it.fill(element) }

fun <T> Array<Array<T>>.fill(init: () -> T) = forEach { it.fill(init()) }

operator fun <T> Array<Array<T>>.get(x: Int, y: Int): T {
    return get(x)[y]
}

operator fun <T> Array<Array<T>>.get(point: Point): T {
    return this[point.x, point.y]
}

operator fun <T> Array<Array<T>>.set(x: Int, y: Int, a: T) {
    get(x)[y] = a
}

operator fun <T> Array<Array<T>>.set(point: Point, a: T) {
    this[point.x, point.y] = a
}

operator fun Array<DoubleArray>.get(x: Int, y: Int): Double {
    return get(x)[y]
}

operator fun Array<DoubleArray>.get(point: Point): Double {
    return this[point.x, point.y]
}

operator fun Array<DoubleArray>.set(x: Int, y: Int, a: Double) {
    get(x)[y] = a
}

operator fun Array<DoubleArray>.set(point: Point, a: Double) {
    this[point.x, point.y] = a
}

operator fun Array<BooleanArray>.get(x: Int, y: Int): Boolean {
    return get(x)[y]
}

operator fun Array<BooleanArray>.get(point: Point): Boolean {
    return this[point.x, point.y]
}

operator fun Array<BooleanArray>.set(x: Int, y: Int, a: Boolean) {
    get(x)[y] = a
}

operator fun Array<BooleanArray>.set(point: Point, a: Boolean) {
    this[point.x, point.y] = a
}
