package gehenna.level

import gehenna.factory.LevelPart
import gehenna.utils.BooleanArray
import gehenna.utils.*

class CellularPart(
    override val width: Int,
    override val height: Int,
    private val spawner: (Int, Int) -> Unit
) : LevelPart {
    var cells = BooleanArray(width, height)

    private fun neighbours(x: Int, y: Int): Int {
        var cnt = 0
        for (dir in Dir) {
            val p = (x at y) + dir
            if (cells.getOrNull(p.x)?.getOrNull(p.y) != false) cnt++
        }
        return cnt
    }

    override fun spawnTo(toX: Int, toY: Int, level: BasicLevel) {
        for ((x, y) in range(width, height))
            if (!cells[x, y]) spawner(toX + x, toY + y)
    }

    override fun needs(x: Int, y: Int): Boolean = cells[x, y]

    fun automaton(birth: Int, death: Int, k: Int) {
        repeat(k) {
            val newCells = BooleanArray(width, height)
            for ((x, y) in range(width, height)) {
                newCells[x, y] = cells[x, y]
                if (cells[x, y] && neighbours(x, y) < death) newCells[x, y] = false
                if (!cells[x, y] && neighbours(x, y) > birth) newCells[x, y] = true
            }
            cells = newCells
        }
    }
}