package gehenna.level

import gehenna.Entity
import gehenna.Factory
import gehenna.utils.*

interface LevelPart {
    fun spawnTo(toX: Int, toY: Int, level: BasicLevel)
    fun needs(x: Int, y: Int): Boolean
    val width: Int
    val height: Int
}

sealed class EntityConfig {
    data class Name(val name: String) : EntityConfig()
    data class Choice(val list: List<String>) : EntityConfig()
}

class FixedPart(private val entities: List<Pair<Point, EntityConfig>>, private val factory: Factory<Entity>) : LevelPart {
    override val width = entities.map { it.first.x }.max() ?: 0
    override val height = entities.map { it.first.y }.max() ?: 0

    override fun spawnTo(toX: Int, toY: Int, level: BasicLevel) {
        entities.forEach { (point, config) ->
            when (config) {
                is EntityConfig.Name -> level.spawn(factory.new(config.name), toX + point.x, toY + point.y)
                is EntityConfig.Choice -> level.spawn(factory.new(random.choose(config.list)), toX + point.x, toY + point.y)
            }
        }
    }

    override fun needs(x: Int, y: Int) = entities.any { it.first == (x to y) }
}
