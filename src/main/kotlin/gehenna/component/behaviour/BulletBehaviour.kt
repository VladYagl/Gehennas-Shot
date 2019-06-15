package gehenna.component.behaviour

import gehenna.action.Collide
import gehenna.action.Destroy
import gehenna.action.Move
import gehenna.component.Health
import gehenna.component.Position
import gehenna.component.Reflecting
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

    data class Bounce(private val entity: Entity, val dir: Dir) : Action(30) {
        fun bounce(pos: Position): Dir {
            val (x, y) = dir
            val (newx, newy) = pos + dir
            val h = pos.level.obstacle(newx - x at newy)?.has<Reflecting>() ?: false
            val v = pos.level.obstacle(newx at newy - y)?.has<Reflecting>() ?: false
            return if (h && v) {
                -x on -y
            } else if (h) {
                +x on -y
            } else if (v) {
                -x on +y
            } else {
                -x on -y
            }
        }

        override fun perform(context: Context): ActionResult {
            val behaviour = entity<BulletBehaviour>()
            behaviour?.dir = bounce(entity.one())
            return end()
        }
    }

    override fun predictImpl(pos: Position, dir: Dir): Action {
        val obstacle = pos.level.obstacle(pos + dir)
        if (obstacle != null) {
            if (obstacle.has<Reflecting>()) {
                return Bounce(entity, dir)
            }
            return Collide(entity, obstacle, damage)
        }
        return Move(entity, dir)
    }

    override suspend fun behave(): Action {
        if (lastResult?.succeeded == false) {
            return Destroy(entity)
        }
        return predictImpl(entity.one(), dir)
    }
}