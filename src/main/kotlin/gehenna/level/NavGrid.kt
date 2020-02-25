package gehenna.level

import org.xguzm.pathfinding.PathFinderOptions
import org.xguzm.pathfinding.grid.NavigationGrid
import org.xguzm.pathfinding.grid.NavigationGridGraphNode
import org.xguzm.pathfinding.grid.finders.GridFinderOptions
import java.util.ArrayList

/**
 * NavigationGrid which if dontCrossCorners is false allows you to squeeze between two obstacles in diagonal movement
 */
class NavGrid<T : NavigationGridGraphNode>(nodes: Array<Array<T>>, autoAssignXY: Boolean) : NavigationGrid<T>(nodes, autoAssignXY) {

    /**
     * Changed getNeighbors so d0..3 is always true for (dontCrossCorners = false)
     */
    override fun getNeighbors(node: T, opt: PathFinderOptions): List<T> {
        val options = opt as GridFinderOptions
        val allowDiagonal = options.allowDiagonal
        val dontCrossCorners = options.dontCrossCorners
        val yDir = if (options.isYDown) -1 else 1
        val x = node.x
        val y = node.y
        val neighbors: ArrayList<T> = ArrayList()
        var s0 = false
        var d0 = false
        var s1 = false
        var d1 = false
        var s2 = false
        var d2 = false
        var s3 = false
        var d3 = false
        // up
        if (isWalkable(x, y + yDir)) {
            neighbors.add(nodes[x][y + yDir])
            s0 = true
        }
        // right
        if (isWalkable(x + 1, y)) {
            neighbors.add(nodes[x + 1][y])
            s1 = true
        }
        // down
        if (isWalkable(x, y - yDir)) {
            neighbors.add(nodes[x][y - yDir])
            s2 = true
        }
        // left
        if (isWalkable(x - 1, y)) {
            neighbors.add(nodes[x - 1][y])
            s3 = true
        }
        if (!allowDiagonal) {
            return neighbors
        }
        if (dontCrossCorners) {
            d0 = s3 && s0
            d1 = s0 && s1
            d2 = s1 && s2
            d3 = s2 && s3
        } else {
            d0 = true
            d1 = true
            d2 = true
            d3 = true
        }
        // up left
        if (d0 && this.isWalkable(x - 1, y + yDir)) {
            neighbors.add(nodes[x - 1][y + yDir])
        }
        // up right
        if (d1 && this.isWalkable(x + 1, y + yDir)) {
            neighbors.add(nodes[x + 1][y + yDir])
        }
        // down right
        if (d2 && this.isWalkable(x + 1, y - yDir)) {
            neighbors.add(nodes[x + 1][y - yDir])
        }
        // down left
        if (d3 && this.isWalkable(x - 1, y - yDir)) {
            neighbors.add(nodes[x - 1][y - yDir])
        }
        return neighbors
    }

}