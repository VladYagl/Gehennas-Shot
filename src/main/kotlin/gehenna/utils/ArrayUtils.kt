@file:Suppress("FunctionName")

package gehenna.utils

/**
 * Functions to create/manipulate 2-dimensional arrays of Double/Boolean/Int
 */

inline fun <reified T> Array(width: Int, height: Int, init: () -> T) = Array(width) { Array(height) { init() } }

inline fun DoubleArray(width: Int, height: Int, init: () -> Double) = Array(width) { DoubleArray(height) { init() } }

inline fun BooleanArray(width: Int, height: Int, init: () -> Boolean = { false }) =
        Array(width) { BooleanArray(height) { init() } }

inline fun IntArray(width: Int, height: Int, init: () -> Int = { 0 }) =
        Array(width) { IntArray(height) { init() } }

inline fun CharArray(width: Int, height: Int, init: () -> Char = { 0.toChar() }) =
        Array(width) { CharArray(height) { init() } }

inline fun <reified T> Array(size: Size, init: () -> T) = Array(size.width, size.height, init)
inline fun DoubleArray(size: Size, init: () -> Double) = DoubleArray(size.width, size.height, init)
inline fun BooleanArray(size: Size, init: () -> Boolean = { false }) = BooleanArray(size.width, size.height, init)
inline fun IntArray(size: Size, init: () -> Int = { 0 }) = IntArray(size.width, size.height, init)
inline fun CharArray(size: Size, init: () -> Char = { 0.toChar() }) = CharArray(size.width, size.height, init)

fun <T> Array<Array<T>>.fill(init: () -> T) = forEach { it.fill(init()) }
fun Array<DoubleArray>.fill(element: Double = 0.0) = forEach { it.fill(element) }
fun Array<BooleanArray>.fill(element: Boolean = false) = forEach { it.fill(element) }
fun Array<IntArray>.fill(element: Int = 0) = forEach { it.fill(element) }
fun Array<CharArray>.fill(element: Char = 0.toChar()) = forEach { it.fill(element) }


// T Arrays

fun <T> Array<Array<T>>.inBounds(x: Int, y: Int): Boolean {
    return (0 <= x && x <= this.lastIndex) && (0 <= y && y <= this[x].lastIndex)
}

fun <T> Array<Array<T>>.inBounds(point: Point): Boolean = inBounds(point.x, point.y)

fun <T> Array<Array<T>>.getOrNull(x: Int, y: Int): T? {
    return if (inBounds(x, y)) {
        get(x, y)
    } else {
        null
    }
}

fun <T> Array<Array<T>>.getOrNull(point: Point) = getOrNull(point.x, point.y)

operator fun <T> Array<Array<T>>.get(x: Int, y: Int): T = get(x)[y]

operator fun <T> Array<Array<T>>.get(point: Point): T = this[point.x, point.y]

operator fun <T> Array<Array<T>>.set(x: Int, y: Int, a: T) {
    get(x)[y] = a
}

operator fun <T> Array<Array<T>>.set(point: Point, a: T) {
    get(point.x)[point.y] = a
}

// Double Arrays

fun Array<DoubleArray>.inBounds(x: Int, y: Int): Boolean {
    return (0 <= x && x <= this.lastIndex) && (0 <= y && y <= this[x].lastIndex)
}

fun Array<DoubleArray>.inBounds(point: Point): Boolean = inBounds(point.x, point.y)

fun Array<DoubleArray>.getOrNull(x: Int, y: Int): Double? {
    return if (inBounds(x, y)) {
        get(x, y)
    } else {
        null
    }
}

fun Array<DoubleArray>.getOrNull(point: Point) = getOrNull(point.x, point.y)

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
    get(point.x)[point.y] = a
}

// Boolean Arrays

fun Array<BooleanArray>.inBounds(x: Int, y: Int): Boolean {
    return (0 <= x && x <= this.lastIndex) && (0 <= y && y <= this[x].lastIndex)
}

fun Array<BooleanArray>.inBounds(point: Point): Boolean = inBounds(point.x, point.y)

fun Array<BooleanArray>.getOrNull(x: Int, y: Int): Boolean? {
    return if (inBounds(x, y)) {
        get(x, y)
    } else {
        null
    }
}

fun Array<BooleanArray>.getOrNull(point: Point) = getOrNull(point.x, point.y)

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
    get(point.x)[point.y] = a
}

// Int Arrays

fun Array<IntArray>.inBounds(x: Int, y: Int): Boolean {
    return (0 <= x && x <= this.lastIndex) && (0 <= y && y <= this[x].lastIndex)
}

fun Array<IntArray>.inBounds(point: Point): Boolean = inBounds(point.x, point.y)

fun Array<IntArray>.getOrNull(x: Int, y: Int): Int? {
    return if (inBounds(x, y)) {
        get(x, y)
    } else {
        null
    }
}

fun Array<IntArray>.getOrNull(point: Point) = getOrNull(point.x, point.y)

operator fun Array<IntArray>.get(x: Int, y: Int): Int {
    return get(x)[y]
}

operator fun Array<IntArray>.get(point: Point): Int {
    return this[point.x, point.y]
}

operator fun Array<IntArray>.set(x: Int, y: Int, a: Int) {
    get(x)[y] = a
}

operator fun Array<IntArray>.set(point: Point, a: Int) {
    get(point.x)[point.y] = a
}

// Char Arrays

fun Array<CharArray>.inBounds(x: Int, y: Int): Boolean {
    return (0 <= x && x <= this.lastIndex) && (0 <= y && y <= this[x].lastIndex)
}

fun Array<CharArray>.inBounds(point: Point): Boolean = inBounds(point.x, point.y)

fun Array<CharArray>.getOrNull(x: Int, y: Int): Char? {
    return if (inBounds(x, y)) {
        get(x, y)
    } else {
        null
    }
}

fun Array<CharArray>.getOrNull(point: Point) = getOrNull(point.x, point.y)

operator fun Array<CharArray>.get(x: Int, y: Int): Char {
    return get(x)[y]
}

operator fun Array<CharArray>.get(point: Point): Char {
    return this[point.x, point.y]
}

operator fun Array<CharArray>.set(x: Int, y: Int, a: Char) {
    get(x)[y] = a
}

operator fun Array<CharArray>.set(point: Point, a: Char) {
    get(point.x)[point.y] = a
}
