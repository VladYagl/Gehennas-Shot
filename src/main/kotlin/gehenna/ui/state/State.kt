package gehenna.ui.state

import com.beust.klaxon.internal.firstNotNullResult
import gehenna.action.UseDoor
import gehenna.component.*
import gehenna.core.Entity
import gehenna.ui.Input
import gehenna.ui.TextItem
import gehenna.ui.UIContext
import gehenna.ui.panel.MenuPanel
import gehenna.ui.panel.SelectPanel
import gehenna.utils.Dir
import gehenna.utils.Angle

abstract class State {
    open fun handleInput(input: Input): Pair<State, Boolean> = this to false

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
        is Input.Direction -> onDir(input.dir) to true
        Input.Cancel -> onCancel() to true
        else -> this to false
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

class Aim(context: UIContext, private val gun: Gun) : Target(context, onlyVisible = false, autoAim = true, drawLine = true) {
    override fun select(): State {
        val diff = cursor - context.player.one<Position>()
        context.action = gun.fire(context.player, Angle(diff.x, diff.y, error))
        return Normal(context)
    }
}

class Examine(context: UIContext) : Target(context) {

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
