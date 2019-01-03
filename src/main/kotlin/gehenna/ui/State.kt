package gehenna.ui

import gehenna.ApplyEffect
import gehenna.ClimbStairs
import gehenna.Move
import gehenna.components.*
import gehenna.utils.Point
import java.awt.event.KeyEvent

abstract class State {
    open fun handleChar(char: Char): State = this
    open fun handleKey(keyCode: Int): State = this
}

private fun getDir(char: Char): Point? {
    return when (char) {
        '.', '5' -> 0 to 0
        'j', '2' -> 0 to +1
        'k', '8' -> 0 to -1
        'h', '4' -> -1 to 0
        'l', '6' -> +1 to 0
        'y', '7' -> -1 to -1
        'u', '9' -> +1 to -1
        'n', '3' -> +1 to +1
        'b', '1' -> -1 to +1
        else -> null
    }
}

abstract class Select<T>(protected val context: Context, private val items: List<T>, title: String) : State() {
    private val select = BooleanArray(items.size) { false }
    private val window = context.newWindow(100, 30)

    private fun updateItem(index: Int) {
        window.write("   ${if (select[index]) '+' else '-'} ${'a' + index}: ${items[index]}", 0, 1 + index)
    }

    init {
        window.write(title, 0, 0)
        for (i in 0 until items.size) {
            updateItem(i)
        }
        window.repaint()
    }

    override fun handleChar(char: Char): State {
        if (char in 'a'..'z') {
            val index = char - 'a'
            if (index < select.size) {
                select[index] = !select[index]
                updateItem(index)
                window.repaint()
            }
        }
        return this
    }

    override fun handleKey(keyCode: Int): State {
        return when (keyCode) {
            KeyEvent.VK_ENTER, KeyEvent.VK_SPACE -> {
                context.pane.remove(window.parent)
                return onAccept(items.filterIndexed { index, _ -> select[index] })
            }
            KeyEvent.VK_ESCAPE, KeyEvent.VK_CAPS_LOCK -> {
                context.pane.remove(window.parent)
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

class Normal(private val context: Context) : State() {
    override fun handleChar(char: Char): State {
        val dir = getDir(char)
        if (dir != null) {
            context.action(Move(context.game.player, dir))
        }
        return when (char) {
            'f' -> {
                val inventory = context.game.player[Inventory::class]!!
                val gun = inventory.all().mapNotNull { it.entity[Gun::class] }.firstOrNull()
                if (gun == null) {
                    context.log.add("You don't have any guns!")
                    return this
                }
                Aim(context, gun)
            }
            ',', 'g' -> {
                val pos = context.game.player[Position::class]!!
                val items = pos.neighbors.mapNotNull { it[Item::class] }
                if (items.isEmpty()) {
                    context.log.add("There is no items to pickup(((")
                    return this
                }
                Pickup(context, items)
            }
            'd' -> {
                Drop(context)
            }
            '>', '<' -> {
                context.action(ClimbStairs(context.game.player))
                this
            }
            else -> this
        }
    }
}

class Aim(private val context: Context, private val gun: Gun) : State() {
    init {
        context.log.add("Fire in which direction?")
    }

    override fun handleChar(char: Char): State {
        val dir = getDir(char)
        if (dir != null) {
            context.action(ApplyEffect(context.game.player, RunAndGun(context.game.player, dir, gun, 500)))
            return Normal(context)
        }
        return this
    }

    override fun handleKey(keyCode: Int): State {
        when (keyCode) {
            KeyEvent.VK_ESCAPE -> {
                context.log.add("Never mind")
                return Normal(context)
            }
        }
        return this
    }
}

class End(private val context: Context) : State() {
    override fun handleChar(char: Char): State {
        if (char == ' ') System.exit(0)
        return this
    }
}

//TODO: Why it's not an action??
class Pickup(context: Context, items: List<Item>) : Select<Item>(context, items, "Pick up what?") {
    override fun onAccept(items: List<Item>): State {
        val inventory = context.game.player[Inventory::class]!!
        items.forEach { item ->
            item.entity.remove(item.entity[Position::class]!!)
            inventory.add(item)
        }
        return Normal(context)
    }
}

class Drop(context: Context) : Select<Item>(context, context.game.player[Inventory::class]!!.all(), "Drop what?") {
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