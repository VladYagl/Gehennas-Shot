package gehenna.ui.state

import gehenna.component.*
import gehenna.component.behaviour.CharacterBehaviour
import gehenna.component.behaviour.PlayerBehaviour
import gehenna.core.Action
import gehenna.core.Behaviour
import gehenna.exception.GehennaException
import gehenna.level.Level
import gehenna.ui.EMPTY_CHAR
import gehenna.ui.Input
import gehenna.ui.UIContext
import gehenna.utils.*
import java.awt.Color
import kotlin.random.Random

abstract class Target(
        protected val context: UIContext,
        protected val onlyVisible: Boolean = true,
        protected val autoAim: Boolean = false,
        protected val drawLine: Boolean = false)
    : State() {

    protected var cursor: Point
    protected var error: Int = 0
    protected val angle: Angle
        get() {
            val diff = cursor - context.player.one<Position>()
            return Angle(diff.x, diff.y, error)
        }

    protected val level: Level = context.player.one<Position>().level
    protected val overlay = context.addOverlay()

    private fun clean() {
        context.focusPlayer()
        context.removeOverlay(overlay)
    }

    private fun isVisible(point: Point): Boolean {
        return context.player.all<Senses>().any { it.isVisible(point) }
    }

    private fun Angle.drawLine(
            start: Point,
            nSteps: Int,
            level: Level?,
            initColor: Color = overlay.fg * 0.5,
            fgColor: Color = Color.gray,
            colorDegrade: Double = 0.975
    ) {
        var color = initColor
        this.walkLine(start, nSteps, level) { point ->
            if (level != null && !isVisible(point) && level.memory(point) == null) {
                false
            } else {
                overlay.colors(fgColor, max(color, overlay.bg), point)
                color *= colorDegrade
                true
            }
        }
    }

    private fun print() {
        context.moveFocus(cursor)
        overlay.clear()
        if (level.inBounds(cursor) && isVisible(cursor)) {
            context.log.addTemp("Here is: " + level.safeGet(cursor).packEntities().joinToString(separator = ", ") { it.name })
        } else {
            context.log.addTemp("You can't see this shit")
        }

        if (drawLine) {
            val playerPos: Position = context.player.one()
            val hand = context.player.one<MainHandSlot>()
            val gun = hand.gun ?: throw GehennaException("Targeting without a gun, why?")
            val ammo: Ammo? = gun.magazine.firstOrNull()

            val speed = gun.speed + (ammo?.speed ?: 0)
            val range = ((ammo?.lifeTime ?: Action.oneTurn) * (speed / Behaviour.normalSpeed) / 100).toInt() + 1

            //Calculating chance to hit the most retarded way possible: shooting 100 bullets and count how many hit
            val rand = Random(1488) // create new seeded random -> so results are always the same
            var successCount = 0
            if (angle.max > 0) {
                for (i in (1..100)) {
                    val randDir = rand.nextAngle(angle, gun.spread)
                    randDir.walkLine(playerPos, angle.max, playerPos.level) {
                        if (it equals (playerPos + angle.point)) {
                            successCount++
                            false
                        } else {
                            true
                        }
                    }
                }
            }

            val time = Behaviour.scaleTime(angle.max.toLong() * Action.oneTurn, speed)
            context.log.addTemp("Bullet will reach its destination in $time with ${successCount}% chance")

            //            val color = context.hud.fgColor * 0.8 // TODO: constants
            val color = Color(128, 160, 210) * 0.5
            if (angle.max > 0) {
                // TODO: this error shit allows you do "Wanted \ Особо опасен" type shots! Added it to the game!
                if (gun.spread > 0) { // TODO: meh, it just hides the fact that lines are not precise (same in action <Shoot>)
                    (angle.value + gun.spread).toAngle(angle.errorShift).drawLine(playerPos, range, null, color)
                    (angle.value - gun.spread).toAngle(angle.errorShift).drawLine(playerPos, range, null, color)
                }

                angle.drawLine(playerPos, range, if (ammo?.bounce == true) playerPos.level else null)
                println("Line Dir : $angle, errorSift: ${angle.errorShift}, angle: ${angle.value}")
            }
        }
        overlay.colors(point = cursor, fg = Color.gray, bg = Color(96, 32, 32))
    }

    init {
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
            this to true
        }
        is Input.Increase -> {
            if (error < angle.maxError) {
                error += 1
                print()
            } else {
                context.log.addTemp("Can't move your hand any further")
            }
            this to true
        }
        is Input.Decrease -> {
            if (error > angle.minError) {
                error -= 1
                print()
            } else {
                context.log.addTemp("Can't move your hand further")
            }
            this to true
        }
        is Input.Run -> {
            cursor += input.dir * 5
            print()
            this to true
        }
        Input.Cancel -> {
            clean()
            Normal(context) to true
        }
        Input.Accept, Input.Fire -> {
            if (level.inBounds(cursor) && (isVisible(cursor) || !onlyVisible)) {
                val state = select()
                if (state != this) clean()
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