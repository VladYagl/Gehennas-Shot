package gehenna.ui

import gehenna.ui.panel.AsciiPanel.TileData
import gehenna.utils.Point
import java.awt.Color

class Overlay(val fg: Color, val bg: Color) {
    private val chars = ArrayList<Pair<Point, TileData>>()

    fun put(char: Char, point: Point, fg: Color = this.fg, bg: Color = this.bg): Overlay {
        chars.add(point to TileData(char, fg, bg))
        return this
    }


    fun colors(fg: Color, bg: Color, point: Point): Overlay {
        chars.add(point to TileData(EMPTY_CHAR, fg, bg))
        return this
    }

    fun fg(fg: Color, point: Point): Overlay = colors(fg, bg, point)

    fun bg(bg: Color, point: Point): Overlay = colors(fg, bg, point)

    fun clear(): Overlay {
        chars.clear()
        return this
    }

    fun apply(func: (Point, TileData) -> Unit) {
        chars.toList().forEach { (point, data) -> func(point, data) }
    }
}