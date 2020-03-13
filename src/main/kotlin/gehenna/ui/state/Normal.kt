package gehenna.ui.state

import com.beust.klaxon.internal.firstNotNullResult
import gehenna.action.*
import gehenna.component.*
import gehenna.component.behaviour.CharacterBehaviour
import gehenna.component.behaviour.PlayerBehaviour
import gehenna.ui.*
import gehenna.ui.panel.ConsolePanel
import gehenna.ui.panel.MenuPanel
import gehenna.ui.panel.MultiSelectPanel
import gehenna.ui.panel.SelectPanel
import gehenna.utils.Dir

class Normal(private val context: UIContext) : State() {

    private fun askForAmmo(ammoType: AmmoType, func: (Collection<Ammo>) -> Unit) {
        context.addWindow(SelectPanel(
                context,
                context.player.one<Inventory>().contents
                        .filter { it.entity<Ammo>()?.type == ammoType }
                        .packStacks(),
                title = "Load what?",
                toString = { it.entity.name }) { func(it.entity.one<ItemStack>().items.map { el -> el.entity.one<Ammo>() }) }
        )
    }

    private fun useStairs(): Pair<Normal, Boolean> {
        val pos = context.player.one<Position>()
        pos.neighbors.firstNotNullResult { it<Stairs>() }?.let { stairs ->
            context.action = ClimbStairs(context.player, stairs)
        } ?: context.log.addTemp("There is no stairs here")
        return this to true
    }

