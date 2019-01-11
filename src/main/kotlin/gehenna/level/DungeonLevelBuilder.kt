package gehenna.level

import gehenna.component.Floor
import gehenna.component.Obstacle
import gehenna.core.Entity
import gehenna.factory.Factory
import gehenna.factory.LevelPart
import gehenna.utils.*
import kotlin.reflect.full.safeCast

class DungeonLevelBuilder : LevelBuilder<DungeonLevelBuilder.DungeonLevel> {
    private class EmptyFactory<T> : Factory<T> {
        override fun new(name: String): T = throw Exception("this is empty factory")
    }

    private var factory: Factory<Entity> = EmptyFactory()
    private var partFactory: Factory<LevelPart> = EmptyFactory()
    private var width: Int = 5 * 8
    private var height: Int = 6 * 8

    private var previous: Level? = null
    private var backPoint: Point? = null

    override fun withPrevious(level: Level) = also { this.previous = level }

    override fun withBackPoint(point: Point) = also { this.backPoint = point }

    fun withFactory(factory: Factory<Entity>) = also { this.factory = factory }

    fun withPartFactory(partFactory: Factory<LevelPart>) = also { this.partFactory = partFactory }

    fun withSize(width: Int, height: Int) = also {
        this.width = width
        this.height = height
    }

    override fun build(): DungeonLevel {
        val previous = DungeonLevel::class.safeCast(this.previous)
        return DungeonLevel(
                width,
                height,
                backPoint ?: random.nextPoint(3, 3, 5, 5),
                (previous?.depth ?: -1) + 1
        ).apply {
            previous?.let { previous ->
                //                val stairs = factory.new("stairsUp")
//                stairs[Stairs::class]?.destination = previous to (backPoint ?: previous.startPosition)
//                spawn(stairs, startPosition.x, startPosition.y)
            }

            box(0, 0, width, height)
            box(startPosition.x - 2, startPosition.y - 2, 5, 5)
            remove(get(startPosition.x, startPosition.y + 2).first())
            corridor(startPosition.x, startPosition.y, 0 to 1) // only 4way dirs!
            rect(startPosition.x - 2, startPosition.y - 2, 5, 5)
            spawn(factory.new("door"), startPosition.x, startPosition.y + 2)


//            room(0, 0, width, height)
//            part(10, 10, "hall")
            allWalls()
            spawn(factory.new("stairsDown"), startPosition)
            //spawn(factory.new("rifle"), 1, 1)
        }
    }

    class DungeonLevel(width: Int, height: Int, override val startPosition: Point, val depth: Int = 0) : Level(width, height) {
        override fun toString(): String = "Dungeon Level #$depth"
    }

    private fun Level.corridor(x: Int, y: Int, dir: Point, len: Int = 0, door: Boolean = false) {
        if (!inBounds(x, y) || get(x, y).isNotEmpty()) return
        floor(x, y)
        if (door) spawn(factory.new("door"), x, y)
        var newLen = len + 1
        if (!door) {
            val d = turnLeft(turnLeft(dir))
            val p = (x to y) + d
            if (inBounds(p.x, p.y) && get(p.x, p.y).isEmpty()) floor(p.x, p.y)
        }
        if (len > 8)
            if (random.nextDouble() > 0.8) {
                val tempDir = turnRight(turnRight(dir))
                corridor(x + tempDir.x, y + tempDir.y, tempDir, 0, true)
                newLen = 3
            }

        if (len > 16)
            if (random.nextDouble() > 0.8) {
                val newDir = turnRight(turnRight(dir))
                corridor(x + newDir.x, y + newDir.y, newDir, 0, true)
                return
            }
        corridor(x + dir.x, y + dir.y, dir, newLen, false)
    }

    private fun Level.allWalls() {
        for ((x, y) in range(width, height)) {
            if (get(x, y).isEmpty())
                for (dir in directions) {
                    if (safeGet(x + dir.x, y + dir.y).isNotEmpty() && safeGet(x + dir.x, y + dir.y).none { it[Obstacle::class]?.blockMove == true }) {
                        wall(x, y)
                        break
                    }
                }
        }
    }

    private fun Level.wall(x: Int, y: Int) {
        spawn(factory.new("wall"), x, y)
    }

    private fun Level.floor(x: Int, y: Int) {
        spawn(factory.new("floor"), x, y)
    }

    private fun Level.rect(x1: Int, y1: Int, width: Int, height: Int) {
        for ((x, y) in (x1 to y1) until (x1 + width to y1 + height)) {
            if (get(x, y).none { it.has(Floor::class) }) {
                floor(x, y)
            }
        }
    }

    private fun Level.box(x1: Int, y1: Int, width: Int, height: Int) {
        for ((x, y) in (x1 to y1) until (x1 + width to y1 + height)) {
            if (x == x1 || x == x1 + width - 1 || y == y1 || y == y1 + height - 1) {
                wall(x, y)
            }
        }
    }

    private fun Level.part(x: Int, y: Int, name: String) {
        partFactory.new(name).spawnTo(x, y, this)
    }
}

