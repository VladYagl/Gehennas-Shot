package gehenna.level

import gehenna.component.Stairs
import gehenna.core.Context
import gehenna.utils.*
import gehenna.utils.Dir.Companion.east
import gehenna.utils.Dir.Companion.south
import gehenna.utils.Dir.Companion.southeast
import gehenna.utils.Point.Companion.zero
import kotlin.reflect.full.safeCast

class DungeonLevelFactory(context: Context) : BaseLevelFactory<Level>(context) {
    override fun build(previous: Level?, backPoint: Point?): Pair<Level, Point> {
        val startPosition = backPoint ?: random.nextPoint(3, 3, 5, 5)
        return Pair(Level(size, (previous?.depth ?: -1) + 1).apply {
            previous?.let {
                backPoint?.let {
                    val stairs = factory.new("stairsUp")
                    stairs<Stairs>()?.destination = previous to backPoint
                    spawn(stairs, startPosition)
                }
            }

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
                val floor = size.range.count { isWalkable(it) }
                if (walkableSquare(startPosition) < floor || !isWalkable(startPosition)) clear()
                else break
            }
            allWalls()
            box(zero, size)

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

            while (true) {
                val point = random.nextPoint(size)
                if (isWalkable(point) && findPath(startPosition, point)?.size ?: 0 > 25) {
                    spawn(factory.new("stairsDown"), point)
                    break
                }
            }

//            box(startPosition.x - 2, startPosition.y - 2, 5, 5)
//            remove(get(startPosition.x, startPosition.y + 2).first())
//            corridor(startPosition.x, startPosition.y, 0 to 1) // only 4way dirs!
//            rect(startPosition.x - 2, startPosition.y - 2, 5, 5)
//            spawn(factory.new("door"), startPosition.x, startPosition.y + 2)


//            box(0, 0, width, height)
//            rect(0, 0, width, height)
//            room(0, 0, width, height)
//            part(10, 10, "hall")

//            spawn(factory.new("stairsDown"), startPosition)
//            spawn(factory.new("rifle"), startPosition)
//            spawn(factory.new("pistol"), startPosition)
        }, startPosition)
    }
}
