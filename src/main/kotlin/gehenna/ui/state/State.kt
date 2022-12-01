package gehenna.ui.state

import com.beust.klaxon.internal.firstNotNullResult
import gehenna.action.Throw
import gehenna.action.UseDoor
import gehenna.component.*
import gehenna.core.Entity
import gehenna.ui.Input
import gehenna.ui.TextItem
import gehenna.ui.UIContext
import gehenna.ui.panel.MenuPanel
import gehenna.ui.panel.SelectPanel
import gehenna.utils.Dir

abstract class State {
    open fun handleInput(input: Input): Boolean = false

    open fun onChange() {}

    companion object {
        fun create(context: UIContext): State = Normal(context)
    }
}

abstract class Direction(protected val context: UIContext) : State() {

    init {
        context.log.addTemp("In which direction?")
    }

    abstract fun onDir(dir: Dir): State
    open fun onCancel(): State {
        context.log.addTemp("Never mind")
        return Normal(context)
    }

    final override fun handleInput(input: Input) = when (input) {
        is Input.Direction -> {
            context.changeState(onDir(input.dir))
            true
        }
        Input.Cancel -> {
            context.changeState(onCancel())
            true
        }
        else -> false
    }
}

class UseDoor(context: UIContext, private val close: Boolean) : Direction(context) {
    override fun onDir(dir: Dir): State {
        val playerPos = context.player.one<Position>()
        playerPos.level[playerPos + dir].firstNotNullResult { it<Door>() }?.let { door ->
            context.action = UseDoor(door, close)
        } ?: context.log.addTemp("There is no door.")
        return Normal(context)
    }
}

class AimGun(override val context: UIContext, private val gun: Gun) : TargetLine() {

    override val onlyVisible: Boolean = false
    override val autoAim: Boolean = true

    override val spread: Double = gun.spread
    private val ammo: Ammo? = gun.magazine.firstOrNull()
    override val bounce: Boolean = ammo?.bounce ?: false
    override val speed = gun.speed + (ammo?.speed ?: 0)
    override val range = ammo?.range ?: 0
    override val projectileName: String = "bullet"

    override fun select(): State {
        context.action = gun.fire(context.player, angle)
        return Normal(context)
    }
}

class AimThrow(override val context: UIContext, private val item: Item) : TargetLine() {
    override val onlyVisible: Boolean = false
    override val autoAim: Boolean = true

    override val speed = 500
    override val range = 10
    override val spread = 0.2
    override val projectileName: String = item.entity.name

    override fun select(): State {
        context.action = Throw(context.player.one(), angle, item)
        return Normal(context)
    }
}

class Examine(override val context: UIContext) : Target() {

    private fun examine(entity: Entity) {
        context.addWindow(MenuPanel(100, 30, context.settings).apply {
            addItem(TextItem("This is a $entity"))
            setOnCancel { context.removeWindow(this) }
            entity.components.values.filterNot {
                it is Glyph || it is DirectionalGlyph || it is Position
            }.forEach { component ->
                addItem(TextItem("${component::class.simpleName}"))

                var depth = 1
                component.toString().split("(", "[").groupBy { it }.forEach {
                    depth += 2
                    if (it.value.size > 1) {
                        (this.items.last() as TextItem).line += " x${it.value.size}"
                    }
                    it.key.split(")", "]").forEach {
                        depth -= 1
                        it.split(", ", ",").filterNot {
                            it.contains("entity=$entity")
                        }.forEach {
                            addItem(TextItem("  ".repeat(depth) + it))
                        }
                    }
                }
            }
        })
    }

    override fun select(): State {
        val entities = level.safeGet(cursor).packEntities()
        when (entities.size) {
            0 -> context.log.addTemp("There is nothing to examine")
            1 -> examine(entities.first())
            else -> {
                context.addWindow(SelectPanel(context, entities, "There are: ") {
                    examine(it)
                })
            }
        }
        return this
    }
}
