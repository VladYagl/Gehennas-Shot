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

class Game(override val factory: Factory<Entity>, override val partFactory: Factory<LevelPart>) : Context {
    override lateinit var player: Entity
        private set
    private var globalTime: Long = 0
    override val time: Long get() = globalTime + (ComponentManager.waiters().firstOrNull()?.time ?: 0)

    override fun newLevelBuilder() = DungeonLevelBuilder()
            .withFactory(factory)
            .withPartFactory(partFactory)
            .withSize(5 * 8, 6 * 8)

    fun init() {
        player = factory.new("player")
        val level = newLevelBuilder().build()
        level.spawnAtStart(player)
        ComponentManager.update()
    }

    fun isPlayerNext(): Boolean = ComponentManager.waiters().firstOrNull() == player[ThinkUntilSet::class]

    // TODO: Think about energy randomization / but maybe i don't really need one
    fun update() {
        ComponentManager.waiters().firstOrNull()?.let { first ->
            val time = first.time
            globalTime += time
            ComponentManager.waiters().forEach {
                it.time -= time
                if (it is Effect) {
                    it.duration -= time
                    if (it.duration < 0) {
                        it.entity.remove(it)
                    }
                }
            }

            val result = when (first) {
                is Behaviour -> {
                    first.lastResult = first.action.perform(this)
                    first.lastResult!!
                }

                is Effect -> first.action.perform(this)

                else -> throw Exception("Unknown waiter: $first of type: ${first::class}")
            }
            first.time += scaleTime(result.time, first.entity[Stats::class]?.speed ?: 100)
            ComponentManager.update()
            val sight = player[Senses.Sight::class]
            sight?.visitFov { _, _, _ -> }
            result.logEntries.forEach { entry ->
                if (player.has(entry.sense)) {
                    when (entry.sense) {
                        Senses.Sight::class -> {
                            if (sight?.isVisible(entry.position!!.x, entry.position.y) == true) {
                                player[Logger::class]?.add(entry.text)
                            }
                        }
                    }
                }
            }
        }
    }
}