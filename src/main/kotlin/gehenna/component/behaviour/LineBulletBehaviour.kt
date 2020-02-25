package gehenna.component.behaviour

import gehenna.action.Collide
import gehenna.action.Move
import gehenna.component.DirectionalGlyph
import gehenna.component.Glyph
import gehenna.component.Position
import gehenna.component.Reflecting
import gehenna.core.*
import gehenna.exceptions.GehennaException
import gehenna.utils.*

data class LineBulletBehaviour(
        override val entity: Entity,
        var dir: LineDir,
        private val damage: Dice,
        override val speed: Int,
        private val bounce: Boolean = true,
        override var waitTime: Long = 0
) : PredictableBehaviour<LineDir>() {
    override val state: LineDir get() = dir

    private data class FollowLine(
            private val entity: Entity,
            private val dir: LineDir,
            private val damage: Dice,
            private val bounce: Boolean
    ) : PredictableAction<LineDir>(100) {

        override fun predict(pos: Position, state: LineDir, glyph: Glyph): Triple<Point, LineDir, Glyph> {
            val (error, next) = dir.next(pos)
            if (!pos.level.inBounds(next)) { // TODO : looks suspicious
                return pos to state to glyph
            }
            val obstacle = pos.level.obstacle(next)
            return if (obstacle?.has<Reflecting>() == true && bounce) {
                val (dx, dy) = (next - pos).dir.bounce(pos, dir)
                pos to LineDir(dx, dy, error) to (entity<DirectionalGlyph>()?.let {
                    glyph.copy(entity = glyph.entity, char = (it.glyphs[(dx at dy).dir]
                            ?: throw GehennaException("unknown direction for glyph")))
                } ?: glyph)
            } else {
                next to LineDir(dir.x, dir.y, error) to (entity<DirectionalGlyph>()?.let {
                    val char = it.glyphs[dir.dir]
                            ?: throw GehennaException("unknown direction for glyph")
                    if (glyph.char != char)
                        glyph.copy(entity = glyph.entity, char = char)
                    else
                        glyph
                } ?: glyph)
            }
        }

        override fun perform(context: Context): ActionResult {
            val pos = entity.one<Position>()
            val (next, dir) = predict(pos, dir, Glyph(Entity.world, '?'))
            val obstacle = pos.level.obstacle(next)
            return if (obstacle?.has<Reflecting>() == false || (obstacle != null && !bounce)) {
                Collide(entity, obstacle, damage).also { it.time = time }.perform(context)
            } else {
                val behaviour = entity<LineBulletBehaviour>()
                Move(entity, (next - pos).dir).also { it.time = time }.perform(context).also {
                    if (it.succeeded) {
                        behaviour?.dir = dir
                        entity<DirectionalGlyph>()?.update(dir.dir)
                    }
                }
            }
        }
    }

    override fun predictImpl(pos: Position, state: LineDir): PredictableAction<in LineDir> {
        return FollowLine(entity, state, damage, bounce)
    }

}
