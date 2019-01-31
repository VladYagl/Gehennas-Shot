package gehenna.component.behaviour

import gehenna.action.Collide
import gehenna.action.Destroy
import gehenna.action.Move
import gehenna.component.Glyph
import gehenna.component.Health
import gehenna.component.Position
import gehenna.core.Action
import gehenna.core.ActionResult
import gehenna.core.Context
import gehenna.core.Entity
import gehenna.utils.Dir
import gehenna.utils.at
import gehenna.utils.on

//TODO: try some player seeking behaviour
data class BulletBehaviour(
        override val entity: Entity,
        var dir: Dir,
        private val damage: Int,
        override val speed: Int,
        override var waitTime: Long = 0
) : PredictableBehaviour() {

    override fun copy(entity: Entity): BulletBehaviour {
        return BulletBehaviour(entity, dir, damage, speed, waitTime)
    }

    fun dirChar(d: Dir = dir) = (130 + Dir.indexOf(d)).toChar()

    data class Bounce(private val entity: Entity, val dir: Dir) : Action(30) {
        fun bounce(pos: Position): Dir {
            val (x, y) = dir
            val (newx, newy) = pos + dir
            val h = pos.level.obstacle(newx - x at newy)
            val v = pos.level.obstacle(newx at newy - y)
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
            behaviour?.dir = bounce(entity.one())
            entity<Glyph>()?.char = behaviour?.dirChar() ?: 130.toChar()
            return end()
        }
    }

    override fun predictImpl(pos: Position, dir: Dir): Action {
        val obstacle = pos.level.obstacle(pos + dir)
        if (obstacle != null) {
            if (obstacle.has<Health>()) {
                return Collide(entity, obstacle, damage)
            }
            return Bounce(entity, dir)
        }
        return Move(entity, dir)
    }

    override suspend fun behave(): Action {
        if (lastResult?.succeeded == false) {
            return Destroy(entity)
        }
        val pos = entity.one<Position>()
        return predictImpl(pos, dir)
    }

    init {
        subscribe<Entity.Add> {
            entity<Glyph>()?.char = dirChar()
        }
    }
}