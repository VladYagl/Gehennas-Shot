package gehenna.level

import gehenna.factory.LevelPart
import gehenna.utils.BooleanArray
import gehenna.utils.*

class CellularPart(
    override val size: Size,
    private val spawner: (Point) -> Unit
) : LevelPart {
    var cells = BooleanArray(size)

    private fun neighbours(point: Point): Int {
        var cnt = 0
        for (dir in Dir) {
            val p = point + dir
            if (cells.getOrNull(p.x)?.getOrNull(p.y) != false) cnt++
        }
        return cnt
    }

    override fun spawnTo(to: Point, level: BasicLevel) {
        for (point in size.range)
            if (!cells[point]) spawner(to + point)
    }

    override fun needs(point: Point): Boolean = cells[point]

    fun automaton(birth: Int, death: Int, k: Int) {
        repeat(k) {
            val newCells = BooleanArray(size)
            for (point in size.range) {
                newCells[point] = cells[point]
                if (cells[point] && neighbours(point) < death) newCells[point] = false
                if (!cells[point] && neighbours(point) > birth) newCells[point] = true
            }
            cells = newCells
        }
    }
}