package gehenna.level

import gehenna.component.Stairs
import gehenna.utils.*
import kotlin.reflect.full.safeCast

class DungeonLevelBuilder : BaseLevelBuilder<DungeonLevelBuilder.DungeonLevel>() {

    override fun build(): DungeonLevel {
        val previous = DungeonLevel::class.safeCast(this.previous)
        return DungeonLevel(
            width,
            height,
            backPoint ?: random.nextPoint(3, 3, 5, 5),
            (previous?.depth ?: -1) + 1
        ).apply {
            previous?.let { previous ->
                val stairs = factory.new("stairsUp")
                stairs<Stairs>()?.destination = previous to (backPoint ?: previous.startPosition)
                spawn(stairs, startPosition.x, startPosition.y)
            }

            while (true) {
                automaton(startPosition.x, startPosition.y, depth)
                for ((x, y) in range(width - 1, height - 1)) {
                    if (has(x, y) && has(x + 1, y + 1) && !has(x + 1, y) && !has(x, y + 1)) {
                        part(x - 2, y - 2, "se_connector")
                    }
                    if (!has(x, y) && !has(x + 1, y + 1) && has(x + 1, y) && has(x, y + 1)) {
                        part(x - 2, y - 2, "sw_connector")
                    }
                }
                val floor = range(width, height).count { (x, y) -> isWalkable(x, y) }
                if (walkableSquare(startPosition.x, startPosition.y) < floor) clear()
                else break
            }
            allWalls()
            box(0, 0, width, height)

            if (depth == 0) repeat(random.nextInt(6) + 4) {
                while (true) {
                    val point = random.nextPoint(width, height)
                    if (isWalkable(point.x, point.y)) {
                        spawn(factory.new("bandit"), point)
                        break
                    }
                }
            }
            else repeat(random.nextInt(6) + 4) {
                while (true) {
                    val point = random.nextPoint(width, height)
                    if (isWalkable(point.x, point.y)) {
                        spawn(factory.new("strongBandit"), point)
                        break
                    }
                }
            }

            while (true) {
                val point = random.nextPoint(width, height)
                if (isWalkable(point.x, point.y) && findPath(
                        startPosition.x,
                        startPosition.y,
                        point.x,
                        point.y
                    )?.size ?: 0 > 25
                ) {
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
        }
    }

    class DungeonLevel(width: Int, height: Int, override val startPosition: Point, val depth: Int = 0) :
        Level(width, height) {
        override fun toString(): String = "Dungeon Level #$depth"
    }
}
