class Level(val width: Int, val height: Int, val factory: EntityFactory) {
    private val cells = Array(width) { Array(height) { HashSet<Entity>() } }

    fun spawn(entity: Entity, x: Int, y: Int) {
        cells[x, y].add(entity)
        val pos = Position(entity, x, y, this)
        entity.add(pos)
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

    fun obstacle(x: Int, y: Int): Entity? {
        return cells[x][y].firstOrNull { it[Obstacle::class]?.blockMove ?: false }
    }

    fun isBlocked(x: Int, y: Int): Boolean {
        return cells[x, y].any { it[Obstacle::class]?.blockMove ?: false }
    }

    private fun wall(x: Int, y: Int) {
        spawn(factory.newEntity("wall"), x, y)
    }

    private fun floor(x: Int, y: Int) {
        spawn(factory.newEntity("floor"), x, y)
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
