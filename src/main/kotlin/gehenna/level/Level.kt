package gehenna.level

import gehenna.component.DirectionalGlyph
import gehenna.component.Glyph
import gehenna.component.Position
import gehenna.component.behaviour.PredictableBehaviour
import gehenna.core.Entity
import gehenna.utils.Point
import gehenna.utils.Size
import gehenna.utils.at
import java.util.*
import kotlin.collections.ArrayList

typealias Prediction = ArrayList<Pair<Point, Glyph>>

class Level(size: Size, val depth: Int = 0, val id: String = UUID.randomUUID().toString()) : FovLevel(size) {

    fun predictWithGlyph(behaviour: PredictableBehaviour, duration: Long): Prediction {
        val realEntity = behaviour.entity
        var fakePos = realEntity.one<Position>().copy(entity = Entity.world)

        var time = behaviour.waitTime
        var dir = behaviour.dir
        val prediction = Prediction()
        val directionalGlyph = realEntity<DirectionalGlyph>()
        var fakeGlyph = directionalGlyph?.glyphs?.get(dir)?.let {
            realEntity.one<Glyph>().copy(entity = Entity.world, char = it)
        } ?: realEntity.one<Glyph>().copy(entity = Entity.world)

        loop@ while (time < duration) {
            val action = behaviour.predict(fakePos, dir)
            time += action.time
            val (x, y) = action.predict(fakePos)
            action.predictDir(fakePos)?.let {newDir ->
                dir = newDir
                directionalGlyph?.glyphs?.get(dir)?.let {newChar ->
                    fakeGlyph = fakeGlyph.copy(char = newChar)
                }
            }
            prediction.add((x at y) to fakeGlyph)
            fakePos = fakePos.copy(x = x, y = y)
        }

        return prediction
    }

    fun predict(realBehaviour: PredictableBehaviour, duration: Long): List<Point> =
            predictWithGlyph(realBehaviour, duration).unzip().first

    override fun toString(): String = "Dungeon Level #$depth"
}

