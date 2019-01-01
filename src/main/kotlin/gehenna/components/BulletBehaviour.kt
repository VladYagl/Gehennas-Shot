package gehenna.components

import gehenna.*
import gehenna.utils.Point

//TODO: try some player seeking behaviour
data class BulletBehaviour(override val entity: Entity, private var dir: Point, override var time: Long = 0) : PredictableBehaviour() {

    override fun copy(entity: Entity): BulletBehaviour {
        return BulletBehaviour(entity, dir, time)
    }

    private fun bounce(pos: Position): Point {
        val (x, y) = dir
        val (newx, newy) = pos + dir
        val h = pos.level.obstacle(newx - x, newy)
        val v = pos.level.obstacle(newx, newy - y)
        return if (h != null && v != null) {
            -x to -y
        } else if (h != null) {
            +x to -y
        } else if (v != null) {
            -x to +y
        } else {
            -x to -y
        }
    }

    override val action: Action
        get() {
            if (lastResult?.succeeded == false) {
                return Destroy(entity)
            }
            val pos = entity[Position::class]!!
            val (newx, newy) = pos + dir
            val obstacle = pos.level.obstacle(newx, newy)
            if (obstacle != null) {
                if (obstacle.has(Health::class)) {
                    return Collide(entity, obstacle, 10) // TODO: Get damage from some component
                }
                this.dir = bounce(pos)
            }
            return Move(entity, dir)
        }
}