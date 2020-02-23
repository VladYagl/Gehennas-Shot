package gehenna.factory

import gehenna.component.Gun
import gehenna.component.Inventory
import gehenna.component.Item
import gehenna.core.Entity
import gehenna.exceptions.GehennaException
import gehenna.utils.random
import java.io.Serializable

interface EntityMutator: Serializable {
    fun mutate(entity: Entity)
}

class GiveOneOf(private val items: ArrayList<Item>) : EntityMutator {
    override fun mutate(entity: Entity) {
        entity<Inventory>()?.add(items.random(random))
    }
}

class GiveGun(private val gun: Item) : EntityMutator {
    override fun mutate(entity: Entity) {
        val gun = gun.entity<Gun>() ?: throw GehennaException("Give Gun item must be a gun")
        entity<Inventory>()?.let {inventory ->
            inventory.add(gun.item)
            inventory.equip(gun)
        }
    }
}

class GiveItem(private val item: Item) : EntityMutator {
    override fun mutate(entity: Entity) {
        entity<Inventory>()?.add(item)
    }
}
