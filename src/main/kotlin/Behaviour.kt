import kotlin.random.Random

abstract class Behaviour : WaitTime() {
    abstract val action: Action
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
            val pos = entity[Position::class]!!
            val next = pos.level.findPath(pos.x, pos.y)?.first()
            return if (next != null) {
                Move(entity, next.first - pos.x to next.second - pos.y)
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
