data class ActionResult(val time: Long, val succeeded: Boolean)

abstract class Action {
    abstract val time: Long
    abstract fun perform(): ActionResult

    protected fun end(): ActionResult = ActionResult(time, true)
}

data class Think(override val time: Long = 0) : Action() {
    override fun perform(): ActionResult = end()
}

//TODO: IMHO MOVE SHOULD NOT CHECK FOR EMPTY SLOTS, BUT OKAY IT COULD IF THIS PLACE IS UNKNOWN
data class Move(
    private val entity: Entity,
    val dir: Pair<Int, Int>,
    override val time: Long = 100
) : Action() {
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

data class Shoot(
    private val entity: Entity,
    private val dir: Pair<Int, Int>,
    override val time: Long = 100
) : Action() {
    override fun perform(): ActionResult {
        val pos = entity[Position::class]!!
        val bullet = pos.level.factory.newEntity("bullet")
        pos.level.spawn(bullet, pos.x, pos.y)
        bullet.add(BulletBehaviour(bullet, dir))
        return end()
    }
}

//TODO: Maybe components should handle this logic
class Collide(
    private val entity: Entity,
    private val victim: Entity,
    private val damage: Int,
    override val time: Long = 100
) : Action() {
    override fun perform(): ActionResult {
        entity[Position::class]?.level?.remove(entity)
        val health = victim[Health::class]
        if (health != null) {
            health.current -= damage
        }
        entity.clean()
        return end()
    }
}

data class ApplyEffect(
    private val entity: Entity,
    private val effect: Effect,
    override val time: Long = 100
) : Action() {
    override fun perform(): ActionResult {
        entity.add(effect)
        return end()
    }
}
