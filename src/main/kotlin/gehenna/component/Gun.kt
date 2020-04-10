package gehenna.component

import gehenna.action.ApplyEffect
import gehenna.action.Shoot
import gehenna.core.*
import gehenna.ui.UIContext
import gehenna.utils.Dice
import gehenna.utils.FixedQueue
import gehenna.utils.Angle
import gehenna.utils._Actor
import kotlin.math.max
import kotlin.math.min

data class Gun(
        override val entity: Entity,
        val magazineCapacity: Int,

        /**
         * Ammo type of a gun, Ammo must much this type to be loaded into the gun
         */
        val ammoType: AmmoType,

        /**
         * Gun damage which is added to firing projectile damage
         */
        val damage: Dice,

        /**
         * Gun projectile speed, which is added to firing projectile speed
         */
        val speed: Int,

        /**
         * Delay between shot and bullet making its first turn
         */
        val delay: Long,

        /**
         * Volume of gun Item, a child component
         */
        private val volume: Int,

        /**
         * Gun spread in idle state, measured in radians
         */
        private val minSpread: Double = 0.0,

        /**
         * Maximum of combined shoot and walk spread, measured in radians
         */
        private val maxSpread: Double = 0.0,

        /**
         * Spread added after one shot (radians)
         */
        private val shootSpread: Double = 0.0,

        /**
         * Spread added after one step (radians)
         */
        private val walkSpread: Double = 0.0,

        /**
         * Passive spread reduction amount
         */
        private val spreadReduce: Double = 0.0,

        /**
         * Amount of game ticks between spread reduction
         */
        private val spreadReduceTick: Long = 10,

        /**
         * Amount of bullets shot in one burst
         */
        private val burstCount: Int = 1,

        /**
         * Time that it takes to make one shot (not full burst)
         */
        private val time: Long = 100,

        /**
         * Time that it takes to apply burst effect
         */
        private val applyTime: Long = time
) : Component() {

    val item = Item(entity, volume)
    override val children: List<Component> = listOf(item)

    val fullDamage get() = damage + (magazine.firstOrNull()?.damage ?: Dice.Const(0))

    private var curSpread = minSpread
    private var curWalkSpread: Double = 0.0

    /**
     * Full gun spread in radians
     */
    val spread: Double get() = curSpread + curWalkSpread

    private val spreadReducer = object : Effect() {
        override val endless = true
        override val entity = this@Gun.entity

        override suspend fun action() = SimpleReturnAction(spreadReduceTick) {
            ActionResult(spreadReduceTick, true, addToQueue = decSpread(spreadReduce))
        }
    }

    var magazine: FixedQueue<Ammo> = FixedQueue(magazineCapacity)

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

    fun applyShootSpread() {
        curSpread = min(curSpread + shootSpread, maxSpread)
        if (!spreadReducer.attached)
            spreadReducer.attach()
    }

    fun applyWalkSpread() {
        curWalkSpread = walkSpread
        if (!spreadReducer.attached)
            spreadReducer.attach()
    }

    fun decSpread(dec: Double): Boolean {
        curSpread = max(curSpread - dec, minSpread)
        curWalkSpread = max(curWalkSpread - dec, 0.0)
        return if (spread == 0.0 && spreadReducer.attached) {
            spreadReducer.detach()
            false
        } else {
            true
        }
    }

    private fun action(actor: Entity, angle: Angle) =
            Shoot(actor.one(), angle, this, time)

    data class BurstFire(private val actor: Entity, private val angle: Angle, val gun: Gun) :
            RepeatAction<Shoot>(actor, gun.burstCount, { gun.action(actor, angle) })

    fun fire(actor: Entity, angle: Angle): Action? {
        if (magazine.isEmpty()) return action(actor, angle) // return gun action to get no ammo message
        return ApplyEffect(BurstFire(actor, angle, this), true, applyTime)
    }
}
