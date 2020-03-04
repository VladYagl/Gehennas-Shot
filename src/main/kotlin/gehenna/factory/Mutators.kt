package gehenna.factory

import gehenna.component.*
import gehenna.core.Component
import gehenna.core.Entity
import gehenna.utils.random
import java.io.Serializable
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

interface EntityMutator : Serializable {
    fun mutate(entity: Entity)
}

class Builder<T>(val build: () -> T)

class GiveOneOf(private val items: Builder<List<Item>>) : EntityMutator {
    override fun mutate(entity: Entity) {
        entity<Inventory>()?.add(items.build().random(random))
    }
}

class EquipItem(private val item: Builder<Item>, private val slot: KClass<out Component>) : EntityMutator {
    override fun mutate(entity: Entity) {
        assert(slot.isSubclassOf(Slot::class)) { "$slot is not a subclass of Slot!" }
        val newItem = item.build()
        entity<Inventory>()?.add(newItem)
        (entity.invoke(slot) as Slot).equip(newItem)
    }
}

class GiveItem(private val item: Builder<Item>, private val amount: Int = 1) : EntityMutator {
    override fun mutate(entity: Entity) {
        repeat(amount) {
            entity<Inventory>()?.add(item.build())
        }
    }
}
