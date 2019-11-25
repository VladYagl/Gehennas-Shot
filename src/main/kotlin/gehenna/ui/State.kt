package gehenna.ui

import com.beust.klaxon.internal.firstNotNullResult
import gehenna.action.ClimbStairs
import gehenna.action.Move
import gehenna.action.Wait
import gehenna.component.*
import gehenna.component.behaviour.PlayerBehaviour
import gehenna.core.Entity
import gehenna.level.Level
import gehenna.ui.panel.ConsolePanel
import gehenna.ui.panel.MenuPanel
import gehenna.ui.panel.MultiSelectPanel
import gehenna.ui.panel.SelectPanel
import gehenna.utils.Dir
import gehenna.utils.LineDir
import gehenna.utils.Point
import kotlin.system.exitProcess

abstract class State {
    open fun handleInput(input: Input): Pair<State, Boolean> = this to false

    companion object {
        fun create(context: UIContext): State = Normal(context)
    }
}

private abstract class Direction(protected val context: UIContext) : State() {

    init {
        context.log.addTemp("In which direction?")
    }

    abstract fun onDir(dir: Dir): State
    open fun onCancel(): State {
        context.log.addTemp("Never mind")
        return Normal(context)
    }

    final override fun handleInput(input: Input) = when (input) {
        is Input.Direction -> onDir(input.dir) to true
        Input.Cancel -> onCancel() to true
        else -> this to false
    }
}

private abstract class Target(protected val context: UIContext, protected val onlyVisible : Boolean = true) : State() {
    //todo: get cursor from ui
    private var cursor: Point = context.player.one<Position>()
    protected val level: Level = context.player.one<Position>().level

    private fun print() {
        context.setCursor(cursor)
        if (level.inBounds(cursor) && context.player.all<Senses>().any { it.isVisible(cursor) }) {
            context.log.addTemp("Here is: " + level.safeGet(cursor).joinToString(separator = ", ") { it.name })
        } else {
            context.log.addTemp("You can't see this shit")
        }
    }

    init {
        context.showCursor()
        print()
    }

    protected abstract fun select(point: Point) : State

    final override fun handleInput(input: Input) = when (input) {
        is Input.Direction -> {
            cursor += input.dir
            print()
            this to true
        }
        is Input.Run -> {
            cursor += input.dir * 5
            print()
            this to true
        }
        Input.Cancel -> {
            context.hideCursor()
            Normal(context) to true
        }
        Input.Accept -> {
            if (level.inBounds(cursor) && context.player.all<Senses>().any { it.isVisible(cursor) || !onlyVisible }) {
                val state = select(cursor)
                if (state != this) context.hideCursor()
                state to true
            } else {
                context.log.addTemp("Can't examine what you can't see")
                this to true
            }
        }
        else -> {
            this to false
        }
    }
}

private class Normal(private val context: UIContext) : State() {

