package gehenna.level

import gehenna.Entity
import gehenna.Factory
import gehenna.utils.Point
import gehenna.utils.x
import gehenna.utils.y

interface LevelPart {
    fun spawnTo(toX: Int, toY: Int, level: BasicLevel)
    fun needs(x: Int, y: Int): Boolean
    val width: Int
    val height: Int
}

class FixedPart(private val entities: List<Pair<Point, String>>, private val factory: Factory<Entity>) : LevelPart {
    override val width = entities.map { it.first.x }.max() ?: 0
    override val height = entities.map { it.first.y }.max() ?: 0

    override fun spawnTo(toX: Int, toY: Int, level: BasicLevel) {
        entities.forEach { (point, name) ->
            level.spawn(factory.new(name), toX + point.x, toY + point.y)
        }
    }

    override fun needs(x: Int, y: Int) = entities.any { it.first == (x to y) }
}
