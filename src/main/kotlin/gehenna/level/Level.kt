package gehenna.level

import gehenna.action.Collide
import gehenna.action.Move
import gehenna.action.scaleTime
import gehenna.component.Glyph
import gehenna.component.Position
import gehenna.component.Stats
import gehenna.component.behaviour.BulletBehaviour
import gehenna.component.behaviour.PredictableBehaviour
import gehenna.core.ActionQueue
import gehenna.core.Entity
import gehenna.utils.Point
import gehenna.utils.Size
import gehenna.utils.at

abstract class Level(size: Size) : FovLevel(size) {
    fun predictWithGlyph(behaviour: PredictableBehaviour, duration: Long): List<Pair<Point, Glyph>> {
        // TODO : LIST OF PAIR ------ SHIT
        behaviour as BulletBehaviour
        val realEntity = behaviour.entity
        var fakePos = realEntity<Position>()!!.copy(entity = Entity.world)
        var fakeGlyph = realEntity<Glyph>()!!.copy(entity = Entity.world)

        val speed = realEntity<Stats>()?.speed ?: 100
        var time = behaviour.waitTime
        var dir = behaviour.dir
        val prediction = ArrayList<Pair<Point, Glyph>>()

        loop@ while (time < duration) {
            val action = behaviour.predict(fakePos, dir)
            time += scaleTime(action.time, speed)
            val (x, y) = when (action) {
                is Move -> fakePos + action.dir
                is Collide -> action.victim<Position>()!!
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

    fun dangerZone(duration: Long): HashSet<Point> {
        val zone = HashSet<Point>()
        ActionQueue.predictables().forEach {
            it.entity<Position>()?.let { pos ->
                if (pos.level == this) {
                    zone.addAll(predict(it, duration))
                }
            }
        }
        return zone
    }
}

