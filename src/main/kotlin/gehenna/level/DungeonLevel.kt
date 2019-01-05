package gehenna.level

import gehenna.core.Entity
import gehenna.factory.Factory
import gehenna.component.Floor
import gehenna.component.Position
import gehenna.component.Stairs
import gehenna.factory.LevelPart
import gehenna.utils.get
import gehenna.utils.until

class DungeonLevel(width: Int, height: Int, factory: Factory<Entity>, val levelFactory: Factory<LevelPart>, val depth: Int = 0, private val previous: Stairs? = null) : Level(width, height, factory) {
    var stairsUp: Stairs? = null

    private fun wall(x: Int, y: Int) {
        spawn(factory.new("wall"), x, y)
    }

    private fun floor(x: Int, y: Int) {
        spawn(factory.new("floor"), x, y)
    }

    private fun room(x1: Int, y1: Int, width: Int, height: Int) {
        for ((x, y) in (x1 to y1) until (x1 + width to y1 + height)) {
            if (cells[x, y].none { it.has(Floor::class) }) {
                floor(x, y)
            }
            if (x == x1 || x == x1 + width - 1 || y == y1 || y == y1 + height - 1) {
                wall(x, y)
            }
        }
    }

    private fun part(x: Int, y: Int, name: String) {
        levelFactory.new(name).spawnTo(x, y, this)
    }

    fun init() {
        room(0, 0, width, height)

        part(10, 10, "hall")

        previous?.let { prev ->
            val stairs = factory.new("stairsUp")
            stairs[Stairs::class]?.pos = prev.entity[Position::class]
            spawn(stairs, 2, 2)
            stairsUp = stairs[Stairs::class]
        }

//        spawn(factory.newEntity("stairs"), 2, 2)
//        spawn(factory.new("rifle"), 1, 1)
    }

    override fun toString(): String {
        return "dungeon level #$depth"
    }
}