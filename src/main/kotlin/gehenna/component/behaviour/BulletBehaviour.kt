package gehenna.component.behaviour

import gehenna.core.Action
import gehenna.action.Collide
import gehenna.action.Destroy
import gehenna.action.Move
import gehenna.component.Glyph
import gehenna.component.Health
import gehenna.component.Position
import gehenna.core.ActionResult
import gehenna.core.Context
import gehenna.core.Entity
import gehenna.utils.Point
import gehenna.utils.directions

//TODO: try some player seeking behaviour
data class BulletBehaviour(
    override val entity: Entity,
    var dir: Point,
    private val damage: Int,
    override var time: Long = 0
) : PredictableBehaviour() {

    override fun copy(entity: Entity): BulletBehaviour {
        return BulletBehaviour(entity, dir, damage, time)
    }

    fun dirChar() = (130 + directions.indexOf(dir)).toChar()

    data class Bounce(private val entity: Entity, val dir: Point) : Action(30) {
        fun bounce(pos: Position): Point {
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

        override fun perform(context: Context): ActionResult {
            val behaviour = entity[BulletBehaviour::class]
            behaviour?.dir = bounce(entity[Position::class]!!)
            entity[Glyph::class]?.char = behaviour?.dirChar() ?: 130.toChar()
            return end()
        }
    }

    override fun onEvent(event: Entity.Event) {
        if (event == Entity.Add) {
            entity[Glyph::class]?.char = dirChar()
        }
    }

    override suspend fun action(): Action {
        if (lastResult?.succeeded == false) {
            return Destroy(entity)
        }
        val pos = entity[Position::class]!!
        val (newx, newy) = pos + dir
        val obstacle = pos.level.obstacle(newx, newy)
        if (obstacle != null) {
            if (obstacle.has(Health::class)) {
                return Collide(entity, obstacle, damage)
            }
            return Bounce(entity, dir)
        }
        return Move(entity, dir)
    }
}