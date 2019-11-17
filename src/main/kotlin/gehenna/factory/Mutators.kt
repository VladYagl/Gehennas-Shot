package gehenna.factory

import gehenna.component.Inventory
import gehenna.component.Item
import gehenna.core.Entity
import gehenna.utils.random
import java.io.Serializable

interface EntityMutator: Serializable {
    fun mutate(entity: Entity);
}

class GiveOneOf(private val items: ArrayList<Item>) : EntityMutator {
    override fun mutate(entity: Entity) {
        entity<Inventory>()?.add(items.random(random))
    }
}

class GiveGun(private val gun: Item) : EntityMutator {
    override fun mutate(entity: Entity) {
        entity<Inventory>()?.equip(gun)
    }
}

class GiveItem(private val item: Item) : EntityMutator {
    override fun mutate(entity: Entity) {
        entity<Inventory>()?.add(item)
    }
}
