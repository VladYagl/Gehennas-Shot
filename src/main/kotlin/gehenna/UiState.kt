package gehenna

import gehenna.components.*

sealed class UiState(val game: Game) {
    fun getDir(char: Char): Pair<Int, Int>? {
        return when (char) {
            '.', '5' -> 0 to 0
            'j', '2' -> 0 to +1
            'k', '8' -> 0 to -1
            'h', '4' -> -1 to 0
            'l', '6' -> +1 to 0
            'y', '7' -> -1 to -1
            'u', '9' -> +1 to -1
            'n', '3' -> +1 to +1
            'b', '1' -> -1 to +1
            else -> null
        }
    }

    abstract fun handleChar(char: Char): UiState

    fun action(value: Action) {
        game.player[ThinkUntilSet::class]?.action = value
    }

    class Normal(game: Game) : UiState(game) {
        override fun handleChar(char: Char): UiState {
            val dir = getDir(char)
            if (dir != null) {
                action(Move(game.player, dir))
            }
            return when (char) {
                'f' -> {
                    val inventory = game.player[Inventory::class]!!
                    val gun = inventory.all().mapNotNull { it.entity[Gun::class] }.firstOrNull()
                    if (gun == null) {
                        game.player[Logger::class]!!.add("You don't have any guns!")
                        return Normal(game)
                    }
                    Aim(game, gun)
                }
                '>', '<' -> {
                    action(ClimbStairs(game.player))
                    this
                }
                else -> this
            }
        }
    }

    class Aim(game: Game, private val gun: Gun) : UiState(game) {
        override fun handleChar(char: Char): UiState {
            val dir = getDir(char)
            if (dir != null) {
                action(ApplyEffect(game.player, RunAndGun(game.player, dir, gun, 500, time = 10)))
                return Normal(game)
            }
            return this
        }
    }

    class End(game: Game) : UiState(game) {
        override fun handleChar(char: Char): UiState {
            if (char == ' ') System.exit(0)
            return this
        }
    }
}
