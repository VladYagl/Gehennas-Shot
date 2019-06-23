package gehenna.level

import gehenna.action.Collide
import gehenna.action.Move
import gehenna.component.DirectionalGlyph
import gehenna.component.Glyph
import gehenna.component.Position
import gehenna.component.behaviour.BulletBehaviour
import gehenna.component.behaviour.PredictableBehaviour
import gehenna.core.Entity
import gehenna.utils.Point
import gehenna.utils.Size
import gehenna.utils.at

class Level(size: Size, val depth: Int = 0) : FovLevel(size) {
    fun predictWithGlyph(behaviour: PredictableBehaviour, duration: Long): List<Pair<Point, Glyph>> {
        // todo all calls calculate duration in a wrong way (duration != speed)
        // TODO : LIST OF PAIR ------ SHIT
        behaviour as BulletBehaviour
        val realEntity = behaviour.entity
        val realGlyph = realEntity.one<Glyph>() // TODO ???
        var fakePos = realEntity.one<Position>().copy(entity = Entity.world)
        val directionalGlyph = realEntity<DirectionalGlyph>()

        var time = behaviour.waitTime
        var dir = behaviour.dir
        val prediction = ArrayList<Pair<Point, Glyph>>()

        loop@ while (time < duration) {
            val action = behaviour.predict(fakePos, dir)
            time += action.time
            val (x, y) = when (action) {
                is Move -> fakePos + action.dir
                is Collide -> action.victim.one<Position>()
                is BulletBehaviour.Bounce -> {
                    dir = action.bounce(fakePos)
                    fakePos
                }
                else -> continue@loop
            }
            val glyph = directionalGlyph?.glyphs?.get(dir)?.let {
                realGlyph.copy(entity = Entity.world, char = it)
            } ?: realGlyph.copy(entity = Entity.world)
            prediction.add((x at y) to glyph)
            fakePos = fakePos.copy(x = x, y = y)
        }

        return prediction
    }

    fun predict(realBehaviour: PredictableBehaviour, duration: Long): List<Point> =
            predictWithGlyph(realBehaviour as BulletBehaviour, duration).unzip().first

    override fun toString(): String = "Dungeon Level #$depth"
}

