package gehenna.component.behaviour

import gehenna.action.Collide
import gehenna.action.Move
import gehenna.component.DirectionalGlyph
import gehenna.component.Glyph
import gehenna.component.Position
import gehenna.component.Reflecting
import gehenna.core.*
import gehenna.utils.*

data class LineBulletBehaviour(
        override val entity: Entity,
        var dir: LineDir,
        private val damage: Dice,
        override val speed: Int,
        override var waitTime: Long = 0
) : PredictableBehaviour<LineDir>() {
    override val state: LineDir get() = dir

    private data class FollowLine(private val entity: Entity, private val dir: LineDir, private val damage: Dice) : PredictableAction<LineDir>(100) {

        override fun predict(pos: Position, state: LineDir, glyph: Glyph): Triple<Point, LineDir, Glyph> {
            val (error, next) = dir.next(pos)
            val obstacle = pos.level.obstacle(next)
            return if (obstacle?.has<Reflecting>() == true) {
                val (dx, dy) = (next - pos).dir.bounce(pos, dir)
                val newGlyph =
                        if (entity.has<DirectionalGlyph>()) {
                            glyph.copy(entity = entity, char = (entity<DirectionalGlyph>()?.glyphs?.get((dx at dy).dir))
                                    ?: throw Exception("unknown direction for glyph"))
                        } else glyph
                Triple(pos, LineDir(dx, dy, error), newGlyph)
            } else {
                Triple(next, LineDir(dir.x, dir.y, error), glyph)
            }
        }

        override fun perform(context: Context): ActionResult {
            val pos = entity.one<Position>()
            val (next, dir) = predict(pos, dir, Glyph(Entity.world, '?'))
            val obstacle = pos.level.obstacle(next)
            return if (obstacle?.has<Reflecting>() == false) {
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
        return FollowLine(entity, state, damage)
    }

}
