package gehenna

import gehenna.components.*

data class ActionResult(val time: Long, val succeeded: Boolean)

abstract class Action {
    abstract val time: Long
    abstract fun perform(): ActionResult

    protected fun end(): ActionResult = ActionResult(time, true)
}

data class Think(override val time: Long = 0) : Action() {
    override fun perform(): ActionResult = end()
}

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

data class Destroy(private val entity: Entity, override val time: Long = 0) : Action() {
    override fun perform(): ActionResult {
//        entity[gehenna.Position::class]?.level?.remove(entity)
        entity.clean()
        return end()
    }
}

//TODO: Maybe components should handle this logic
data class Collide(
    private val entity: Entity,
    private val victim: Entity,
    private val damage: Int,
    override val time: Long = 100
) : Action() {
    override fun perform(): ActionResult {
//        entity[gehenna.Position::class]?.level?.remove(entity)
        val health = victim[Health::class]
        victim[Logger::class]?.add("You were hit by ${entity.name} for $damage damage")
        health?.dealDamage(damage)
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
        entity[Logger::class]?.add("Your start ${effect::class.simpleName}")
        entity.add(effect)
        return end()
    }
}

//Maybe pass stairs?
data class ClimbStairs(private val entity: Entity, override val time: Long = 100) : Action() {
    override fun perform(): ActionResult {
        val pos = entity[Position::class]!!
        val stairs = pos.neighbors.firstOrNull { it.has(Stairs::class) }?.get(Stairs::class)
            ?: return ActionResult(0, false)
        if (stairs.pos == null) {
            val depth = if (pos.level is DungeonLevel) pos.level.depth + 1 else -1
            val level = DungeonLevel(5 * 8, 6 * 8, pos.level.factory, depth)
            level.init()
            stairs.pos = Position(stairs.entity, 2, 2, level)
        }
        val destination = stairs.pos!!
        pos.level.remove(entity)
        destination.level.spawn(entity, destination.x, destination.y)
        return end()
    }
}
