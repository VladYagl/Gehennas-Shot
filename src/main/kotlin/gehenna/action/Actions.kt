package gehenna.action

import gehenna.component.*
import gehenna.component.behaviour.BulletBehaviour
import gehenna.core.Action
import gehenna.core.ActionResult
import gehenna.core.Context
import gehenna.core.Entity
import gehenna.utils.Dice
import gehenna.utils.Dir
import gehenna.utils.Dir.Companion.zero
import gehenna.utils._Actor
import gehenna.utils.minOf

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
        return ActionResult(context.actionQueue.minOf { it.waitTime }?.plus(1) ?: 0, true, log)
    }
}

data class Shoot(
        private val pos: Position,
        private val dir: Dir,
        private val bulletName: String,
        private val damage: Dice,
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

data class Collide(val entity: Entity, val victim: Entity, val damage: Dice) : Action(100, false) {
    override fun perform(context: Context): ActionResult {
        val damageRoll = damage.roll()
        logFor(victim, "$_Actor were hit by $entity for $damageRoll damage")
        victim<Health>()?.dealDamage(damageRoll)
        entity.clean()
        return end()
    }
}

data class ApplyEffect(private val entity: Entity, private val effect: Effect) : Action(100) {
    override fun perform(context: Context): ActionResult {
        logFor(entity, "$_Actor start[s] ${effect::class.simpleName}")
        entity.add(effect)
        return end()
    }
}

data class ClimbStairs(private val entity: Entity, private val stairs: Stairs) : Action(100) {
    override fun perform(context: Context): ActionResult {
        val pos = entity.one<Position>()
        val destination = stairs.destination ?: context.levelFactory.new(pos.level, pos).let { (level, pos) ->
            (level to pos).also { stairs.destination = it }
        }
        pos.level.remove(entity)
        destination.first.spawn(entity, destination.second)
        logFor(entity, "$_Actor climbed stairs to " + stairs.destination?.first)
        return end()
    }
}

data class Pickup(private val entity: Entity, private val items: List<Item>) : Action(45) {
    override fun perform(context: Context): ActionResult {
        items.forEach { item ->
            item.entity.remove<Position>()
            entity.one<Inventory>().add(item)
        }
        logFor(entity, "$_Actor picked up: " + items.joinToString { it.entity.name })
        return end()
    }
}

data class Equip(private val entity: Entity, private val item: Item?) : Action(15) {
    override fun perform(context: Context): ActionResult {
        val inventory = entity.one<Inventory>()
        val old = inventory.gun
        inventory.equip(item)
        if (item != null) {
            logFor(entity, "$_Actor have equipped a ${item.entity.name}")
        } else {
            logFor(entity, "$_Actor unequipped a ${old?.entity?.name}")
        }
        return end()
    }
}

data class Drop(private val entity: Entity, private val items: List<Item>, private val pos: Position = entity.one()) :
        Action(45) {
    override fun perform(context: Context): ActionResult {
        items.forEach { item ->
            entity.one<Inventory>().remove(item)
            pos.spawnHere(item.entity)
        }
        logFor(entity, "$_Actor have dropped: ${items.joinToString { it.entity.name }}")
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
