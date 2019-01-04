package gehenna.ui

import gehenna.ApplyEffect
import gehenna.ClimbStairs
import gehenna.Move
import gehenna.components.*

abstract class State {
    open fun handleInput(input: Input): State = this

    companion object {
        fun create(context: Context): State = Normal(context)
    }
}

private abstract class Select<T>(protected val context: Context, private val items: List<T>, title: String) : State() {
    private val select = BooleanArray(items.size) { false }
    private val window = context.newWindow(100, 30)

    private fun updateItem(index: Int) {
        window.writeLine("   ${if (select[index]) '+' else '-'} ${'a' + index}: ${items[index]}", 1 + index)
    }

    init {
        window.writeLine(title, 0)
        for (i in 0 until items.size) {
            updateItem(i)
        }
        window.repaint()
    }

    override fun handleInput(input: Input): State {
        return when (input) {
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
    }

    abstract fun onAccept(items: List<T>): State
    open fun onCancel(): State {
        context.log.add("Never mind")
        return Normal(context)
    }
}

private class Normal(private val context: Context) : State() {

    override fun handleInput(input: Input): State {
        return when (input) {
            is Input.Direction -> {
                context.action(Move(context.game.player, input.dir))
                this
            }
            Input.Fire -> {
                val inventory = context.game.player[Inventory::class]!!
                val gun = inventory.all().mapNotNull { it.entity[Gun::class] }.firstOrNull()
                if (gun == null) {
                    context.log.add("You don't have any guns!")
                    return this
                }
                Aim(context, gun)
            }
            Input.Pickup -> {
                val pos = context.game.player[Position::class]!!
                val items = pos.neighbors.mapNotNull { it[Item::class] }
                if (items.isEmpty()) {
                    context.log.add("There is no items to pickup(((")
                    return this
                }
                Pickup(context, items)
            }
            Input.Drop -> {
                Drop(context)
            }
            Input.ClimbStairs -> {
                context.action(ClimbStairs(context.game.player))
                this
            }
            else -> this
        }
    }
}

private class Aim(private val context: Context, private val gun: Gun) : State() {
    init {
        context.log.add("Fire in which direction?")
    }

    override fun handleInput(input: Input): State {
        return when (input) {
            is Input.Direction -> {
                context.action(ApplyEffect(context.game.player, RunAndGun(context.game.player, input.dir, gun, 500)))
                Normal(context)
            }
            is Input.Cancel -> {
                context.log.add("Never mind")
                Normal(context)
            }
            else -> this
        }
    }
}

class End(private val context: Context) : State() {
    override fun handleInput(input: Input): State {
        if (input == Input.Accept) System.exit(0)
        return this
    }
}

//TODO: Why it's not an action??
private class Pickup(context: Context, items: List<Item>) : Select<Item>(context, items, "Pick up what?") {
    override fun onAccept(items: List<Item>): State {
        val inventory = context.game.player[Inventory::class]!!
        items.forEach { item ->
            item.entity.remove(item.entity[Position::class]!!)
            inventory.add(item)
        }
        return Normal(context)
    }
}

private class Drop(context: Context) : Select<Item>(context, context.game.player[Inventory::class]!!.all(), "Drop what?") {
    override fun onAccept(items: List<Item>): State {
        val pos = context.game.player[Position::class]!!
        val inventory = context.game.player[Inventory::class]!!
        items.forEach { item ->
            inventory.remove(item)
            pos.level.spawn(item.entity, pos.x, pos.y)
        }
        return Normal(context)
    }
}