    override fun handleInput(input: Input) = when (input) {
        is Input.Direction -> {
            if (input.dir == Dir.zero) {
//                context.action = Wait
                context.action = Move(context.player, input.dir)
                this to true
            } else {
                val playerPos = context.player.one<Position>()

                // check for closed door
                playerPos.level[playerPos + input.dir].firstNotNullResult {
                    if (it<Door>()?.closed == true) it<Door>() else null
                }?.let { door ->
                    context.action = UseDoor(door, close = false)
                    return this to true
                }

                // check for melee attack
                playerPos.level[playerPos + input.dir].firstNotNullResult {
                    it.any<CharacterBehaviour>()?.faction?.let { faction ->
                        if (context.player.one<PlayerBehaviour>().faction.isEnemy(faction)) it else null
                    }
                }?.let { _ ->
                    context.action = Attack(context.player, input.dir)
                    return this to true
                }

                context.action = Move(context.player, input.dir)
                this to true
            }
        }
        is Input.Run -> {
            if (input.dir == Dir.zero) {
//                context.action = Move(context.player, input.dir)
                useStairs()
            } else {
                context.player<PlayerBehaviour>()?.walk(input.dir)
            }
            this to true
        }
        Input.Wait -> {
            context.action = Wait
            this to true
        }
        Input.Cancel -> {
            context.player<PlayerBehaviour>()?.cancel()
            this to true
        }
        Input.Fire -> {
            val gun = context.player.one<MainHandSlot>().gun
            if (gun == null) {
                context.log.addTemp("You don't have a gun equipped")
                this to true
            } else Aim(context, gun) to true
        }
        Input.Pickup -> {
            val pos = context.player.one<Position>()
            val neighbors = pos.neighbors.mapNotNull { it<Item>() }
            if (neighbors.isEmpty()) {
                context.log.addTemp("There is no items to pickup(((")
                this to true
            } else {
                context.addWindow(MultiSelectPanel(
                        context,
                        neighbors.packStacks(),
                        "Pick up what?",
                        toString = { it.entity.name }
                ) { items ->
                    context.action = Pickup(context.player, items.unpackStacks())
                })
                this to true
            }
        }
        is Input.Help -> {
            context.addWindow(MenuPanel(100, 30, context.settings).apply {
                setOnCancel { context.removeWindow(this) }
                addItem(TextItem("CONTROLS", alignment = Alignment.center))
                addItem(TextItem("Use Numpad / vim-keys / arrow-keys for movement", alignment = Alignment.center))
                addItem(TextItem("Use Shift + (Numpad / vim-keys / arrow-keys) for walking in direction", alignment = Alignment.center))
                GameInput(object : InputListener {
                    override fun onInput(input: Input): Boolean = false
                }).keyMap.forEach { (key, command) ->
                    val k = if (key.first() == '+') "Shift + " + key.drop(1) else key
                    println("$k --- ${command::class.simpleName}")
                    addItem(TextItem(
                            "    ${k.padEnd(10)} -- ${command::class.simpleName!!.toString().padStart(15)}",
                            alignment = Alignment.center
                    ))
                }
            })
            this to true
        }
        Input.Drop -> {
            context.addWindow(MultiSelectPanel(
                    context,
                    context.player.one<Inventory>().stacks,
                    "Drop what?",
                    toString = { it.entity.toString() }
            ) { items -> context.action = Drop(context.player, items.unpackStacks()) })
            this to true
        }
        Input.Use -> {
            context.addWindow(SelectPanel(
                    context,
                    context.player.one<Inventory>().items.filter { it.entity.any<Consumable>() != null }.packStacks(),
                    "Use what?",
                    toString = { it.entity.toString() }
            ) {
                context.action = it.unstack().entity.any<Consumable>()?.apply(context.player)
            })
            this to true
        }
        Input.Inventory -> {
            context.addWindow(SelectPanel(
                    context,
                    context.player.one<Inventory>().stacks,
                    "Inventory ${context.player.one<Inventory>().currentVolume}/${context.player.one<Inventory>().maxVolume}",
                    toString = { it.entity.toString() }
            ) { selectedItem ->
                context.addWindow(MenuPanel(100, 30, context.settings).apply {
                    setOnCancel { context.removeWindow(this) }
                    addItem(TextItem("${selectedItem.entity} -- vol.: ${selectedItem.volume}"))

                    selectedItem.unstack().entity.any<Consumable>()?.let { usableItem ->
                        addItem(ButtonItem("Use", {
                            context.action = usableItem.apply(context.player)
                            context.removeWindow(this)
                        }, 'u'))
                    }
                    selectedItem.entity<Gun>()?.let { gun ->
                        addItem(ButtonItem("Equip", {
                            context.action = Equip(context.player, context.player.one<MainHandSlot>(), gun.item)
                            context.removeWindow(this)
                        }, 'e'))
                        addItem(ButtonItem("Load", {
                            askForAmmo(gun.ammoType) {
                                context.action = gun.load(context.player, it)
                                context.removeWindow(this)
                            }
                        }, 'L'))
                        addItem(ButtonItem("Unload", {
                            context.action = gun.unload(context.player)
                            context.removeWindow(this)
                        }, 'U'))
                    }
                    addItem(ButtonItem("Drop", {
                        context.action = Drop(context.player, listOf(selectedItem))
                        context.removeWindow(this)
                    }, 'd'))
                    addItem(TextItem(""))
                    addItem(TextItem("  ** About **"))
                    selectedItem.entity.components.values.filterNot { it is Glyph }.forEach { component ->
                        addItem(TextItem("| ${component::class.simpleName}"))
                        component.toString().split(", ", ",", "(", ")").drop(1).filterNot {
                            it.contains("entity=")
                        }.forEach {
                            addItem(TextItem("|   $it"))
                        }
                    }
                })
            })
            this to true
        }
        Input.Equip -> {
            val select = SelectPanel(
                    context,
                    context.player.one<Inventory>().stacks + (null as Item?),
                    title = "Equip what?",
                    toString = { it?.entity?.toString() ?: " - unequip current" }) {
                context.action = Equip(context.player, context.player.one<MainHandSlot>(), it)
            }
            context.addWindow(select)
            this to true
        }
        Input.Reload -> {
            val gun = context.player.one<MainHandSlot>().gun
            if (gun == null) {
                context.log.addTemp("You don't have a gun to reload")
            } else {
                askForAmmo(gun.ammoType) { context.action = gun.load(context.player, it) }
            }
            this to true
        }
        Input.ClimbStairs -> {
            useStairs()
        }
        Input.Open -> UseDoor(context, false) to true
        Input.Close -> UseDoor(context, true) to true
        Input.Console -> {
            context.addWindow(ConsolePanel(context))
            this to true
        }
        Input.Examine -> Examine(context) to true
        else -> this to false
    }
}