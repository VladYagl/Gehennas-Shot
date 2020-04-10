package gehenna.component.behaviour

import gehenna.action.Collide
import gehenna.action.Move
import gehenna.component.DirectionalGlyph
import gehenna.component.Glyph
import gehenna.component.Position
import gehenna.component.Reflecting
import gehenna.core.*
import gehenna.exception.GehennaException
import gehenna.ui.UIContext
import gehenna.utils.*

//TODO: try some player seeking behaviour
data class ProjectileBehaviour(
        override val entity: Entity,
        var angle: Angle,
        private val damage: Dice,
        override val speed: Int,
        private val bounce: Boolean = true,
        override var waitTime: Long = 0
) : PredictableBehaviour<Angle>() {
    override val state: Angle get() = angle

    private data class FollowLine(
            private val entity: Entity,
            private val angle: Angle,
            private val damage: Dice,
            private val bounce: Boolean
    ) : PredictableAction<Angle>(100) {

        override fun predict(pos: Position, state: Angle, glyph: Glyph): Triple<Point, Angle, Glyph> {
            val (error, next) = angle.next(pos)
            if (!pos.level.inBounds(next)) { // TODO : looks suspicious
                return pos to state to glyph
            }
            val obstacle = pos.level.obstacle(next)
            return if (obstacle?.has<Reflecting>() == true && bounce) {
                val (dx, dy) = (next - pos).dir.bounce(pos, angle)
                pos to Angle(dx, dy, error) to (entity<DirectionalGlyph>()?.let {
                    glyph.copy(entity = glyph.entity, char = (it.glyphs[(dx at dy).dir]
                            ?: throw GehennaException("unknown direction for glyph")))
                } ?: glyph)
            } else {
                next to Angle(angle.x, angle.y, error) to (entity<DirectionalGlyph>()?.let {
                    val char = it.glyphs[angle.dir]
                            ?: throw GehennaException("unknown direction for glyph")
                    if (glyph.char != char)
                        glyph.copy(entity = glyph.entity, char = char)
                    else
                        glyph
                } ?: glyph)
            }
        }

        override fun perform(context: UIContext): ActionResult {
            val pos = entity.one<Position>()
            val (next, dir) = predict(pos, angle, Glyph(Entity.world, '?'))
            val obstacle = pos.level.obstacle(next)
            return if (obstacle?.has<Reflecting>() == false || (obstacle != null && !bounce)) {
                Collide(entity, obstacle, damage).also { it.time = time }.perform(context)
            } else {
                val behaviour = entity<ProjectileBehaviour>()
                Move(entity, (next - pos).dir).also { it.time = time }.perform(context).also {
                    if (it.succeeded) {
                        behaviour?.angle = dir
                        entity<DirectionalGlyph>()?.update(dir.dir)
                    }
                }
            }
        }
    }

    override fun predictImpl(pos: Position, state: Angle): PredictableAction<in Angle> {
        return FollowLine(entity, state, damage, bounce)
    }

}
