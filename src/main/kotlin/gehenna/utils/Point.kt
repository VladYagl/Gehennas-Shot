package gehenna.utils

import gehenna.utils.Point.Companion.zero
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sign

interface Point {
    val x: Int
    val y: Int
    operator fun minus(other: Point): Point = PointImpl(x - other.x, y - other.y)

    operator fun plus(other: Point): Point = PointImpl(x + other.x, y + other.y)

    val dir: Dir get() = x.sign on y.sign
    val size: Size get() = Size(x, y)
    val max: Int get() = max(abs(x), abs(y))

    infix fun until(to: Point): List<Point> {
        return ((x until to.x) * (y until to.y)).map { it.point }
    }

    operator fun rangeTo(to: Point): List<Point> {
        return ((x..to.x) * (y..to.y)).map { it.point }
    }

    operator fun component1(): Int = x
    operator fun component2(): Int = y

    companion object {
        val zero = 0 at 0
    }
}

private data class PointImpl(override val x: Int, override val y: Int) : Point

data class Dir(override val x: Int, override val y: Int) : Point {
    init {
        assert(x in -1..1)
        assert(y in -1..1)
    }

    val turnRight get() = Dir[(Dir.indexOf(dir) + 1) % Dir.size]
    val turnLeft get() = Dir[(Dir.indexOf(dir) + Dir.size - 1) % Dir.size]

    companion object : List<Dir> by listOf(
            1 on 0,
            1 on 1,
            0 on 1,
            -1 on 1,
            -1 on 0,
            -1 on -1,
            0 on -1,
            1 on -1
    ) {
        val east = this[0]
        val southeast = this[1]
        val south = this[2]
        val southwest = this[3]
        val west = this[4]
        val northwest = this[5]
        val north = this[6]
        val northeast = this[7]
        val zero = 0 on 0
    }

    override fun toString(): String {
        return when (this) {
            east -> "east"
            southeast -> "southeast"
            south -> "south"
            southwest -> "southwest"
            west -> "west"
            northwest -> "northwest"
            north -> "north"
            northeast -> "northeast"
            zero -> "zero"
            else -> throw Exception("This is not a direction [$x on $y]???")
        }
    }
}

data class Size(val width: Int, val height: Int) : Point {
    operator fun contains(point: Point): Boolean = point.x >= 0 && point.y >= 0 && point.x < width && point.y < height

    override val x: Int = width
    override val y: Int = height

    val range: List<Point> get() = zero until this
}

val Pair<Int, Int>.point: Point get() = PointImpl(first, second)
val Pair<Int, Int>.dir get() = Dir(first, second)
infix fun Int.at(y: Int): Point = PointImpl(this, y)
infix fun Int.on(y: Int) = Dir(this, y)
