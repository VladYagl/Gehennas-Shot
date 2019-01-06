package gehenna.level

import gehenna.action.Collide
import gehenna.action.Move
import gehenna.action.scaleTime
import gehenna.component.Position
import gehenna.component.Stats
import gehenna.component.behaviour.PredictableBehaviour
import gehenna.core.ComponentManager
import gehenna.core.Entity
import gehenna.utils.Point

abstract class Level(width: Int, height: Int) : FovLevel(width, height) {
    fun predict(realBehaviour: PredictableBehaviour, duration: Long): ArrayList<Point> {
        // FIXME: Now it actually moves this fake entity through real level, and also adds it to component manager
        // FIXME: Though I'm not performing it's action, so in theory it can't break anything
        val fakeEntity = Entity("stub")
        val fakeBehaviour = realBehaviour.copy(entity = fakeEntity)
        val realEntity = realBehaviour.entity
        val realPos = realEntity[Position::class]!!
        if (realPos.level != this) throw Exception("predicting other level")
        var fakePos = realPos.copy(entity = fakeEntity)
        val speed = realEntity[Stats::class]?.speed ?: 100
        fakeEntity.add(fakePos)
        var time = fakeBehaviour.time
        val prediction = ArrayList<Point>()
        while (time < duration) {
            val action = fakeBehaviour.action
            when (action) {
                is Move, is Collide -> {
                    val (x, y) = when (action) {
                        is Move -> fakePos + action.dir
                        is Collide -> action.victim[Position::class]!!.point
                        else -> throw Exception("concurrent meme")
                    }
                    prediction.add(x to y)
                    fakeEntity.remove(fakePos)
                    fakePos = Position(fakeEntity, x, y, fakePos.level)
                    fakeEntity.add(fakePos)
                }
            }
            time += scaleTime(action.time, speed)
        }
        fakeEntity.remove(fakePos)

        return prediction
    }

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

