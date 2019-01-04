package gehenna.actions

import gehenna.Entity
import gehenna.components.*
import gehenna.components.behaviour.BulletBehaviour
import gehenna.level.DungeonLevel
import gehenna.utils.Point

fun scaleTime(time: Long, speed: Int): Long {
    return time * 100 / speed
}

data class Think(override val time: Long = 0) : Action() {
    override fun perform(): ActionResult = end()
}

data class Move(
        private val entity: Entity,
        val dir: Point,
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
                fail()
            } else {
                pos.level[newx, newy].firstOrNull { it.has(BulletBehaviour::class) }?.let { bullet ->
                    entity[Logger::class]?.add("You've perfectly dodged ${bullet.name}")
                }
                pos.move(newx, newy)
                end()
            }
        }
    }
}

data class Shoot(
        private val entity: Entity,
        private val dir: Point,
        private val gun: Gun,
        override val time: Long = 100
) : Action() {
    override fun perform(): ActionResult {
        val pos = entity[Position::class]!!
        val bullet = pos.level.factory.newEntity(gun.bullet)
        pos.level.spawn(bullet, pos.x, pos.y)
        bullet.add(BulletBehaviour(bullet, dir, 80))
        return end()
    }
}

data class Destroy(private val entity: Entity, override val time: Long = 0) : Action() {
    override fun perform(): ActionResult {
        entity.clean()
        return end()
    }
}

//TODO: Maybe components should handle this logic
data class Collide(
        val entity: Entity,
        val victim: Entity,
        val damage: Int,
        override val time: Long = 100
) : Action() {
    override fun perform(): ActionResult {
        val health = victim[Health::class]
        victim[Logger::class]?.add("You were hit by ${entity.name} for $damage damage")
                ?: log("$victim were hit by $entity for $damage damage", victim[Position::class])
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
        val stairs = pos.neighbors.firstOrNull { it.has(Stairs::class) }?.get(Stairs::class) ?: return fail()
        if (stairs.pos == null) {
            val depth = if (pos.level is DungeonLevel) pos.level.depth + 1 else -1
            val level = DungeonLevel(5 * 8, 6 * 8, pos.level.factory, depth)
            level.init()
            stairs.pos = Position(stairs.entity, 2, 2, level)
        }
        val destination = stairs.pos!!
        pos.level.remove(entity)
        destination.level.spawn(entity, destination.x, destination.y)
        entity[Logger::class]?.add("You've climbed stairs to " + stairs.pos!!.level)
        return end()
    }
}
