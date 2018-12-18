class Game {
    private val level = Level(11 * 8, 8 * 8)
    val player = Entity("Player")
    var gameTime: Long = 0

    init {
        level.spawn(player, 10, 10)
        Glyph(player, 2.toChar(), 100)
        Obstacle(player, blockView = false, blockMove = true)
        Stats(player, speed = 100)
        ThinkUntilSet(player)
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
                if (first.entity.name == "Bullet") {
                    println(first.time)
                }
            }
        }
    }
}