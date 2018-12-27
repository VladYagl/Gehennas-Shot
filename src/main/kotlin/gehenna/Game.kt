package gehenna

import gehenna.components.*

class Game(private val factory: EntityFactory) {
    val level = Level(5 * 8, 6 * 8, factory)
    val player = factory.newEntity("player")
    var gameTime: Long = 0

    init {
        level.init()
        level.spawn(player, 10, 10)
    }

    // TODO: Think about energy randomization / but maybe i don't really need one
    fun update() {
        val waiters = ComponentManager.all(WaitTime::class)
        val first = waiters.minBy { it.time }
        if (first != null) {
            val time = first.time
            gameTime += time
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
            first.time += result.time * 100 / (first.entity[Stats::class]?.speed ?: 100)
        }
    }
}