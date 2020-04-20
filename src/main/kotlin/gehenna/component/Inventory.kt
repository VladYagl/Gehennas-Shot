package gehenna.component

import gehenna.core.Action.Companion.oneTurn
import gehenna.core.Component
import gehenna.core.Entity
import gehenna.utils.Dice
import gehenna.utils.toDice

interface Slot {
    data class Equip(val slot : Slot) : Entity.Event
    object Unequip : Entity.Event

    var item: Item?

    fun equip(newItem: Item) {
        assert(item == null) { "Trying to equip $newItem in busy slot: $this" }
        assert(isValid(newItem))
        item = newItem
        newItem.slot = this
        newItem.entity.emit(Equip(this))
    }

    fun unequip() {
        item?.slot = null
        item?.entity?.emit(Unequip)
        item = null
    }

    fun isValid(item: Item): Boolean = true
}

data class Item(override val entity: Entity, val volume: Int, val stackable: Boolean = false) : Component() {
    var inventory: Inventory? = null
    var slot: Slot? = null

    /**
     * Unequips items if it is in a slot, others does nothing
     */
    fun unequip() {
        val slot = slot
        assert(slot == null || slot.item == this)
        slot?.unequip()
    }

    fun remove() {
        inventory?.remove(this)
    }

    init {
        subscribe<Entity.Remove> {
            unequip()
            inventory?.remove(this)
        }
    }
}

data class ItemStack(val items: Collection<Item>) : Component() {

    //TODO: check for memory leak
    override val entity: Entity = Entity(items.firstOrNull()?.entity.toString() + " x${items.size}")

    val item = Item(entity, items.sumBy { it.volume })
    override val children: List<Component> = listOf(item)
}

fun Collection<Item>.packStacks(): List<Item> {
    val list = this.toList()
    return list.filter { !it.stackable } + list.filter { it.stackable }.groupBy { it.entity.name }.map {
        ItemStack(it.value).also { stack -> stack.attach() }.item
    }
}

fun Collection<Item>.unpackStacks(): List<Item> {
    val list = this.toList()
    return list.filter { !it.entity.has<ItemStack>() } + list.mapNotNull { it.entity<ItemStack>()?.items }.flatten()
}

fun Collection<Entity>.packEntities(): List<Entity> {
    val list = this.toList()
    return list.filter { !it.has<Item>() } + list.mapNotNull { it<Item>() }.packStacks().map { it.entity }
}

fun Collection<Entity>.unpackEntities(): List<Entity> {
    val list = this.toList()
    return list.filter { !it.has<ItemStack>() } + list.mapNotNull { it<ItemStack>()?.items }.flatten().map { it.entity }
}

fun Entity.unstack(): Entity {
    val stack = this<ItemStack>()
    return stack?.items?.first()?.entity ?: this
}

fun Item.unstack(): Item {
    val stack = this.entity<ItemStack>()
    return stack?.items?.first() ?: this
}

data class Inventory(
        override val entity: Entity,
        val maxVolume: Int,
        val items: ArrayList<Item> = ArrayList()
) : Component() {
    var currentVolume = items.sumBy { it.volume }
        private set

    fun add(item: Item): Boolean {
        if (item.volume + currentVolume > maxVolume) {
            return false
        }
        currentVolume += item.volume
        items.add(item)
        item.inventory = this
        return true
    }

    fun remove(item: Item) {
        item.unequip()
        currentVolume -= item.volume
        items.remove(item)
        item.inventory = null
    }

    val contents
        get() = items.toList()

    val stacks
        get() = items.packStacks()

    init {
        //todo: Do you need death? You can call it on entity.remove?
        subscribe<Health.Death> {
            entity<Position>()?.let { pos ->
                items.forEach { item ->
                    pos.spawnHere(item.entity)
                }
            }
        }
    }
}

interface MeleeAttacker {
    val damage: Dice
    val time: Long
    val name: String
}

data class Teeth(override val entity: Entity, override val damage: Dice, override val time: Long) : Component(), MeleeAttacker {
    override val name: String = "sharp teeth"
}

data class MainHandSlot(override val entity: Entity, override var item: Item? = null) : Component(), Slot, MeleeAttacker {
    val gun: Gun? get() = item?.entity?.invoke()

    override val name: String
        get() = item?.entity?.toString() ?: "fist"

    override val damage: Dice
        get() {
            val item = item
            return if (item == null) {
                "d3".toDice() // TODO: Fist damage
            } else {
                item.entity<MeleeWeapon>()?.damage ?: Dice.SingleDice((item.volume + 5) / 5)
            }
        }

    override val time: Long
        get() {
            val item = item
            return if (item == null) {
                33 // TODO: Fist damage
            } else {
                item.entity<MeleeWeapon>()?.time ?: oneTurn
            }
        }
}

