package gehenna.ui

import com.beust.klaxon.internal.firstNotNullResult
import gehenna.action.ClimbStairs
import gehenna.action.Move
import gehenna.action.Wait
import gehenna.component.*
import gehenna.component.behaviour.PlayerBehaviour
import gehenna.level.Level
import gehenna.utils.Dir
import gehenna.utils.Point
import kotlin.system.exitProcess

abstract class State {
    open fun handleInput(input: Input): Pair<State, Boolean> = this to false

    companion object {
        fun create(context: UIContext): State = Normal(context)
    }
}

private abstract class Select<T>(
        protected val context: UIContext,
        private val items: List<T>,
        title: String,
        private val selectMultiple: Boolean = true
) : State() {
    private val select = BooleanArray(items.size) { false }
    private val window = context.newWindow(100, 30)

    private fun updateItem(index: Int) {
        window.writeLine("   ${if (select[index]) '+' else '-'} ${'a' + index}: ${items[index]}", 1 + index)
    }

    init {
        window.writeLine(title, 0)
        repeat(items.size) { i ->
            updateItem(i)
        }
        window.repaint()
    }

    override fun handleInput(input: Input) = when (input) {
        //TODO: well actually I probably want to select items with hjkl
        is Input.Char -> {
            if (input.char in 'a'..'z') {
                val index = input.char - 'a'
                if (index < select.size) {
                    if (selectMultiple) {
                        select[index] = !select[index]
                        updateItem(index)
                        window.repaint()
                        this to true
                    } else {
                        context.removeWindow(window)
                        onAccept(listOf(items[index])) to true
                    }
                } else this to true
            } else this to false
        }
        is Input.Accept -> {
            context.removeWindow(window)
            onAccept(items.filterIndexed { index, _ -> select[index] }) to true
        }
        is Input.Cancel -> {
            context.removeWindow(window)
            onCancel() to true
        }
        else -> this to false
    }

    abstract fun onAccept(items: List<T>): State
    open fun onCancel(): State {
        context.log.addTemp("Never mind")
        return Normal(context)
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

    override fun handleInput(input: Input) = when (input) {
        is Input.Direction -> onDir(input.dir) to true
        is Input.Cancel -> onCancel() to true
        else -> this to false
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
                context.action = Move(context.player, input.dir) // todo: this is [Shit+.] == '>'  ???
            } else {
                context.player<PlayerBehaviour>()?.repeat(Move(context.player, input.dir))
            }
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
            val items = pos.neighbors.mapNotNull { it<Item>() }
            if (items.isEmpty()) {
                context.log.addTemp("There is no items to pickup(((")
                this to true
            } else Pickup(context, items) to true
        }
        Input.Drop -> Drop(context) to true
        Input.Use -> Use(context) to true
        Input.Equip -> Equip(context) to true
        Input.ClimbStairs -> {
            val pos = context.player.one<Position>()
            pos.neighbors.firstNotNullResult { it<Stairs>() }?.let { stairs ->
                context.action = ClimbStairs(context.player, stairs)
            } ?: context.log.addTemp("There is no stairs here")
            this to true
        }
        Input.Open -> UseDoor(context, false) to true
        Input.Close -> UseDoor(context, true) to true
        Input.Console -> Console(context) to true
        Input.Examine -> Examine(context) to true
        else -> this to false
    }
}

private class Aim(context: UIContext, private val gun: Gun) : Direction(context) {
    override fun onDir(dir: Dir): State {
        context.action = gun.fire(context.player, dir)
        return Normal(context)
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

class End(private val context: UIContext) : State() {
    override fun handleInput(input: Input) = when (input) {
        Input.Accept, Input.Cancel -> {
            exitProcess(0)
        }
        else -> this to false
    }
}

private class Pickup(context: UIContext, items: List<Item>) : Select<Item>(context, items, "Pick up what?") {
    override fun onAccept(items: List<Item>): State {
        context.action = gehenna.action.Pickup(context.player, items)
        return Normal(context)
    }
}

private class Drop(context: UIContext) : Select<Item>(context, context.player.one<Inventory>().contents, "Drop what?") {
    override fun onAccept(items: List<Item>): State {
        context.action = gehenna.action.Drop(context.player, items)
        return Normal(context)
    }
}

private class Equip(context: UIContext) : Select<Item?>(context, context.player.one<Inventory>().contents + (null as Item?), "Equip what?", false) {
    override fun onAccept(items: List<Item?>): State {
        context.action = gehenna.action.Equip(context.player, items.firstOrNull())
        return Normal(context)
    }
}

private class Use(context: UIContext) : Select<Item>(context, context.player.one<Inventory>().contents, "Use what?", false) {
    override fun onAccept(items: List<Item>): State {
        context.action = items.first().entity.any<Consumable>()?.apply(context.player)
        return Normal(context)
    }
}

private class Console(private val context: UIContext) : State() {
    private val window = context.newWindow(100, 2)
    private var command: String = ""

    override fun handleInput(input: Input) = when (input) {
        is Input.Char -> {
            command += input.char
            window.writeLine(command, 0, alignment = Alignment.left)
            this to true
        }
        is Input.Accept -> {
            try {
                val words = command.split(' ')
                when (words[0]) {
                    "spawn" -> context.player.one<Position>().spawnHere(context.factory.new(words[1]))
                    "give" -> context.player.one<Inventory>().add(context.factory.new(words[1])()!!)
                    "find" -> context.log.addTemp(
                            context.player.one<Position>().level.getAll().find { it.name == words[1] }?.invoke<Position>().toString()
                    )
                }
            } catch (e: Throwable) {
                context.printException(e)
            }
            context.removeWindow(window)
            Normal(context) to true
        }
        is Input.Backspace -> {
            command = command.dropLast(1)
            window.writeLine(command, 0, alignment = Alignment.left)
            this to true
        }
        is Input.Cancel -> {
            context.removeWindow(window)
            Normal(context) to true
        }
        else -> this to false
    }
}

private class Examine(private val context: UIContext) : State() {
    //todo: get cursor from ui
    var cursor: Point = context.player.one<Position>()
    val level: Level = context.player.one<Position>().level

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

    override fun handleInput(input: Input) = when (input) {
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
        is Input.Cancel -> {
            context.hideCursor()
            Normal(context) to true
        }
        else -> {
            this to false
        }
    }
}
