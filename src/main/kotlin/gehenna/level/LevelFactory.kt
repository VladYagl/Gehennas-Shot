package gehenna.level

import gehenna.utils.Point

interface LevelFactory<T : Level> {
    fun new(previous: Level? = null, backPoint: Point? = null): T
}