    override fun handleInput(input: Input) = when (input) {
        is Input.Direction -> {
            if (input.dir == Dir.zero) {
                context.action = Wait
                this to true
            } else {
                val playerPos = context.player.one<Position>()
                // check for closed do
                playerPos.level[playerPos + input.dir].firstNotNullResult {
                    if (it<Door>()?.closed == true) it<Door>() else null
                }?.let { door ->
                    context.action = gehenna.action.UseDoor(door, close = false)
                } ?: run {
                    context.action = Move(context.player, input.dir)
                }
                this to true
            }
        }
        is Input.Run -> {
            if (input.dir == Dir.zero) {
                context.action = Move(context.player, input.dir)
            } else {
                context.player<PlayerBehaviour>()?.walk(input.dir)
            }
            this to true
        }
        Input.Cancel -> {
            context.player<PlayerBehaviour>()?.cancel()
            this to true
        }
        Input.Fire -> {
            val inventory = context.player.one<Inventory>()
            val gun = inventory.gun?.entity?.invoke<Gun>()
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
                context.addWindow(MultiSelectPanel(context, neighbors, { items ->
                    context.action = gehenna.action.Pickup(context.player, items)
                }, "Pick up what?"))
                this to true
            }
        }
        Input.Drop -> {
            context.addWindow(MultiSelectPanel(context, context.player.one<Inventory>().contents, { items ->
                context.action = gehenna.action.Drop(context.player, items)
            }, "Drop what?"))
            this to true
        }
        Input.Use -> {
            context.addWindow(SelectPanel(context, context.player.one<Inventory>().contents.filter { it.entity.has<Consumable>() }, {
                context.action = it.entity.any<Consumable>()?.apply(context.player)
            }, "Use what?"))
            this to true
        }
        Input.Inventory -> {
            context.addWindow(SelectPanel(context, context.player.one<Inventory>().contents, { selectedItem ->
                context.addWindow(MenuPanel(100, 30, context.settings).apply {
                    setOnCancel { context.removeWindow(this) }
                    addItem(TextItem("${selectedItem.entity} -- vol.: ${selectedItem.volume}"))
                    addItem(ButtonItem("Equip", {
                        context.action = gehenna.action.Equip(context.player, selectedItem)
                        context.removeWindow(this)
                    }, 'e'))
                    addItem(ButtonItem("Use", {
                        context.action = selectedItem.entity.any<Consumable>()?.apply(context.player)
                        context.removeWindow(this)
                    }, 'u'))
                    addItem(ButtonItem("Drop", {
                        context.action = gehenna.action.Drop(context.player, listOf(selectedItem))
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
            }, "Inventory ${context.player.one<Inventory>().currentVolume}/${context.player.one<Inventory>().maxVolume}"))
            this to true
        }
        Input.Equip -> {
            context.addWindow(SelectPanel(context, context.player.one<Inventory>().contents.filter { it.entity.has<Gun>() } + (null as Item?), {
                context.action = gehenna.action.Equip(context.player, it)
            }, "Equip what?"))
            this to true
        }
        Input.ClimbStairs -> {
            val pos = context.player.one<Position>()
            pos.neighbors.firstNotNullResult { it<Stairs>() }?.let { stairs ->
                context.action = ClimbStairs(context.player, stairs)
            } ?: context.log.addTemp("There is no stairs here")
            this to true
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

private class UseDoor(context: UIContext, private val close: Boolean) : Direction(context) {
    override fun onDir(dir: Dir): State {
        val playerPos = context.player.one<Position>()
        playerPos.level[playerPos + dir].firstNotNullResult { it<Door>() }?.let { door ->
            context.action = gehenna.action.UseDoor(door, close)
        } ?: context.log.addTemp("There is no door.")
        return Normal(context)
    }
}

private class Aim(context: UIContext, private val gun: Gun) : Target(context, onlyVisible = false) {
    override fun select(point: Point): State {
        val diff = point - context.player.one<Position>()
        context.action = gun.fire(context.player, LineDir(diff.x, diff.y))
        return Normal(context)
    }
}

private class Examine(context: UIContext) : Target(context) {
    private fun examine(entity: Entity) {
        context.addWindow(MenuPanel(100, 30, context.settings).apply {
            addItem(TextItem("This is a $entity"))
            setOnCancel { context.removeWindow(this) }
            entity.components.values.filterNot {
                it is Glyph || it is Inventory || it is DirectionalGlyph || it is Position
            }.forEach { component ->
                addItem(TextItem("${component::class.simpleName}"))
                component.toString().split(", ", ",", "(", ")").drop(1).filterNot {
                    it.contains("entity=")
                }.forEach {
                    addItem(TextItem("  $it"))
                }
            }
        })
    }

    override fun select(point: Point): State {
        val entities = level.safeGet(point)
        when (entities.size) {
            0 -> context.log.addTemp("There is nothing to examine")
            1 -> examine(entities.first())
            else -> {
                context.addWindow(SelectPanel(context, entities, {
                    examine(it)
                }, "There are: "))
            }
        }
        return this
    }
}
