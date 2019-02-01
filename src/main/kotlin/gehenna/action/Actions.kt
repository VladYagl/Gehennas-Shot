package gehenna.action

import com.beust.klaxon.internal.firstNotNullResult
import gehenna.component.*
import gehenna.component.behaviour.BulletBehaviour
import gehenna.core.Action
import gehenna.core.ActionResult
import gehenna.core.Context
import gehenna.core.Entity
import gehenna.utils.Dir
import gehenna.utils.Dir.Companion.zero

object Think : Action(0) {
    override fun perform(context: Context): ActionResult = end()
}

data class Move(private val entity: Entity, val dir: Dir) : Action(100) {
    override fun perform(context: Context): ActionResult {
        return if (dir == zero) {
            end()
        } else {
            val pos = entity.one<Position>()
            if (pos.level.isWalkable(pos + dir)) {
                pos.level[pos + dir].firstOrNull { it.has<BulletBehaviour>() }?.let { bullet ->
                    entity<Logger>()?.add("You've perfectly dodged ${bullet.name}")
                }
                pos.move(pos + dir)
                end()
            } else {
                fail()
            }
        }
    }
}

object Wait : Action() {
    override fun perform(context: Context): ActionResult {
        return ActionResult(context.actionQueue.map { it.waitTime }.min()?.plus(1) ?: 0, true, log)
    }
}

data class Shoot(
        private val pos: Position,
        private val dir: Dir,
        private val bulletName: String,
        private val damage: Int,
        private val delay: Long,
        private val speed: Int,
        override var time: Long = 100L
) : Action() {
    override fun perform(context: Context): ActionResult {
        val bullet = context.factory.new(bulletName)
        pos.spawnHere(bullet)
        bullet.add(BulletBehaviour(bullet, dir, damage, speed, delay))
        return end()
    }
}

data class Destroy(private val entity: Entity) : Action(0, false) {
    override fun perform(context: Context): ActionResult {
        entity.clean()
        return end()
    }
}

data class Collide(val entity: Entity, val victim: Entity, val damage: Int) : Action(100, false) {
    override fun perform(context: Context): ActionResult {
        victim<Logger>()?.add("You were hit by ${entity.name} for $damage damage")
                ?: log("$victim were hit by $entity for $damage damage", victim())
        victim<Health>()?.dealDamage(damage)
        entity.clean()
        return end()
    }
}

data class ApplyEffect(private val entity: Entity, private val effect: Effect) : Action(100) {
    override fun perform(context: Context): ActionResult {
        entity<Logger>()?.add("Your start ${effect::class.simpleName}") ?: log("$entity starts $effect", entity())
        entity.add(effect)
        return end()
    }
}

data class ClimbStairs(private val entity: Entity) : Action(100) {
    //todo Maybe pass stairs?
    override fun perform(context: Context): ActionResult {
        val pos = entity.one<Position>()
        val stairs = pos.neighbors.firstNotNullResult { it<Stairs>() } ?: return fail()
        val destination = stairs.destination ?: context.newLevelBuilder()
                .withPrevious(pos.level)
                .withBackPoint(pos)
                .build().let { level ->
                    (level to level.startPosition).also { stairs.destination = it }
                }
        pos.level.remove(entity)
        destination.first.spawn(entity, destination.second)
        entity<Logger>()?.add("You've climbed stairs to " + stairs.destination?.first)
        return end()
    }
}

data class Pickup(private val items: List<Item>, private val inventory: Inventory) : Action(45) {
    override fun perform(context: Context): ActionResult {
        items.forEach { item ->
            item.entity.remove<Position>()
            inventory.add(item)
        }
        return end()
    }
}

data class Equip(private val item: Item?, private val inventory: Inventory) : Action(15) {
    override fun perform(context: Context): ActionResult {
        inventory.equip(item)
        return end()
    }
}

data class Drop(private val items: List<Item>, private val inventory: Inventory, private val pos: Position) :
        Action(45) {
    override fun perform(context: Context): ActionResult {
        items.forEach { item ->
            inventory.remove(item)
            pos.spawnHere(item.entity)
        }
        return end()
    }
}

data class UseDoor(private val door: Door, private val close: Boolean) : Action(100) {
    override fun perform(context: Context): ActionResult {
        if (door.closed == close) return fail()
        door.change(close)
        return end()
    }
}
