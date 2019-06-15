package gehenna.core

import gehenna.factory.Factory
import gehenna.factory.LevelPart
import gehenna.level.Level
import gehenna.level.LevelFactory

interface Context {
    val factory: Factory<Entity>
    val partFactory: Factory<LevelPart>
    val levelFactory: LevelFactory<out Level>

    val time: Long
    val player: Entity
    val levels: MutableList<Level>

    val actionQueue: ActionQueue
}