package gehenna.ui

import gehenna.action.ClimbStairs
import gehenna.action.Move
import gehenna.component.Gun
import gehenna.component.Inventory
import gehenna.component.Item
import gehenna.component.Position

abstract class State {
    open fun handleInput(input: Input): State = this

    companion object {
        fun create(context: UIContext): State = Normal(context)
    }
}

private abstract class Select<T>(protected val context: UIContext, private val items: List<T>, title: String) : State() {
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

private class Normal(private val context: UIContext) : State() {

    override fun handleInput(input: Input) = when (input) {
        is Input.Direction -> {
            context.action = Move(context.player, input.dir)
            this
        }
        Input.Fire -> {
            val inventory = context.player[Inventory::class]!!
            val gun = inventory.all().mapNotNull { it.entity.all(Gun::class).firstOrNull() }.firstOrNull()
            if (gun == null) {
                context.log.add("You don't have any guns!")
                this
            } else Aim(context, gun)
        }
        Input.Pickup -> {
            val pos = context.player[Position::class]!!
            val items = pos.neighbors.mapNotNull { it[Item::class] }
            if (items.isEmpty()) {
                context.log.add("There is no items to pickup(((")
                this
            } else Pickup(context, items)
        }
        Input.Drop -> {
            Drop(context)
        }
        Input.ClimbStairs -> {
            context.action = ClimbStairs(context.player)
            this
        }
        else -> this
    }
}

private class Aim(private val context: UIContext, private val gun: Gun) : State() {
    init {
        context.log.add("Fire in which direction?")
    }

    override fun handleInput(input: Input) = when (input) {
        is Input.Direction -> {
            context.action = gun.fire(context.player, input.dir)
            Normal(context)
        }
        is Input.Cancel -> {
            context.log.add("Never mind")
            Normal(context)
        }
        else -> this
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
        val inventory = context.player[Inventory::class]!!
        context.action = gehenna.action.Pickup(items, inventory)
        return Normal(context)
    }
}

private class Drop(context: UIContext) : Select<Item>(context, context.player[Inventory::class]!!.all(), "Drop what?") {
    override fun onAccept(items: List<Item>): State {
        val pos = context.player[Position::class]!!
        val inventory = context.player[Inventory::class]!!
        context.action = gehenna.action.Drop(items, inventory, pos)
        return Normal(context)
    }
}
