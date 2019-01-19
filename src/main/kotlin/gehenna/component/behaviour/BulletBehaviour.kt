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
import gehenna.utils.*

//TODO: try some player seeking behaviour
data class BulletBehaviour(
    override val entity: Entity,
    var dir: Dir,
    private val damage: Int,
    override var waitTime: Long = 0
) : PredictableBehaviour() {

    override fun copy(entity: Entity): BulletBehaviour {
        return BulletBehaviour(entity, dir, damage, waitTime)
    }

    fun dirChar(d: Dir = dir) = (130 + Dir.indexOf(d)).toChar()

    data class Bounce(private val entity: Entity, val dir: Dir) : Action(30) {
        fun bounce(pos: Position): Dir {
            val (x, y) = dir
            val (newx, newy) = pos + dir
            val h = pos.level.obstacle(newx - x, newy)
            val v = pos.level.obstacle(newx, newy - y)
            return if (h != null && v != null) {
                -x on -y
            } else if (h != null) {
                +x on -y
            } else if (v != null) {
                -x on +y
            } else {
                -x on -y
            }
        }

        override fun perform(context: Context): ActionResult {
            val behaviour = entity<BulletBehaviour>()
            behaviour?.dir = bounce(entity()!!)
            entity<Glyph>()?.char = behaviour?.dirChar() ?: 130.toChar()
            return end()
        }
    }

    fun predict(pos: Position, dir: Dir): Action {
        val (newx, newy) = pos + dir
        val obstacle = pos.level.obstacle(newx, newy)
        if (obstacle != null) {
            if (obstacle.has<Health>()) {
                return Collide(entity, obstacle, damage)
            }
            return Bounce(entity, dir)
        }
        return Move(entity, dir)
    }

    override suspend fun action(): Action {
        if (lastResult?.succeeded == false) {
            return Destroy(entity)
        }
        val pos = entity<Position>()!!
        return predict(pos, dir)
    }

    init {
        subscribe<Entity.Add> {
            entity<Glyph>()?.char = dirChar()
        }
    }
}