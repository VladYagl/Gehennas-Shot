package gehenna.ui.state

import gehenna.component.*
import gehenna.component.behaviour.CharacterBehaviour
import gehenna.component.behaviour.PlayerBehaviour
import gehenna.level.Level
import gehenna.ui.Input
import gehenna.ui.UIContext
import gehenna.utils.*
import java.awt.Color

abstract class Target : State() {

    protected abstract val context: UIContext
    protected open val onlyVisible: Boolean = true
    protected open val autoAim: Boolean = false

    protected lateinit var cursor: Point
    protected var error: Int = 0
    protected val angle: Angle
        get() {
            val diff = cursor - context.player.one<Position>()
            return Angle(diff.x, diff.y, error)
        }

    protected val level: Level by lazy { context.player.one<Position>().level }
    protected val overlay by lazy { context.addOverlay() }

    private fun clean() {
        context.focusPlayer()
        context.removeOverlay(overlay)
    }

    protected fun isVisible(point: Point): Boolean {
        return context.player.all<Senses>().any { it.isVisible(point) }
    }

    protected open fun print() {
        context.moveFocus(cursor)
        overlay.clear()
        if (level.inBounds(cursor) && isVisible(cursor)) {
            context.log.addTemp("Here is: " + level.safeGet(cursor).packEntities().joinToString(separator = ", ") { it.name })
        } else {
            context.log.addTemp("You can't see this shit")
        }

        overlay.colors(point = cursor, fg = Color.gray, bg = Color(96, 32, 32))
    }

    override fun onChange() {
        val playerPos = context.player.one<Position>()
        cursor = if (!autoAim) {
            playerPos
        } else {
            var target: Point = playerPos
            context.player<PlayerBehaviour>()?.let { player ->
                context.player.all<Senses>().forEach { sense ->
                    sense.visitFov { entity, point ->
                        if (entity.any<CharacterBehaviour>()?.faction?.isEnemy(player.faction) == true && entity != context.player) {
                            if ((target equals playerPos) || (target - playerPos).max > (point - playerPos).max) {
                                target = point
                            }
                        }
                    }
                }
            }
            target
        }
        error = angle.findBestError(playerPos) ?: angle.defaultError
        print()
    }

    protected abstract fun select(): State

    final override fun handleInput(input: Input) = when (input) {
        is Input.Direction -> {
            cursor += input.dir
            error = angle.findBestError(context.player.one()) ?: angle.defaultError
            print()
            true
        }
        is Input.Increase -> {
            if (error < angle.maxError) {
                error += 1
                print()
            } else {
                context.log.addTemp("Can't move your hand any further")
            }
            true
        }
        is Input.Decrease -> {
            if (error > angle.minError) {
                error -= 1
                print()
            } else {
                context.log.addTemp("Can't move your hand further")
            }
            true
        }
        is Input.Run -> {
            cursor += input.dir * 5
            print()
            true
        }
        Input.Cancel -> {
            clean()
            context.changeState(Normal(context))
            true
        }
        Input.Accept, Input.Fire -> {
            if (level.inBounds(cursor) && (isVisible(cursor) || !onlyVisible)) {
                val state = select()
                if (state != this) clean()
                context.changeState(state)
                true
            } else {
                context.log.addTemp("Can't examine what you can't see")
                true
            }
        }
        else -> false
    }
}
