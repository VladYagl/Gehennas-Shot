import java.beans.beancontext.BeanContext

data class ActionResult(val time: Long, val succeeded: Boolean)

abstract class Action(private val time: Long) {
    abstract fun perform(): ActionResult

    protected fun end(): ActionResult = ActionResult(time, true)
}

class Think : Action(0) {
    override fun perform(): ActionResult = end()
}

//TODO: IMHO MOVE SHOULD NOT CHECK FOR EMPTY SLOTS, BUT OKAY IT COULD IF THIS PLACE IS UNKNOWN
class Move(private val entity: Entity, private val dir: Pair<Int, Int>) : Action(100) {
    override fun perform(): ActionResult {
        val (x, y) = dir
        return if (x == 0 && y == 0) {
            end()
        } else {
            val pos = entity[Position::class]!!
            val newx = pos.x + x
            val newy = pos.y + y
            if (pos.level.isBlocked(newx, newy)) {
                ActionResult(0, false)
            } else {
                pos.move(newx, newy)
                end()
            }
        }
    }
}

//TODO Create objects through factories or builders, do some thing with it
class Shoot(private val entity: Entity, private val dir: Pair<Int, Int>) : Action(100) {
    override fun perform(): ActionResult {
        val pos = entity[Position::class]!!
        val bullet = pos.level.factory.newEntity("bullet")
        pos.level.spawn(bullet, pos.x, pos.y)
        bullet.add(BulletBehaviour(bullet, dir))
        return end()
    }
}

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

data class BulletBehaviour(override val entity: Entity, private var dir: Pair<Int, Int>, override var time: Long = 0) :
    Behaviour() {
    override val action: Action
        get() {
            val (x, y) = dir
            val pos = entity[Position::class]!!
            val (newx, newy) = pos + dir
            val obstacle = pos.level.obstacle(newx, newy)
            val dir = if (obstacle == null) {
                Pair(x, y)
            } else {
                val h = pos.level.obstacle(newx - x, newy)
                val v = pos.level.obstacle(newx, newy - y)
                if (h != null && v != null) {
                    Pair(-x, -y)
                } else if (h != null) {
                    Pair(+x, -y)
                } else if (v != null) {
                    Pair(-x, +y)
                } else {
                    Pair(-x, -y)
                }
            }
            this.dir = dir
            return Move(entity, dir)
        }
}
