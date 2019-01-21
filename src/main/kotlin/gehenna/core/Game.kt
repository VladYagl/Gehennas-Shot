package gehenna.core

import gehenna.action.scaleTime
import gehenna.component.Effect
import gehenna.component.Logger
import gehenna.component.Senses
import gehenna.component.Stats
import gehenna.component.behaviour.Behaviour
import gehenna.component.behaviour.ThinkUntilSet
import gehenna.factory.Factory
import gehenna.factory.LevelPart
import gehenna.level.DungeonLevelBuilder
import gehenna.level.StubLevelBuilder

class Game(override val factory: Factory<Entity>, override val partFactory: Factory<LevelPart>) : Context {
    override lateinit var player: Entity
        private set
    private var globalTime: Long = 0
    override val time: Long get() = globalTime + (ActionQueue.firstOrNull()?.waitTime ?: 0)

    override fun newLevelBuilder() = DungeonLevelBuilder()
//    override fun newLevelBuilder() = StubLevelBuilder()
        .withFactory(factory)
        .withPartFactory(partFactory)
        .withSize(8 * 8, 7 * 8)

    fun init() {
        player = factory.new("player")
        val level = newLevelBuilder().build()
        level.spawnAtStart(player)
        ActionQueue.update()
    }

    fun isPlayerNext(): Boolean = ActionQueue.firstOrNull() == player<ThinkUntilSet>()

    // TODO: Think about energy randomization / but maybe i don't really need one
    suspend fun update() {
        ActionQueue.firstOrNull()?.let { first ->
            val time = first.waitTime
            globalTime += time
            ActionQueue.toList().forEach {
                it.waitTime -= time
                if (it is Effect) {
                    it.duration -= time
                    if (it.duration <= 0) {
                        it.entity.remove(it)
//                        ActionQueue.remove(it)
                    }
                }
            }

            val result = first.action().perform(this)
            first.lastResult = result
            first.waitTime += scaleTime(result.time, first.entity<Stats>()?.speed ?: 100)

            ActionQueue.update()
            val sight = player<Senses.Sight>()
            sight?.visitFov { _, _ -> }
            result.logEntries.forEach { entry ->
                if (player.all<Senses>().any { entry.sense.isInstance(it) }) { // fixme
                    when (entry.sense) {
                        Senses.Sight::class -> {
                            if (sight?.isVisible(entry.position!!) == true) {
                                player<Logger>()?.add(entry.text)
                            }
                        }
                    }
                }
            }
        }
    }
}