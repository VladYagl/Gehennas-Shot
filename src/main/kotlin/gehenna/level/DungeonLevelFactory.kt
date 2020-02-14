package gehenna.level

import gehenna.component.Floor
import gehenna.component.Stairs
import gehenna.core.Context
import gehenna.utils.Dir.Companion.east
import gehenna.utils.Dir.Companion.south
import gehenna.utils.Dir.Companion.southeast
import gehenna.utils.Point
import gehenna.utils.Point.Companion.zero
import gehenna.utils.at
import gehenna.utils.nextPoint
import gehenna.utils.random

class DungeonLevelFactory(context: Context) : BaseLevelFactory<Level>(context) {
    override fun build(previous: Level?, backPoint: Point?): Pair<Level, Point> {
        val startPosition = backPoint ?: random.nextPoint(3, 3, 5, 5)
        return Pair(Level(size, (previous?.depth ?: -1) + 1).apply {
            //Place random walls and 3-tiles wide corridors
            while (true) {
                automaton(startPosition)
                for (p in (size - (1 at 1)).size.range) {
                    if (has(p) && has(p + southeast) && !has(p + east) && !has(p + south)) {
                        part(p.x - 2 at p.y - 2, "se_connector")
                    }
                    if (!has(p) && !has(p + southeast) && has(p + east) && has(p + south)) {//fixme
                        part(p.x - 2 at p.y - 2, "sw_connector")
                    }
                }
                size.range.forEach {
                    while (get(it).count { entity -> entity.has<Floor>() } > 1) {
                        remove(get(it).find { entity -> entity.has<Floor>() }!!) // todo: this remove double floors | maybe you can do it better
                    }
                }
                val floor = size.range.count { isWalkable(it) }
                if (walkableSquare(startPosition) < floor || !isWalkable(startPosition)) clear()
                else break
            }
            allWalls()
            box(zero, size)

            //Place bandits depending on level
            if (depth == 0) repeat(random.nextInt(6) + 4) {
                while (true) {
                    val point = random.nextPoint(size)
                    if (isWalkable(point)) {
                        spawn(factory.new("bandit"), point)
                        break
                    }
                }
            }
            else repeat(random.nextInt(6) + 4) {
                while (true) {
                    val point = random.nextPoint(size)
                    if (isWalkable(point)) {
                        spawn(factory.new("strongBandit"), point)
                        break
                    }
                }
            }

            //Place stairs
            while (true) {
                val point = random.nextPoint(size)
                if (isWalkable(point) && findPath(startPosition, point)?.size ?: 0 > 25) {
                    spawn(factory.new("stairsDown"), point)
                    break
                }
            }

            previous?.let {
                backPoint?.let {
                    val stairs = factory.new("stairsUp")
                    stairs<Stairs>()?.destination = previous to backPoint
                    spawn(stairs, startPosition)
                }
            }
        }, startPosition)
    }
}
