package gehenna.component.behaviour

import gehenna.action.Destroy
import gehenna.action.Move
import gehenna.component.DirectionalGlyph
import gehenna.component.Glyph
import gehenna.component.Position
import gehenna.component.Reflecting
import gehenna.core.*
import gehenna.exception.GehennaException
import gehenna.ui.UIContext
import gehenna.utils.Angle
import gehenna.utils.Point
import gehenna.utils.at
import gehenna.utils.bounce

//TODO: try some player seeking behaviour
data class ProjectileBehaviour(
        override val entity: Entity,
        var angle: Angle,
        override val speed: Int,
        var distance: Int,
        private val collisionAction: (Entity) -> Action,
        private val bounce: Boolean = true,
        override var waitTime: Long = 0,
        private val maxDistanceAction: Action = Destroy(entity)
) : PredictableBehaviour<Pair<Angle, Int>>() {
    override val state: Pair<Angle, Int> get() = angle to distance

    private data class FollowLine(
            private val entity: Entity,
            private val angle: Angle,
            private val distance: Int,
            private val bounce: Boolean,
            private val collisionAction: (Entity) -> Action,
            private val maxDistanceAction: Action
    ) : PredictableAction<Pair<Angle, Int>>(100) {

        override fun predict(pos: Position, state: Pair<Angle, Int>, glyph: Glyph): Triple<Point, Pair<Angle, Int>, Glyph>? {
            val (error, next) = angle.next(pos)
            if (!pos.level.inBounds(next) || state.second <= 0) { // TODO : looks suspicious
                return null
            }
            val obstacle = pos.level.obstacle(next)
            return if (obstacle?.has<Reflecting>() == true && bounce) {
                val (dx, dy) = (next - pos).dir.bounce(pos, angle)
                Triple(pos, Angle(dx, dy, error) to (state.second - 1), (entity<DirectionalGlyph>()?.let {
                    glyph.copy(entity = glyph.entity, char = (it.glyphs[(dx at dy).dir]
                            ?: throw GehennaException("unknown direction for glyph")))
                } ?: glyph))
            } else {
                Triple(next, Angle(angle.x, angle.y, error) to (state.second - 1), (entity<DirectionalGlyph>()?.let {
                    val char = it.glyphs[angle.dir]
                            ?: throw GehennaException("unknown direction for glyph")
                    if (glyph.char != char)
                        glyph.copy(entity = glyph.entity, char = char)
                    else
                        glyph
                } ?: glyph))
            }
        }

        override fun perform(context: UIContext): ActionResult {
            val pos = entity.one<Position>()
            val (next, state) = predict(pos, angle to distance, Glyph(Entity.world, '?'))
                    ?: return maxDistanceAction.perform(context)
            val angle = state.first
            val obstacle = pos.level.obstacle(next)
            return if (obstacle?.has<Reflecting>() == false || (obstacle != null && !bounce)) {
                collisionAction(obstacle).also { it.time = time }.perform(context)
            } else {
                val behaviour = entity<ProjectileBehaviour>()
                Move(entity, (next - pos).dir).also { it.time = time }.perform(context).also {
                    if (it.succeeded) {
                        behaviour?.apply {
                            this.angle = angle
                            distance -= 1
                        }
                        entity<DirectionalGlyph>()?.update(angle.dir)
                    }
                }
            }
        }
    }

    override fun predictImpl(pos: Position, state: Pair<Angle, Int>): PredictableAction<in Pair<Angle, Int>> {
        return FollowLine(entity, state.first, state.second, bounce, collisionAction, maxDistanceAction)
    }

}
