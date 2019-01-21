package gehenna.level

import gehenna.factory.LevelPart
import gehenna.utils.BooleanArray
import gehenna.utils.*

class CellularPart(
    override val width: Int,
    override val height: Int,
    private val spawner: (Point) -> Unit
) : LevelPart {
    var cells = BooleanArray(width, height)

    private fun neighbours(point: Point): Int {
        var cnt = 0
        for (dir in Dir) {
            val p = point + dir
            if (cells.getOrNull(p.x)?.getOrNull(p.y) != false) cnt++
        }
        return cnt
    }

    override fun spawnTo(to: Point, level: BasicLevel) {
        for (point in range(width, height))
            if (!cells[point]) spawner(to + point)
    }

    override fun needs(point: Point): Boolean = cells[point]

    fun automaton(birth: Int, death: Int, k: Int) {
        repeat(k) {
            val newCells = BooleanArray(width, height)
            for (point in range(width, height)) {
                newCells[point] = cells[point]
                if (cells[point] && neighbours(point) < death) newCells[point] = false
                if (!cells[point] && neighbours(point) > birth) newCells[point] = true
            }
            cells = newCells
        }
    }
}