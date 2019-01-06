package gehenna.core

import gehenna.factory.Factory
import gehenna.factory.LevelPart
import gehenna.level.Level
import gehenna.level.LevelBuilder

interface Context {
    val factory: Factory<Entity>
    val partFactory: Factory<LevelPart>
    fun newLevelBuilder(): LevelBuilder<out Level>

    val time: Long
    val player: Entity
}