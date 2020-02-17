package gehenna.action

import gehenna.component.*
import gehenna.component.behaviour.LineBulletBehaviour
import gehenna.component.behaviour.PredictableBehaviour
import gehenna.core.*
import gehenna.utils.*
import gehenna.utils.Dir.Companion.zero

data class Think(override var time: Long) : Action(time) {
    override fun perform(context: Context): ActionResult = end()
}

data class Move(private val entity: Entity, val dir: Dir) : PredictableAction<Any>(oneTurn) {
    override fun predict(pos: Position, state: Any, glyph: Glyph): Triple<Point, Any, Glyph> {
        return Triple(pos + dir, state, glyph)
    }

    override fun perform(context: Context): ActionResult {
        return if (dir == zero) {
            end()
        } else {
            val pos = entity.one<Position>()
            if (pos.level.isWalkable(pos + dir)) {
                pos.move(pos + dir)
                //update weapon spread FIXME: maybe it should be in different place, but here is OK
                //TODO: maybe it should depend on a speed?
                entity<Inventory>()?.gun?.entity?.invoke<Gun>()?.applyWalkSpread()
                pos.level[pos].forEach {
                    it.any<PredictableBehaviour<*>>()?.let { behaviour ->
                        entity<Logger>()?.add("You've perfectly dodged ${behaviour.entity.name}")
                    }
                }
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
        private val dir: LineDir,
        private val gun: Gun,
        override var time: Long = oneTurn
) : Action() {
    override fun perform(context: Context): ActionResult {
        val bullet = context.factory.new(gun.bullet)
        pos.spawnHere(bullet)
        val shootDir = if (gun.spread > 0) {
            (dir.angle + random.nextDouble(gun.spread) - random.nextDouble(gun.spread)).toLineDir(dir.errorShift)
        } else dir
        bullet.add(LineBulletBehaviour(bullet, shootDir, gun.damage, gun.speed, gun.bounce, gun.delay))
        bullet.add(DestroyTimer(bullet, gun.bulletTime))
        gun.applyShootSpread()
        return end()
    }
}

data class Destroy(private val entity: Entity) : Action(0, false) {
    override fun perform(context: Context): ActionResult {
        entity.clean()
        return end()
    }
}

data class Collide(val entity: Entity, val victim: Entity, val damage: Dice) : PredictableAction<Any>(oneTurn, false) {
    override fun predict(pos: Position, state: Any, glyph: Glyph): Triple<Point, Any, Glyph> {
        return Triple(victim.one<Position>(), state, glyph)
    }

    override fun perform(context: Context): ActionResult {
        val damageRoll = damage.roll()
        victim<Health>()?.let{
            logFor(victim, "$_Actor were hit by $entity for $damageRoll damage")
            it.dealDamage(damageRoll, this)
        }
        entity.clean()
        return end()
    }
}

data class ApplyEffect(
        private val entity: Entity,
        private val effect: Effect,
        private val replace: Boolean = false,
        override var time: Long = oneTurn
) : Action() {
    override fun perform(context: Context): ActionResult {
        if (!entity.has(effect::class)) {
            if (effect is Gun.BurstFire) {
                logFor(entity, "$_Actor fire[s] ${effect.gun.entity}")
            } else {
                logFor(entity, "$_Actor start[s] ${effect::class.simpleName}")
            }
            entity.add(effect)
        } else if (replace) {
            entity(effect::class)?.let {
                entity.remove(it)
            }
            entity.add(effect)
        }
        return end()
    }
}

data class ClimbStairs(private val entity: Entity, private val stairs: Stairs) : Action(oneTurn) {
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

data class UseDoor(private val door: Door, private val close: Boolean) : Action(oneTurn) {
    override fun perform(context: Context): ActionResult {
        if (door.closed == close) return fail()
        door.change(close)
        return end()
    }
}
