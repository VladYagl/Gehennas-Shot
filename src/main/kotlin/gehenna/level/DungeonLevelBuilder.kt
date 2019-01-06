package gehenna.level

import gehenna.component.Floor
import gehenna.component.Stairs
import gehenna.core.Entity
import gehenna.factory.Factory
import gehenna.factory.LevelPart
import gehenna.utils.Point
import gehenna.utils.until
import gehenna.utils.x
import gehenna.utils.y
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
        return DungeonLevel(width, height, Point(2, 2), (previous?.depth ?: -1) + 1).apply {
            previous?.let { previous ->
                val stairs = factory.new("stairsUp")
                stairs[Stairs::class]?.destination = previous to (backPoint ?: previous.startPosition)
                spawn(stairs, startPosition.x, startPosition.y)
            }

            room(0, 0, width, height)

            part(10, 10, "hall")


            //spawn(factory.new("stairs"), 2, 2)
            //spawn(factory.new("rifle"), 1, 1)
        }
    }

    class DungeonLevel(width: Int, height: Int, override val startPosition: Point, val depth: Int = 0) : Level(width, height) {
        override fun toString(): String = "Dungeon Level #$depth"
    }

    private fun Level.wall(x: Int, y: Int) {
        spawn(factory.new("wall"), x, y)
    }

    private fun Level.floor(x: Int, y: Int) {
        spawn(factory.new("floor"), x, y)
    }

    private fun Level.room(x1: Int, y1: Int, width: Int, height: Int) {
        for ((x, y) in (x1 to y1) until (x1 + width to y1 + height)) {
            if (get(x, y).none { it.has(Floor::class) }) {
                floor(x, y)
            }
            if (x == x1 || x == x1 + width - 1 || y == y1 || y == y1 + height - 1) {
                wall(x, y)
            }
        }
    }

    private fun Level.part(x: Int, y: Int, name: String) {
        partFactory.new(name).spawnTo(x, y, this)
    }
}

