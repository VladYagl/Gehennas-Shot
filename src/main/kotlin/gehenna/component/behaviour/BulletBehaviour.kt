package gehenna.component.behaviour

import gehenna.action.Collide
import gehenna.action.Move
import gehenna.component.Position
import gehenna.component.Reflecting
import gehenna.core.*
import gehenna.utils.*

//TODO: try some player seeking behaviour
data class BulletBehaviour(
        override val entity: Entity,
        override var dir: Dir,
        private val damage: Dice,
        override val speed: Int,
        override var waitTime: Long = 0
) : PredictableBehaviour() {

    override fun copy(entity: Entity): BulletBehaviour {
        return BulletBehaviour(entity, dir, damage, speed, waitTime)
    }

    data class Bounce(private val entity: Entity, val dir: Dir) : PredictableAction(30) {
        override fun predictDir(position: Position): Dir {
            val (x, y) = dir
            val (newx, newy) = position + dir
            val h = position.level.obstacle(newx - x at newy)?.has<Reflecting>() ?: false
            val v = position.level.obstacle(newx at newy - y)?.has<Reflecting>() ?: false
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

        override fun predict(pos: Position): Point = pos

        override fun perform(context: Context): ActionResult {
            val behaviour = entity<BulletBehaviour>()
            behaviour?.dir = predictDir(entity.one())
            return end()
        }
    }

    override fun predictImpl(pos: Position, dir: Dir): PredictableAction {
        val obstacle = pos.level.obstacle(pos + dir)
        if (obstacle != null) {
            if (obstacle.has<Reflecting>()) {
                return Bounce(entity, dir)
            }
            return Collide(entity, obstacle, damage)
        }
        return Move(entity, dir)
    }
}