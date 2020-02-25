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

//TODO: try some player seeking behaviour
data class BulletBehaviour(
        override val entity: Entity,
        var dir: Dir,
        private val damage: Dice,
        override val speed: Int,
        override var waitTime: Long = 0
) : PredictableBehaviour<Dir>() {
    override val state get() = dir

    data class Bounce(private val entity: Entity, val dir: Dir) : PredictableAction<Dir>(30) {

        override fun predict(pos: Position, state: Dir, glyph: Glyph): Triple<Point, Dir, Glyph> {
            val dir = state.bounce(pos).dir
            val directionalGlyph = entity<DirectionalGlyph>()
            val newGlyph = if (directionalGlyph != null) {
                glyph.copy(char = directionalGlyph.glyphs[dir]
                        ?: throw GehennaException("No glyph for this direction: $dir in ${directionalGlyph.glyphs}"))
            } else {
                glyph
            }
            return pos to dir to newGlyph
        }

        override fun perform(context: Context): ActionResult {
            val behaviour = entity<BulletBehaviour>()
            behaviour?.dir = dir.bounce(entity.one()).dir
            entity<DirectionalGlyph>()?.update(dir)
            return end()
        }
    }

    override fun predictImpl(pos: Position, state: Dir): PredictableAction<in Dir> {
        val dir = state
        val obstacle = pos.level.obstacle(pos + dir)
        if (obstacle != null) {
            if (obstacle.has<Reflecting>()) {
                return Bounce(entity, dir)
            }
            return Collide(entity, obstacle, damage)
        }
        return Move(entity, dir)
    }
}