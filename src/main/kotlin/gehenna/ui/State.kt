package gehenna.ui

import com.beust.klaxon.internal.firstNotNullResult
import gehenna.action.ClimbStairs
import gehenna.action.Move
import gehenna.component.*
import gehenna.utils.Dir

abstract class State {
    open fun handleInput(input: Input): State = this

    companion object {
        fun create(context: UIContext): State = Normal(context)
    }
}

private abstract class Select<T>(protected val context: UIContext, private val items: List<T>, title: String) :
    State() {
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
        is Input.Char -> {
            if (input.char in 'a'..'z') {
                val index = input.char - 'a'
                if (index < select.size) {
                    select[index] = !select[index]
                    updateItem(index)
                    window.repaint()
                }
            }
            this
        }
        is Input.Accept -> {
            context.removeWindow(window)
            onAccept(items.filterIndexed { index, _ -> select[index] })
        }
        is Input.Cancel -> {
            context.removeWindow(window)
            onCancel()
        }
        else -> this
    }

    abstract fun onAccept(items: List<T>): State
    open fun onCancel(): State {
        context.log.add("Never mind")
        return Normal(context)
    }
}

private abstract class Direction(protected val context: UIContext) : State() {
    init {
        context.log.add("Fire in which direction?")
    }

    abstract fun onDir(dir: Dir): State
    open fun onCancel(): State {
        context.log.add("Never mind")
        return Normal(context)
    }

    override fun handleInput(input: Input) = when (input) {
        is Input.Direction -> onDir(input.dir)
        is Input.Cancel -> onCancel()
        else -> this
    }
}

private class Normal(private val context: UIContext) : State() {

    override fun handleInput(input: Input) = when (input) {
        is Input.Direction -> {
            val playerPos = context.player<Position>()!!
            val point = playerPos + input.dir
            // check for closed do
            playerPos.level[point.x, point.y].firstNotNullResult {
                if (it<Door>()?.closed == true) it<Door>() else null
            }?.let { door ->
                context.action = gehenna.action.UseDoor(door, close = false)
            } ?: run {
                context.action = Move(context.player, input.dir)
            }
            this
        }
        Input.Fire -> {
            val inventory = context.player<Inventory>()!!
            val gun = inventory.all().mapNotNull { it.entity.all<Gun>().firstOrNull() }.firstOrNull()
            if (gun == null) {
                context.log.add("You don't have any guns!")
                this
            } else Aim(context, gun)
        }
        Input.Pickup -> {
            val pos = context.player<Position>()!!
            val items = pos.neighbors.mapNotNull { it<Item>() }
            if (items.isEmpty()) {
                context.log.add("There is no items to pickup(((")
                this
            } else Pickup(context, items)
        }
        Input.Drop -> Drop(context)
        Input.ClimbStairs -> {
            context.action = ClimbStairs(context.player)
            this
        }
        Input.Open -> UseDoor(context, false)
        Input.Close -> UseDoor(context, true)
        Input.Console -> Console(context)
        else -> this
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
        val playerPos = context.player<Position>()!!
        val point = playerPos + dir
        playerPos.level[point.x, point.y].firstNotNullResult { it<Door>() }?.let { door ->
            context.action = gehenna.action.UseDoor(door, close)
        } ?: context.log.add("there is no door")
        return Normal(context)
    }
}

class End(private val context: UIContext) : State() {
    override fun handleInput(input: Input) = when (input) {
        Input.Accept, Input.Cancel -> {
            System.exit(0)
            this
        }
        else -> this
    }
}

//TODO: Why it's not an action??
private class Pickup(context: UIContext, items: List<Item>) : Select<Item>(context, items, "Pick up what?") {
    override fun onAccept(items: List<Item>): State {
        context.action = gehenna.action.Pickup(items, context.player()!!)
        return Normal(context)
    }
}

private class Drop(context: UIContext) : Select<Item>(context, context.player<Inventory>()!!.all(), "Drop what?") {
    override fun onAccept(items: List<Item>): State {
        context.action = gehenna.action.Drop(items, context.player()!!, context.player()!!) //wtf is this??? kill me pls
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
            this
        }
        is Input.Accept -> {
            try {
                val words = command.split(' ')
                when (words[0]) {
                    "spawn" -> context.player<Position>()!!.spawnHere(context.factory.new(words[1]))
                    "give" -> context.player<Inventory>()!!.add(context.factory.new(words[1])()!!)
                }
            } catch (e: Throwable) {
                context.printException(e)
            }
            context.removeWindow(window)
            Normal(context)
        }
        is Input.Backspace -> {
            command = command.dropLast(1)
            window.writeLine(command, 0, alignment = Alignment.left)
            this
        }
        is Input.Cancel -> Normal(context)
        else -> this
    }
}
