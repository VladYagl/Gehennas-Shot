@file:Suppress("EnumEntryName")

package gehenna.ui

import gehenna.utils.Dir

sealed class Input {
    data class Direction(val dir: Dir) : Input()
    data class Run(val dir: Dir) : Input()
    data class Char(val char: kotlin.Char) : Input()
    object Fire : Input()
    object Pickup : Input()
    object Drop : Input()
    object Cancel : Input()
    object Accept : Input()
    object ClimbStairs : Input()
    object Open : Input()
    object Close : Input()
    object Quit : Input()
    object Console : Input()
    object Backspace : Input()
}
