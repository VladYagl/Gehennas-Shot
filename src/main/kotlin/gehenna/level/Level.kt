package gehenna.level

import gehenna.action.Collide
import gehenna.action.Move
import gehenna.action.scaleTime
import gehenna.component.Glyph
import gehenna.component.Position
import gehenna.component.Stats
import gehenna.component.behaviour.BulletBehaviour
import gehenna.component.behaviour.PredictableBehaviour
import gehenna.core.ComponentManager
import gehenna.core.Entity
import gehenna.utils.Point
import gehenna.utils.directions

abstract class Level(width: Int, height: Int) : FovLevel(width, height) {
    fun predictWithGlyph(realBehaviour: PredictableBehaviour, duration: Long): List<Pair<Point, Glyph>> {
        // TODO : LIST OF PAIR ------ SHIT
        // FIXME: Now it actually moves this fake entity through real level, and also adds it to component manager
        // FIXME: Though I'm not performing it's action, so in theory it can't break anything
        val fakeEntity = Entity("stub")
        val fakeBehaviour = realBehaviour.copy(entity = fakeEntity)
        val realEntity = realBehaviour.entity
        val realPos = realEntity[Position::class]!!
        if (realPos.level != this) throw Exception("predicting other level")
        var fakePos = realPos.copy(entity = fakeEntity)
        var fakeGlyph = realEntity[Glyph::class]!!.copy(entity = fakeEntity)
        val speed = realEntity[Stats::class]?.speed ?: 100
        fakeEntity.add(fakePos)
        var time = fakeBehaviour.time
        val prediction = ArrayList<Pair<Point, Glyph>>()
        loop@ while (time < duration) {
            val action = fakeBehaviour.action
            time += scaleTime(action.time, speed)
            val (x, y) = when (action) {
                is Move -> fakePos + action.dir
                is Collide -> action.victim[Position::class]!!.point
                is BulletBehaviour.Bounce -> {
                    val dir = action.bounce(fakePos)
                    (fakeBehaviour as BulletBehaviour).dir = dir
                    fakeGlyph = realEntity[Glyph::class]!!.copy(
                        entity = fakeEntity,
                        char = fakeBehaviour.dirChar()
                    )
                    fakePos.point
                }
                else -> continue@loop
            }
            prediction.add((x to y) to fakeGlyph)
            fakeEntity.remove(fakePos)
            fakePos = Position(fakeEntity, x, y, fakePos.level)
            fakeEntity.add(fakePos)
        }
        fakeEntity.remove(fakePos)

        return prediction
    }

    fun predict(realBehaviour: PredictableBehaviour, duration: Long): List<Point> =
        predictWithGlyph(realBehaviour, duration).unzip().first

    fun dangerZone(duration: Long): HashSet<Point> {
        val zone = HashSet<Point>()
        ComponentManager.predictables().forEach {
            it.entity[Position::class]?.let { pos ->
                if (pos.level == this) {
                    zone.addAll(predict(it, duration))
                }
            }
        }
        return zone
    }
}

