package gehenna.ui.state

import gehenna.component.Position
import gehenna.core.Action
import gehenna.core.Behaviour
import gehenna.level.Level
import gehenna.utils.*
import java.awt.Color
import kotlin.random.Random

abstract class TargetLine : Target() {

    protected abstract val range: Int
    protected abstract val speed: Int
    protected open val spread: Double = 0.0
    protected open val bounce: Boolean = false
    protected open val projectileName: String = "it"

    private fun Angle.drawLine(
            start: Point,
            nSteps: Int,
            level: Level?,
            initColor: Color = overlay.fg * 0.4 + Color(64, 0, 32),
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

    override fun print() {
        super.print()

        val playerPos: Position = context.player.one()

        //Calculating chance to hit the most retarded way possible: shooting 100 bullets and count how many hit
        val rand = Random(1488) // create new seeded random -> so results are always the same
        var successCount = 0
        if (angle.max > 0) {
            for (i in (1..100)) {
                val randDir = rand.nextAngle(angle, spread)
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
        context.log.addTemp("${projectileName.replaceFirstChar{ it.titlecase() }} will reach its destination in $time with ${successCount}% chance")

        //            val color = context.hud.fgColor * 0.8 // TODO: constants
        val color = Color(128, 160, 210) * 0.5
        if (angle.max > 0) {
            // TODO: this error shit allows you do "Wanted \ Особо опасен" type shots! Added it to the game!
            if (spread > 0) { // TODO: meh, it just hides the fact that lines are not precise (same in action <Shoot>)
                (angle.value + spread).toAngle(angle.errorShift).drawLine(playerPos, range, null, color)
                (angle.value - spread).toAngle(angle.errorShift).drawLine(playerPos, range, null, color)
            }

            angle.drawLine(playerPos, range, if (bounce) playerPos.level else null)
            println("Line Dir : $angle, errorSift: ${angle.errorShift}, angle: ${angle.value}")
        }

        overlay.colors(point = cursor, fg = Color.gray, bg = Color(96, 32, 32))
    }
}