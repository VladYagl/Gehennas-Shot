data class ActionResult(val time: Long, val succeeded: Boolean)

abstract class Action(private val time: Long) {
    abstract fun perform(): ActionResult

    protected fun end(): ActionResult = ActionResult(time, true)
}

class Think : Action(0) {
    override fun perform(): ActionResult = end()
}

class Move(private val entity: Entity, private val dir: Pair<Int, Int>) : Action(10) {
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
                pos.level.move(entity, newx, newy)
                end()
            }
        }
    }
}

abstract class Behaviour(entity: Entity) : WaitTime(entity) {
    abstract val action: Action
}

class ThinkUntilSet(entity: Entity) : Behaviour(entity) {
    override var action: Action = Think()
        get() {
            val res = field
            field = Think()
            return res
        }
}
