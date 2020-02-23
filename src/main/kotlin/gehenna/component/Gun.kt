package gehenna.component

import gehenna.action.ApplyEffect
import gehenna.action.Shoot
import gehenna.core.*
import gehenna.core.Action.Companion.oneTurn
import gehenna.utils.Dice
import gehenna.utils.LineDir
import gehenna.utils._Actor
import kotlin.math.max
import kotlin.math.min

data class Gun(
        override val entity: Entity,
        val ammoType: AmmoType,

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

    override val children: List<Component> = listOf(spreadReducer, item)
    private var curSpread = minSpread
    private var curWalkSpread: Double = 0.0

    var ammo: Ammo? = null
        private set

    fun unload(actor: Entity, inventory: Inventory? = actor.one()): Action {
        return object : Action(15) {
            override fun perform(context: Context): ActionResult {
                ammo?.item?.let {
                    logFor(actor, "$_Actor unloaded ${it.entity} from ${this@Gun.entity}")
                    inventory?.add(it)
                }
                ammo = null
                return end()
            }
        }
    }

    fun load(actor: Entity, new: Ammo, inventory: Inventory? = actor.one()): Action {
        assert(new.type == ammoType)
        return object : Action(15) {
            override fun perform(context: Context): ActionResult {
                inventory?.remove(new.item)
                logFor(actor, "$_Actor loaded ${new.entity} to ${this@Gun.entity}")
                ammo = new
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
    }

    fun applyWalkSpread() {
        curWalkSpread = walkSpread
    }

    fun decSpread(dec: Double) {
        curSpread = max(curSpread - dec, minSpread)
        curWalkSpread = max(curWalkSpread - dec, 0.0)
    }

    private fun action(actor: Entity, dir: LineDir) =
            Shoot(actor.one(), dir, this, time)

    data class BurstFire(private val actor: Entity, private val dir: LineDir, val gun: Gun) :
            RepeatAction<Shoot>(actor, gun.burstCount, { gun.action(actor, dir) })

    fun fire(actor: Entity, dir: LineDir): Action? {
        if (ammo == null || ammo?.amount == 0) return action(actor, dir) // return gun action to get no ammo message
        return ApplyEffect(actor, BurstFire(actor, dir, this), true, shootTime)
    }
}
