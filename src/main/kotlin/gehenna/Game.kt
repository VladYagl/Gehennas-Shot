package gehenna

import gehenna.actions.scaleTime
import gehenna.components.*
import gehenna.components.behaviour.Behaviour
import gehenna.level.DungeonLevel

class Game(private val factory: EntityFactory) {
    lateinit var player: Entity
        private set
    var time: Long = 0

    fun init() {
        player = factory.newEntity("player")
        val level = DungeonLevel(5 * 8, 6 * 8, factory)
        level.init()
        level.spawn(player, 10, 10)
    }

    // TODO: Think about energy randomization / but maybe i don't really need one
    fun update() {
        val waiters = ComponentManager.all(WaitTime::class)
        val first = waiters.minBy { it.time }
        if (first != null) {
            val time = first.time
            this.time += time
            waiters.forEach {
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
            result.logEntries.forEach { entry ->
                if (player.has(entry.sense)) {
                    when (entry.sense) {
                        Senses.Sight::class -> {
                            if (player[Position::class]?.level?.isVisible(entry.position!!.x, entry.position.y) == true) {
                                player[Logger::class]?.add(entry.text)
                            }
                        }
                    }
                }
            }
        }
    }
}