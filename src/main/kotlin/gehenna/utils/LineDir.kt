package gehenna.utils

import gehenna.component.Reflecting
import gehenna.level.Level
import kotlin.math.*

fun Double.toLineDir(): LineDir {
    val angle = this.normalizeAngle()

    val x: Int
    val y: Int
    val tg = (tan(angle) * 1000).roundToInt()

    if (tg == Int.MAX_VALUE || tg == Int.MIN_VALUE) {
        x = 0
        y = -sign(tg)
    } else {
        x = 1000
        y = tg
    }

    return if (angle < PI / 2 && angle > -PI / 2) {
        LineDir(x, y)
    } else {
        LineDir(-x, -y)
    }
}

data class LineDir(override val x: Int, override val y: Int, val error: Int = abs(x) - abs(y)) : Point {

    val angle: Double = atan2(y.toDouble(), x.toDouble())

    override val dir
        get(): Dir {
            return when {
                abs(x) > abs(y) * 2 -> {
                    (x at 0).dir
                }
                abs(y) > abs(x) * 2 -> {
                    (0 at y).dir
                }
                else -> {
                    (x at y).dir
                }
            }
        }

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
//        println("dx=$dx, dy=$dy, error=$ne")
        return Pair(ne, nx at ny)
    }

    fun walkLine(start: Point, nSteps: Int, levelBounce: Level? = null, walker: (Point) -> Boolean) {
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

            if (!walker(point))
                return@repeat
        }
    }

    override fun plus(other: Point): Point {
        return next(other).second
    }

    override fun minus(other: Point): Point {
        throw UnsupportedOperationException("No minus with Line Direction");
    }
}