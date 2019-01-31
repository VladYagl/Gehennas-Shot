package gehenna.level

import gehenna.action.Collide
import gehenna.action.Move
import gehenna.component.Glyph
import gehenna.component.Position
import gehenna.component.behaviour.BulletBehaviour
import gehenna.component.behaviour.PredictableBehaviour
import gehenna.core.Entity
import gehenna.utils.Point
import gehenna.utils.Size
import gehenna.utils.at

abstract class Level(size: Size) : FovLevel(size) {
    fun predictWithGlyph(behaviour: PredictableBehaviour, duration: Long): List<Pair<Point, Glyph>> {
        // todo all calls calculate duration in a wrong way (duration != speed)
        // TODO : LIST OF PAIR ------ SHIT
        behaviour as BulletBehaviour
        val realEntity = behaviour.entity
        var fakePos = realEntity.one<Position>().copy(entity = Entity.world)
        var fakeGlyph = realEntity.one<Glyph>().copy(entity = Entity.world)

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
                    fakeGlyph = fakeGlyph.copy(char = behaviour.dirChar(dir))
                    fakePos
                }
                else -> continue@loop
            }
            prediction.add((x at y) to fakeGlyph)
            fakePos = fakePos.copy(x = x, y = y)
        }

        return prediction
    }

    fun predict(realBehaviour: PredictableBehaviour, duration: Long): List<Point> =
            predictWithGlyph(realBehaviour as BulletBehaviour, duration).unzip().first
}

