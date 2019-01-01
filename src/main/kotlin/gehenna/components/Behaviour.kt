package gehenna.components

import gehenna.*
import gehenna.utils.x
import gehenna.utils.y
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
    private var lastTarget: Position? = null
    override val action: Action
        get() {
            if (lastResult?.succeeded == false) {
                return Move(entity, 0 to 0, 1000)
            }
            val pos = entity[Position::class]!!
            pos.visitFov { entity, x, y ->
                if (entity.name == "player") {
                    lastTarget = entity[Position::class]
                }
            }
            val random = Random.Default
            if (lastTarget == null) return Move(entity, (random.nextInt(3) - 1) to (random.nextInt(3) - 1))
            if (lastTarget!!.x == pos.x) {
                if (lastTarget!!.y > pos.y) {
                    return Shoot(entity, 0 to 1, entity[Inventory::class]!!.all().mapNotNull { it.entity[Gun::class] }.first())
                }
            }
            val next = pos.level.findPath(pos.x, pos.y, lastTarget!!.x, lastTarget!!.y)?.firstOrNull()
            return if (next != null) {
                Move(entity, next.x - pos.x to next.y - pos.y)
            } else {
                Move(entity, (random.nextInt(3) - 1) to (random.nextInt(3) - 1))
            }
        }
}

//TODO: try some player seeking behaviour
data class BulletBehaviour(override val entity: Entity, private var dir: Pair<Int, Int>, override var time: Long = 0) :
        Behaviour() {

    private fun bounce(pos: Position): Pair<Int, Int> {
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
