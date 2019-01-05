package gehenna.level

import gehenna.EntityFactory
import gehenna.components.Floor
import gehenna.components.Obstacle
import gehenna.components.Position
import gehenna.components.Stairs
import gehenna.utils.*

class DungeonLevel(width: Int, height: Int, factory: EntityFactory, val depth: Int = 0, private val previous: Stairs? = null) : Level(width, height, factory) {
    var stairsUp: Stairs? = null

    private fun wall(x: Int, y: Int) {
        spawn(factory.newEntity("wall"), x, y)
    }

    private fun floor(x: Int, y: Int) {
        spawn(factory.newEntity("floor"), x, y)
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

    fun init() {
        room(0, 0, width, height)

        val room = random.nextInt(width - 5) to random.nextInt(height - 5)
        val size = random.nextInt(width - room.x - 5) + 4 to random.nextInt(height - room.y - 5) + 4

        room(room.x, room.y, size.x, size.y)
        remove(cells[room.x + size.x - 1, room.y + random.nextInt(size.y - 1)].find { it.has(Obstacle::class) }!!)

        spawn(
                factory.newEntity("bandit"),
                room.x + 1 + random.nextInt(size.x - 3),
                room.y + 1 + random.nextInt(size.y - 3)
        )
        spawn(
                factory.newEntity("stairsDown"),
                room.x + 1 + random.nextInt(size.x - 3),
                room.y + 1 + random.nextInt(size.y - 3)
        )

        previous?.let { prev ->
            val stairs = factory.newEntity("stairsUp")
            stairs[Stairs::class]?.pos = prev.entity[Position::class]
            spawn(stairs, 2, 2)
            stairsUp = stairs[Stairs::class]
        }
//        spawn(factory.newEntity("stairs"), 2, 2)

        spawn(factory.newEntity("teddy bear"), 1, 1)
        spawn(factory.newEntity("rifle"), 1, 1)
    }

    override fun toString(): String {
        return "dungeon level #$depth"
    }
}