package gehenna.utils

import gehenna.component.Position
import gehenna.component.Reflecting
import gehenna.level.Level
import kotlin.math.*
import kotlin.random.Random

/**
 * Converts angle in radians to an Angle
 *
 * @param errorShift Relative error: how much line is shifted from center of a cell
 */
fun Double.toAngle(errorShift: Double = 0.0): Angle {
    val angle = this.normalizeAngle()

    val x: Int
    val y: Int
    val tg = (tan(angle) * 1000).roundToInt()

    // check if tg(pi/2) = inf
    if (tg == Int.MAX_VALUE || tg == Int.MIN_VALUE) {
        x = 0
        y = -sign(tg)
    } else {
        x = 1000
        y = tg
    }

    val dist = max(abs(x), abs(y))
    val error = abs(x) - abs(y) + errorShift * dist
    return if (angle < PI / 2 && angle > -PI / 2) {
        Angle(x, y, error.roundToInt())
    } else {
        Angle(-x, -y, error.roundToInt())
    }
}

fun Random.nextAngle(angle: Angle, spread: Double): Angle {
    return if (spread > 0) {
        (angle.value + this.nextDouble(spread) - this.nextDouble(spread)).toAngle(angle.errorShift)
    } else angle
}

/**
 * Line from (0, 0) to (x, y) which represents an angle for projectiles/line drawing using Bresneham's algorithm
 *
 * @param error - shifts line up/down keeping directions,
 */
data class Angle(override val x: Int, override val y: Int, val error: Int = abs(x) - abs(y)) : Point {

    /**
     * Relative error, represents how much lines is shifted from center of a cell,
     * used to calculate multiple lines from same <position, error>
     */
    val errorShift: Double get() = (error - (abs(x) - abs(y))).toDouble() / this.max

    val value: Double = atan2(y.toDouble(), x.toDouble())
    val defaultError: Int get() = abs(x) - abs(y)

    // if minError <= error <= maxError then line still passes through (x, y)
    val maxError: Int get() = defaultError + (max - (if (abs(y) > abs(x)) 1 else 0)) / 2
    val minError: Int get() = defaultError - (max - (if (abs(y) > abs(x)) 0 else 1)) / 2

    override val dir
        get(): Dir {
            return next(0 at 0, defaultError).second.dir
        }

    /**
     * Next point in the line and value of the error in that point
     */
    fun next(point: Point, error: Int = this.error): Pair<Int, Point> {
        val dx = abs(x)
        val dy = -abs(y)
        var nx = point.x
        var ny = point.y
        var ne = error
        if (error * 2 >= dy) {
            nx += sign(x)
            ne += dy
        }
        if (error * 2 <= dx) {
            ne += dx
            ny += sign(y)
        }
        return Pair(ne, nx at ny)
    }

    /**
     * Walks a line from start nSteps forward, if levelBounce is passed the line will bounce of the walls in that level
     */
    fun walkLine(start: Point, nSteps: Int, levelBounce: Level? = null, visit: (Point) -> Boolean) {
        var point = start
        var angle = this
        var curError = this.error
        repeat(nSteps) {
            val (newError, nextPoint) = angle.next(point, curError)
            curError = newError

            if (levelBounce != null) {
                val obstacle = levelBounce.obstacle(nextPoint)

                if (obstacle?.has<Reflecting>() == true) {
                    val (dx, dy) = (nextPoint - point).dir.bounce(point, levelBounce, angle)
                    angle = Angle(dx, dy, curError)
                } else {
                    point = nextPoint
                }
            } else {
                point = nextPoint
            }

            if (!visit(point))
                return@repeat
        }
    }

    /**
     * Returns error value to avoid obstacles from specified position, null if it is not possible
     */
    fun findBestError(from: Position): Int? {
        val target = from + this.point
        val path = from.level.getLOS(from, target)
        if (path != null && (path.last() equals target)) {
            val dx = abs(this.x)
            val dy = -abs(this.y)
            var eMin = this.minError
            var eMax = this.maxError
            var eAdd: Int = 0
            var last: Point = from

            path.drop(1).forEach {
                val oldE = eAdd
                if (last.x != it.x) { // error >= dy / 2 - eAdd
                    eMin = max(eMin, dy / 2 - oldE)
                    eAdd += dy
                    assert((eMin + oldE) * 2 >= dy) { "eMin = $eMin, oldE = $oldE, dy = $dy" }
                } else { // error < dy / 2 - eAdd
                    eMax = min(eMax, dy / 2 - 1 - oldE)
                    assert((eMax + oldE) * 2 < dy) { "eMax = $eMax, oldE = $oldE, dy = $dy" }
                }
                if (last.y != it.y) { // error <= dx / 2 - eAdd
                    eMax = min(eMax, dx / 2 - oldE)
                    eAdd += dx
                    assert((eMax + oldE) * 2 <= dx) { "eMax = $eMax, oldE = $oldE, dx = $dx" }
                } else { // error > dx / 2 - eAdd
                    eMin = max(eMin, dx / 2 + 1 - oldE)
                    assert((eMin + oldE) * 2 > dx) { "eMin = $eMin, oldE = $oldE, dx = $dx" }
                }
                last = it
            }

            println("ans = ($eMin --- $eMax), \n$path")
            return (eMin + eMax) / 2
        } else {
            val goods = (this.minError..this.maxError).mapNotNull {error ->
                var good = false
                Angle(this.x, this.y, error).walkLine(from, (target - from).max, from.level) {
                    if (it equals target) {
                        good = true
                    }
                    true
                }
                if (good) {
                    error
                } else {
                    null
                }
            }
            return if (goods.isNotEmpty()) {
                println("Found Line")
                goods.mean.roundToInt()
            } else {
                println("Default Error // No line")
                null
            }
        }
    }

    /**
     * Return next point in line from <other> point, overrides plus for backward compatibility with ordinary 8-way dir
     */
    override fun plus(other: Point): Point {
        return next(other).second
    }

    /**
     * I don't know how nor do I need a way to calculate previous point in the line
     */
    override fun minus(other: Point): Point {
        throw UnsupportedOperationException("No minus with Line Direction")
    }
}