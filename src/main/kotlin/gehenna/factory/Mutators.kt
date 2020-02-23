package gehenna.factory

import gehenna.component.*
import gehenna.core.Component
import gehenna.core.Entity
import gehenna.exceptions.GehennaException
import gehenna.utils.random
import java.io.Serializable
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

interface EntityMutator : Serializable {
    fun mutate(entity: Entity)
}

class GiveOneOf(private val items: ArrayList<Item>) : EntityMutator {
    override fun mutate(entity: Entity) {
        entity<Inventory>()?.add(items.random(random))
    }
}

class EquipItem(private val item: Item, private val slot: KClass<out Component>) : EntityMutator {
    override fun mutate(entity: Entity) {
        assert(slot.isSubclassOf(Slot::class)) { "$slot is not a subclass of Slot!" }
        entity<Inventory>()?.add(item)
        (entity.invoke(slot) as Slot).equip(item)
    }
}

class GiveItem(private val item: Item) : EntityMutator {
    override fun mutate(entity: Entity) {
        entity<Inventory>()?.add(item)
    }
}
