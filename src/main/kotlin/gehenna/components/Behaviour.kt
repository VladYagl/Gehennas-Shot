package gehenna.components

import gehenna.Action
import gehenna.ActionResult
import gehenna.Collide
import gehenna.Destroy
import gehenna.Entity
import gehenna.Move
import gehenna.Think
import gehenna.utils.*
import kotlin.random.Random

abstract class Behaviour : WaitTime() {
    abstract val action: Action
    var lastResult: ActionResult? = null
}

data class ThinkUntilSet(override val entity: Entity) : Behaviour() {
    override var action: Action = Think()
        get() {
            val res = field
            field = Think()
            return res
        }
}

data class RandomBehaviour(override val entity: Entity) : Behaviour() {
    override val action: Action
        get() {
            val random = Random.Default
            return Move(entity, (random.nextInt(3) - 1) to (random.nextInt(3) - 1))
        }
}

data class MonsterBehaviour(override val entity: Entity) : Behaviour() {
    override val action: Action
        get() {
            if (lastResult?.succeeded == false) {
                return Move(entity, 0 to 0, 1000)
            }
            val pos = entity[Position::class]!!
            val next = pos.level.findPath(pos.x, pos.y)?.first()
            return if (next != null) {
                Move(entity, next.x - pos.x to next.y - pos.y)
            } else {
                val random = Random.Default
                Move(entity, (random.nextInt(3) - 1) to (random.nextInt(3) - 1))
            }
        }
}


data class BulletBehaviour(override val entity: Entity, private var dir: Pair<Int, Int>, override var time: Long = 0) :
    Behaviour() {
    override val action: Action
        get() {
            if (lastResult?.succeeded == false) {
                return Destroy(entity)
            }
            val (x, y) = dir
            val pos = entity[Position::class]!!
            val (newx, newy) = pos + dir
            val obstacle = pos.level.obstacle(newx, newy)
            val dir = if (obstacle == null) {
                x to y
            } else {
                if (obstacle.has(Health::class)) {
                    return Collide(entity, obstacle, 10) // TODO: Get damage from some component
                }
                val h = pos.level.obstacle(newx - x, newy)
                val v = pos.level.obstacle(newx, newy - y)
                if (h != null && v != null) {
                    -x to -y
                } else if (h != null) {
                    +x to -y
                } else if (v != null) {
                    -x to +y
                } else {
                    -x to -y
                }
            }
            this.dir = dir
            return Move(entity, dir)
        }
}
