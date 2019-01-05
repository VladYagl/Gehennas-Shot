package gehenna

import gehenna.actions.scaleTime
import gehenna.components.*
import gehenna.components.behaviour.Behaviour
import gehenna.components.behaviour.ThinkUntilSet
import gehenna.level.DungeonLevel
import gehenna.level.LevelPart

class Game(private val factory: Factory<Entity>, private val levelFactory: Factory<LevelPart>) {
    lateinit var player: Entity
        private set
    var time: Long = 0

    fun init() {
        player = factory.new("player")
        val level = DungeonLevel(5 * 8, 6 * 8, factory, levelFactory)
        level.init()
        level.spawn(player, 10, 10)
        ComponentManager.update()
    }

    fun isPlayerNext(): Boolean = ComponentManager.waiters().firstOrNull() == player[ThinkUntilSet::class]

    // TODO: Think about energy randomization / but maybe i don't really need one
    fun update() {
        val first = ComponentManager.waiters().firstOrNull()
        if (first != null) {
            val time = first.time
            this.time += time
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
                    first.lastResult = first.action.perform()
                    first.lastResult!!
                }

                is Effect -> first.action.perform()

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