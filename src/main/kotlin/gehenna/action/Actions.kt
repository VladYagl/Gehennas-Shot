package gehenna.action

import gehenna.component.*
import gehenna.component.behaviour.BulletBehaviour
import gehenna.core.Action
import gehenna.core.ActionResult
import gehenna.core.Context
import gehenna.core.Entity
import gehenna.utils.Point

fun scaleTime(time: Long, speed: Int): Long {
    return time * 100 / speed
}

object Think : Action(0) {
    override fun perform(context: Context): ActionResult = end()
}

data class Move(private val entity: Entity, val dir: Point) : Action(100) {
    override fun perform(context: Context): ActionResult {
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

data class Shoot(private val pos: Position, private val dir: Point, private val bulletName: String) : Action(100) {
    override fun perform(context: Context): ActionResult {
        val bullet = context.factory.new(bulletName)
        pos.spawnHere(bullet)
        bullet.add(BulletBehaviour(bullet, dir, 80))
        return end()
    }
}

data class Destroy(private val entity: Entity) : Action(0) {
    override fun perform(context: Context): ActionResult {
        entity.clean()
        return end()
    }
}

data class Collide(val entity: Entity, val victim: Entity, val damage: Int) : Action(100) {
    override fun perform(context: Context): ActionResult {
        val health = victim[Health::class]
        victim[Logger::class]?.add("You were hit by ${entity.name} for $damage damage")
                ?: log("$victim were hit by $entity for $damage damage", victim[Position::class])
        health?.dealDamage(damage)
        entity.clean()
        return end()
    }
}

data class ApplyEffect(private val entity: Entity, private val effect: Effect) : Action(100) {
    override fun perform(context: Context): ActionResult {
        entity[Logger::class]?.add("Your start ${effect::class.simpleName}")
        entity.add(effect)
        return end()
    }
}

data class ClimbStairs(private val entity: Entity) : Action(100) {
    //todo Maybe pass stairs?
    override fun perform(context: Context): ActionResult {
        val pos = entity[Position::class]!!
        val stairs = pos.neighbors.firstOrNull { it.has(Stairs::class) }?.get(Stairs::class) ?: return fail()
        val destination = stairs.destination ?: context.newLevelBuilder()
                .withPrevious(pos.level)
                .withBackPoint(pos.point)
                .build().let { level ->
                    (level to level.startPosition).also { stairs.destination = it }
                }
        pos.level.remove(entity)
        destination.first.spawn(entity, destination.second)
        entity[Logger::class]?.add("You've climbed stairs to " + stairs.destination)
        return end()
    }
}

data class Pickup(private val items: List<Item>, private val inventory: Inventory) : Action(50) {
    override fun perform(context: Context): ActionResult {
        items.forEach { item ->
            item.entity.remove(item.entity[Position::class]!!)
            inventory.add(item)
        }
        return end()
    }
}

data class Drop(private val items: List<Item>, private val inventory: Inventory, private val pos: Position) : Action(50) {
    override fun perform(context: Context): ActionResult {
        items.forEach { item ->
            inventory.remove(item)
            pos.spawnHere(item.entity)
        }
        return end()
    }
}
