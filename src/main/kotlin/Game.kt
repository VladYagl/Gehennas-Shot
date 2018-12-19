class Game(private val factory: EntityFactory) {
    private val level = Level(11 * 8, 8 * 8, factory)
    val player = factory.newEntity("player")
    var gameTime: Long = 0

    init {
        level.spawn(player, 10, 10)
    }

    fun update() {
        val waiters = ComponentManager.all(WaitTime::class)
        val first = waiters.minBy { it.time }
        if (first != null) {
            val time = first.time
            gameTime += time
            waiters.forEach { it.time -= time }

            if (first is Behaviour) {
                val result = first.action.perform()
                first.time += result.time * 100 / (first.entity[Stats::class]?.speed ?: 100)
            }
        }
    }
}