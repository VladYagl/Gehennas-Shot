@file:Suppress("EnumEntryName")

package gehenna.ui

import gehenna.utils.Dir

sealed class Input {
    data class Direction(val dir: Dir) : Input()
    data class Run(val dir: Dir) : Input()

    object Pickup : Input()
    object Drop : Input()
    object Use : Input()
    object Equip : Input()
    object Inventory : Input()
    object Fire : Input()
    object ClimbStairs : Input()
    object Open : Input()
    object Close : Input()
    object Console : Input()
    object Examine : Input()
    object Quit : Input()
    object Increase : Input()
    object Decrease : Input()

//    object Unload : Input()
//    object Load : Input()
    object Reload : Input()

    data class Char(val char: kotlin.Char) : Input()
    object Backspace : Input()

    object Cancel : Input()
    object Accept : Input()
}
