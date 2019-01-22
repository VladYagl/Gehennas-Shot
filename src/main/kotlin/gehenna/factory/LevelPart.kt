package gehenna.factory

import gehenna.core.Entity
import gehenna.level.BasicLevel
import gehenna.utils.*

interface LevelPart {
    fun spawnTo(to: Point, level: BasicLevel)
    fun needs(point: Point): Boolean
    val size: Size
}

sealed class EntityConfig {
    data class Name(val name: String) : EntityConfig()
    data class Choice(val list: List<String>) : EntityConfig()
}

class FixedPart(private val entities: List<Pair<Point, EntityConfig>>, private val factory: Factory<Entity>) :
    LevelPart {
    override val size = Size(
        entities.map { it.first.x }.max() ?: 0,
        entities.map { it.first.y }.max() ?: 0
    )

    override fun spawnTo(to: Point, level: BasicLevel) {
        entities.forEach { (point, config) ->
            when (config) {
                is EntityConfig.Name -> level.spawn(factory.new(config.name), to + point)
                is EntityConfig.Choice -> level.spawn(factory.new(config.list.random(random)), to + point)
            }
        }
    }

    override fun needs(point: Point) = entities.any { it.first == point }
}
