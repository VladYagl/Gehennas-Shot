package gehenna.utils

import gehenna.component.Position
import gehenna.component.Reflecting
import gehenna.level.Level
import kotlin.math.*

/**
 * Converts angle in radians to a LineDir
 *
 * @param errorShift Relative error: how much line is shifted from center of a cell
 */
fun Double.toLineDir(errorShift: Double = 0.0): LineDir {
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
        LineDir(x, y, error.roundToInt())
    } else {
        LineDir(-x, -y, error.roundToInt())
    }
}

/**
 * Line from (0, 0) to (x, y) which represents a direction for projectiles/line drawing using Bresneham's algorithm
 *
 * @param error - shifts line up/down keeping directions,
 */
data class LineDir(override val x: Int, override val y: Int, val error: Int = abs(x) - abs(y)) : Point {

    /**
     * Relative error, represents how much lines is shifted from center of a cell,
     * used to calculate multiple lines from same <position, error>
     */
    val errorShift: Double get() = (error - (abs(x) - abs(y))).toDouble() / this.max

    val angle: Double = atan2(y.toDouble(), x.toDouble())
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
        var dir = this
        var curError = this.error
        repeat(nSteps) {
            val (newError, nextPoint) = dir.next(point, curError)
            curError = newError

            if (levelBounce != null) {
                val obstacle = levelBounce.obstacle(nextPoint)

                if (obstacle?.has<Reflecting>() == true) {
                    val (dx, dy) = (nextPoint - point).dir.bounce(point, levelBounce, dir)
                    dir = LineDir(dx, dy, curError)
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
//         TODO: maybe this can work, but for now the dumb way is better
//        val path = from.level.getLOS(from, target)
//        if (path != null && (path.last() equals target)) {
//            val dx = abs(this.x)
//            val dy = -abs(this.y)
//            var eMin = this.minError
//            var eMax = this.maxError
//            var eAdd: Int = 0
//            var last: Point = from
//
//            path.drop(1).dropLast(1).forEach {
//                val oldE = eAdd
//                if (last.x != it.x) { // error >= dy / 2 - eAdd
//                    eMin = max(eMin, dy / 2 - oldE)
//                    eAdd += dy
//                } else { // error < dy / 2 - eAdd
//                    eMax = min(eMax, (dy - 1) / 2 - oldE) // TODO check the math!
//                }
//                if (last.y != it.y) { // error <= dx / 2 - eAdd
//                    eMax = min(eMax, dx / 2 - oldE)
//                    eAdd += dx
//                } else { // error > dx / 2 - eAdd
//                    eMin = max(eMin, (dx + 1) / 2 - oldE)
//                }
//                last = it
//            }
//
//            println("ans = ($eMin --- $eMax), \n$path")
//            return (eMin + eMax) / 2
//        } else {
//            println("NO LINE OF SIGHT!!!")
        for (error in (this.minError..this.maxError)) {
            var good = false
            LineDir(this.x, this.y, error).walkLine(from, (target - from).max, from.level) {
                if (it equals target) {
                    good = true
                }
                true
            }
            if (good) {
//                    println("Found line: $error")
                return error
            }
        }
//            println("Default Error // No line")
        return null
//        }
    }

    /**
     * Return next point in line from <other> point
     */
    override fun plus(other: Point): Point {
        return next(other).second
    }

    override fun minus(other: Point): Point {
        throw UnsupportedOperationException("No minus with Line Direction")
    }
}