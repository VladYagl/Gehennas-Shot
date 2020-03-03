package gehenna.component

import gehenna.action.ApplyEffect
import gehenna.action.Shoot
import gehenna.core.*
import gehenna.ui.UIContext
import gehenna.utils.Dice
import gehenna.utils.FixedQueue
import gehenna.utils.LineDir
import gehenna.utils._Actor
import kotlin.math.max
import kotlin.math.min

data class Gun(
        override val entity: Entity,
        val ammoType: AmmoType,
        val magazineCapacity: Int,

        val damage: Dice,
        val speed: Int,
        val delay: Long,
        private val volume: Int,
        private val minSpread: Double = 0.0,
        private val maxSpread: Double = 0.0,
        private val shootSpread: Double = 0.0,
        private val walkSpread: Double = 0.0,
        private val spreadReduce: Double = 0.0,
        private val spreadReduceTick: Long = 10,
        private val burstCount: Int = 1,
        private val time: Long = 100,
        private val shootTime: Long = time
) : Component() {

    private val spreadReducer = object : Effect() {
        override val endless = true
        override val entity = this@Gun.entity

        override suspend fun action() = SimpleAction(spreadReduceTick) {
            decSpread(spreadReduce)
        }
    }
    val item = Item(entity, volume)

    val fullDamage get() = damage + (magazine.firstOrNull()?.damage ?: Dice.Const(0))

    override val children: List<Component> = listOf(item)
    private var curSpread = minSpread
    private var curWalkSpread: Double = 0.0

    var magazine: FixedQueue<Ammo> = FixedQueue(magazineCapacity)

    fun unload(actor: Entity, inventory: Inventory? = actor.one()): Action {
        return object : Action(15) {
            override fun perform(context: UIContext): ActionResult {
                if (magazine.size > 0) {
                    logFor(actor, "$_Actor unloaded ${magazine.first().entity}x${magazine.size} from ${this@Gun.entity}")
                } else {
                    logFor(actor, "${this@Gun.entity.toString().capitalize()} is Empty!")
                }
                while (magazine.size > 0) {
                    inventory?.add(magazine.remove().item)
                }
                return end()
            }
        }
    }

    fun load(actor: Entity, ammoStack: Collection<Ammo>, inventory: Inventory? = actor.one()): Action {
        return object : Action(15) {
            override fun perform(context: UIContext): ActionResult {
                var cnt = 0
                ammoStack.forEach { new ->
                    if (!magazine.isFull()) {
                        assert(new.type == ammoType)
                        inventory?.remove(new.item)
                        magazine.add(new)
                        cnt++
                    }
                }
                logFor(actor, "$_Actor loaded $cnt rounds to ${this@Gun.entity}")
                return end()
            }
        }
    }

    val spread: Double
        get() {
            return curSpread + curWalkSpread
        }

    fun applyShootSpread() {
        curSpread = min(curSpread + shootSpread, maxSpread)
        if (!spreadReducer.attached) spreadReducer.attach()
    }

    fun applyWalkSpread() {
        curWalkSpread = walkSpread
        if (!spreadReducer.attached) spreadReducer.attach()
    }

    fun decSpread(dec: Double) {
        curSpread = max(curSpread - dec, minSpread)
        curWalkSpread = max(curWalkSpread - dec, 0.0)
        if (spread == 0.0 && spreadReducer.attached) entity.remove(spreadReducer)
    }

    private fun action(actor: Entity, dir: LineDir) =
            Shoot(actor.one(), dir, this, time)

    data class BurstFire(private val actor: Entity, private val dir: LineDir, val gun: Gun) :
            RepeatAction<Shoot>(actor, gun.burstCount, { gun.action(actor, dir) })

    fun fire(actor: Entity, dir: LineDir): Action? {
        if (magazine.isEmpty()) return action(actor, dir) // return gun action to get no ammo message
        return ApplyEffect(BurstFire(actor, dir, this), true, shootTime)
    }
}
