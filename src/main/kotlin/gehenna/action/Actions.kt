package gehenna.action

import gehenna.component.*
import gehenna.component.behaviour.ProjectileBehaviour
import gehenna.core.*
import gehenna.exception.GehennaException
import gehenna.ui.UIContext
import gehenna.utils.*
import gehenna.utils.Dir.Companion.zero
import kotlinx.coroutines.runBlocking

data class Think(override var time: Long) : Action(time) {
    override fun perform(context: UIContext): ActionResult = end()
}

data class Move(private val entity: Entity, val dir: Dir) : PredictableAction<Any>(oneTurn) {
    override fun predict(pos: Position, state: Any, glyph: Glyph): Triple<Point, Any, Glyph> {
        return pos + dir to state to glyph
    }

    override fun perform(context: UIContext): ActionResult {
        return if (dir == zero) {
            end()
        } else {
            val pos = entity.one<Position>()
            if (!pos.level.isBlocked(pos + dir) && (entity.has<Flying>() || pos.level.isWalkable(pos + dir))) {
                pos.move(pos + dir)
                //update weapon spread FIXME: maybe it should be in different place, but here is OK
                //TODO: maybe it should depend on a speed?
                entity<MainHandSlot>()?.gun?.applyWalkSpread()
                pos.level[pos].forEach {
                    it.any<PredictableBehaviour<*>>()?.let { behaviour ->
                        entity<Logger>()?.add("You've perfectly dodged ${behaviour.entity}")
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
    override fun perform(context: UIContext): ActionResult {
        return ActionResult(context.actionQueue.minOf { it.waitTime }.plus(1), true, results)
    }
}

data class Shoot(
        private val pos: Position,
        private val angle: Angle,
        private val gun: Gun,
        override var time: Long = oneTurn
) : Action() {
    override fun perform(context: UIContext): ActionResult {
        return if (gun.magazine.isEmpty()) {
            logFor(pos.entity, "Click! $_Actor_s ${gun.entity} is out of ammo")
            fail()
        } else {
            val ammo = gun.magazine.remove()
            ammo.entity.any<ShootFunc>()?.invoke(pos, angle, gun, ammo, context)
                ?: throw GehennaException("Ammo doesn't have ShootFunc!")
            gun.applyShootSpread()
            end()
        }
    }
}

data class Destroy(private val entity: Entity) : Action(0, false) {
    override fun perform(context: UIContext): ActionResult {
        entity.clean()
        return end()
    }
}

data class Collide(
        val entity: Entity,
        val victim: Entity,
        val damage: Dice,
        val destroyAction: Action = Destroy(entity)
) : PredictableAction<Any>(oneTurn, false) {
    override fun predict(pos: Position, state: Any, glyph: Glyph): Triple<Point, Any, Glyph> {
        return victim.one<Position>() to state to glyph
    }

    override fun perform(context: UIContext): ActionResult {
        victim<Position>()?.let { animate(it) { context.animateChar('X', it) } }

        val damageRoll = damage.roll()
        victim<Health>()?.let {
            logFor(victim, "$_Actor were hit by $entity for $damageRoll damage")
            it.dealDamage(damageRoll, this)
        }
        destroyAction.perform(context)

        return end()
    }
}

data class Attack(val entity: Entity, val dir: Dir) : Action(oneTurn) {
    override fun perform(context: UIContext): ActionResult {
        val pos = entity.one<Position>()
        entity.all<MeleeAttacker>().also {
            if (it.isEmpty()) return fail().also { logFor(entity, "$_Actor don't have a hand to attack") }
        }.forEach { attacker ->
            pos.level.obstacle(pos + dir)?.let { victim ->
                victim<Position>()?.let { it: Position -> animate(it) { context.animateChar('%', it) } }

                victim<Health>()?.let {
                    val damageRoll = attacker.damage.roll()
                    logFor(victim, "$_Actor were hit by $entity with a ${attacker.name} " +
                            "for $damageRoll damage")
                    it.dealDamage(damageRoll, this)
                }
            } ?: return fail().also {
                logFor(entity, "$_Actor swings your ${attacker.name} in open space")
            }
        }
        return end()
    }
}

data class ApplyEffect(
        private val effect: Effect,
        private val replace: Boolean = false,
        override var time: Long = oneTurn
) : Action() {
    override fun perform(context: UIContext): ActionResult {
        val entity = effect.entity
        if (!entity.has(effect::class)) {
            if (effect is Gun.BurstFire) {
                logFor(entity, "$_Actor fire[s] ${effect.gun.entity}")
            } else {
                logFor(entity, "$_Actor start[s] ${effect::class.simpleName}")
            }
            effect.attach()
        } else if (replace) {
            entity(effect::class)?.detach()
            effect.attach()
        }
        return end()
    }
}

data class ClimbStairs(private val entity: Entity, private val stairs: Stairs) : Action(oneTurn) {
    override fun perform(context: UIContext): ActionResult {
        val pos = entity.one<Position>()
        val destination = stairs.destination ?: run {
            runBlocking {
                context.loadingWindow("LOADING") {
                    context.levelFactory.new(pos.level, pos).let { (level, pos) ->
                        (level to pos).also { stairs.destination = it }
                    }
                }
            }
        }
        pos.level.remove(entity)
        destination.first.spawn(entity, destination.second)
        logFor(entity, "$_Actor climbed stairs to " + stairs.destination?.first)
        return end()
    }
}

//TODO: action time constants
data class Pickup(private val entity: Entity, private val items: List<Item>) : Action(45) {
    override fun perform(context: UIContext): ActionResult {
        items.forEach { item ->
            if (entity.one<Inventory>().add(item)) {
                item.entity.remove<Position>()
            }
        }
        logFor(entity, "$_Actor picked up: " + items.packStacks().joinToString { it.entity.name })
        return end()
    }
}

/**
 * Equip item in a slot, or unequip slot if item is null
 */
data class Equip(private val entity: Entity, private val slot: Slot, private val item: Item?) : Action(15) {
    override fun perform(context: UIContext): ActionResult {
        val old = slot.item
        slot.unequip()
        if (old != null) logFor(entity, "$_Actor unequipped a ${old.entity}")

        if (item != null) {
            slot.unequip()
            slot.equip(item)
            logFor(entity, "$_Actor have equipped a ${item.entity}")
        }

        return end()
    }
}

data class Drop(private val entity: Entity, private val items: List<Item>, private val pos: Position = entity.one()) :
        Action(45) {
    override fun perform(context: UIContext): ActionResult {
        items.forEach { item ->
            entity.one<Inventory>().remove(item)
            pos.spawnHere(item.entity)
        }
        logFor(entity, "$_Actor have dropped: ${items.packStacks().joinToString { it.entity.name }}")
        return end()
    }
}

data class UseDoor(private val door: Door, private val close: Boolean) : Action(oneTurn) {
    override fun perform(context: UIContext): ActionResult {
        if (door.closed == close) return fail()
        door.change(close)
        return end()
    }
}

data class Throw(
        private val pos: Position,
        private val angle: Angle,
        private val item: Item
) : Action(oneTurn) {
    override fun perform(context: UIContext): ActionResult {
        val entity = item.entity
        assert(!entity.has<Position>())
        item.remove()
        pos.spawnHere(entity)

        // 0.25 ~ 15 degrees, maybe later add something like throw skill to make it more accurate + faster
        val action = SimpleAction(addToQueue = false) {
            entity<ProjectileBehaviour>()?.detach()
        }
        val damage = item.entity<MeleeWeapon>()?.damage ?: Dice.SingleDice((item.volume + 5) / 5)
        ProjectileBehaviour(
                entity = entity,
                angle = random.nextAngle(angle, 0.2),
                speed = 500,
                distance = 10,
                bounce = false,
                waitTime = oneTurn / 10,
                collisionAction = { Collide(entity, it, damage, action) },
                maxDistanceAction = action
        ).attach()

        return end()
    }
}
