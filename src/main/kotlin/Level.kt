class Level(val width: Int, val height: Int) {
    private val cells = Array(width) { Array(height) { HashSet<Entity>() } }

    fun spawn(entity: Entity, x: Int, y: Int) {
        cells[x, y].add(entity)
        Position(entity, x, y, this)
    }

    fun remove(entity: Entity) {
        val pos = entity[Position::class]!!
        cells[pos.x, pos.y].remove(entity)
        entity.remove(pos)
    }

    fun move(entity: Entity, x: Int, y: Int) {
        remove(entity)
        spawn(entity, x, y)
    }

    fun obstacles(x: Int, y: Int): List<Entity> {
        return cells[x][y].filter { it.has(Obstacle::class) }
    }

    fun isBlocked(x: Int, y: Int): Boolean {
        return cells[x, y].any { it[Obstacle::class]?.blockMove ?: false }
    }

    private fun wall(x: Int, y: Int) {
        val wall = Entity("Wall")
        spawn(wall, x, y)
        Glyph(wall, 178.toChar(), 10)
        Obstacle(wall, true, true)
    }

    private fun floor(x: Int, y: Int) {
        val floor = Entity("Floor")
        spawn(floor, x, y)
        Glyph(floor, '.', -1)
        Floor(floor)
    }

    init {
        for (x in 0 until width) {
            for (y in 0 until height) {
                floor(x, y)
                if (x == 0 || x == width - 1 || y == 0 || y == height - 1) {
                    wall(x, y)
                }
            }
        }
    }
}
