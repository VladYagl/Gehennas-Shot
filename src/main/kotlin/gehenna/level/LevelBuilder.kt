package gehenna.level

import gehenna.utils.Point

interface LevelBuilder<T : Level> {
    fun build(): T
    fun withPrevious(level: Level): LevelBuilder<T>
    fun withBackPoint(point: Point): LevelBuilder<T>
}