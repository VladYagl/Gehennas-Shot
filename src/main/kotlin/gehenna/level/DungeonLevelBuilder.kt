package gehenna.level

import gehenna.component.Floor
import gehenna.component.Obstacle
import gehenna.component.Stairs
import gehenna.core.Entity
import gehenna.factory.Factory
import gehenna.factory.LevelPart
import gehenna.utils.*
import java.lang.Math.pow
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.random.Random
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
                val stairs = factory.new("stairsUp")
                stairs[Stairs::class]?.destination = previous to (backPoint ?: previous.startPosition)
                spawn(stairs, startPosition.x - 1, startPosition.y)
            }

            automaton(startPosition.x, startPosition.y, depth)
//            floor(startPosition.x, startPosition.y)
            box(0, 0, width, height)

//            box(startPosition.x - 2, startPosition.y - 2, 5, 5)
//            remove(get(startPosition.x, startPosition.y + 2).first())
//            corridor(startPosition.x, startPosition.y, 0 to 1) // only 4way dirs!
//            rect(startPosition.x - 2, startPosition.y - 2, 5, 5)
//            spawn(factory.new("door"), startPosition.x, startPosition.y + 2)


//            box(0, 0, width, height)
//            rect(0, 0, width, height)
//            room(0, 0, width, height)
//            part(10, 10, "hall")
            allWalls()
            spawn(factory.new("stairsDown"), startPosition)
            spawn(factory.new("rifle"), startPosition)
            spawn(factory.new("pistol"), startPosition)
        }
    }

    class DungeonLevel(width: Int, height: Int, override val startPosition: Point, val depth: Int = 0) :
        Level(width, height) {
        override fun toString(): String = "Dungeon Level #$depth"
    }

    private fun Level.corridor(x: Int, y: Int, dir: Point, len: Int = 0, door: Boolean = false) {
        if (!inBounds(x, y) || get(x, y).isNotEmpty()) return
        floor(x, y)
        if (door) spawn(factory.new("door"), x, y)
        var newLen = len + 1
        if (!door) {
            val d = turnLeft(turnLeft(dir))
            var p = (x to y) + d
            if (inBounds(p.x, p.y) && get(p.x, p.y).isEmpty()) floor(p.x, p.y)
            p = (x to y) - d
            if (inBounds(p.x, p.y) && get(p.x, p.y).isEmpty()) floor(p.x, p.y)
        }
        if (len > 8)
            if (random.nextDouble() > 0.8) {
                val tempDir = random.next4way(dir)
                corridor(x + tempDir.x * 2, y + tempDir.y * 2, tempDir, 0, true)
                newLen = 3
            }

        if (len > 16)
            if (random.nextDouble() > 0.8) {
                val newDir = random.next4way(dir)
                corridor(x + newDir.x * 2, y + newDir.y * 2, newDir, 0, true)
                return
            }
        corridor(x + dir.x, y + dir.y, dir, newLen, false)
    }

    private fun Level.allWalls() {
        for ((x, y) in range(width, height)) {
            if (get(x, y).isEmpty())
                for (dir in directions) {
                    if (safeGet(x + dir.x, y + dir.y).isNotEmpty() && safeGet(
                            x + dir.x,
                            y + dir.y
                        ).none { it[Obstacle::class]?.blockMove == true }
                    ) {
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

    private fun Level.automaton(x: Int, y: Int, k: Int) {
        val random = Random(seed)
        val cellular = CellularPart(width, height) { x1, y1 -> floor(x1, y1) }
        for ((i, j) in range(width, height)) {
            fun norm(x: Int) = pow(x.toDouble(), 0.8)
            val d = norm(abs(i - x) + abs(y - j))
//            if (random.nextDouble() < norm(width + height) / d * 0.08 + 0.1) cellular.cells[i, j] = true
            if (random.nextDouble() < 0.4) cellular.cells[i, j] = true
        }
        cellular.automaton(3, 4, k)
        cellular.spawnTo(0, 0, this)
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

private val seed = random.nextInt()
