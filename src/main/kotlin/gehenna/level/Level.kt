package gehenna.level

import gehenna.component.Glyph
import gehenna.component.Position
import gehenna.core.PredictableBehaviour
import gehenna.core.Entity
import gehenna.utils.Point
import gehenna.utils.Size
import java.util.*

typealias Prediction = ArrayList<Pair<Point, Glyph>>

open class Level(size: Size, val depth: Int = 0, val id: String = UUID.randomUUID().toString()) : FovLevel(size) {

    fun predictWithGlyph(behaviour: PredictableBehaviour<Any>, duration: Long): Prediction {
        val realEntity = behaviour.entity

        val prediction = Prediction()
        var time = behaviour.waitTime

        var fakeState = behaviour.state
        var fakePos = realEntity.one<Position>().copy(entity = Entity.world)
        var fakeGlyph = realEntity.one<Glyph>().copy(entity = Entity.world)

        while (time < duration) {
            val action = behaviour.predict(fakePos, fakeState)
            time += action.time
            val (point, state, glyph) = action.predict(fakePos, fakeState, fakeGlyph)
            fakeState = state ?: fakeState
            fakeGlyph = glyph
            prediction.add(point to glyph)
            fakePos = fakePos.copy(x = point.x, y = point.y)
        }

        return prediction
    }

    fun predict(realBehaviour: PredictableBehaviour<Any>, duration: Long): List<Point> =
            predictWithGlyph(realBehaviour, duration).unzip().first

    override fun toString(): String = "Dungeon Level #$depth"
}

