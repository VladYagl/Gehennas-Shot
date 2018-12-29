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

    protected val log = game.player[Logger::class]!!

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
                        log.add("You don't have any guns!")
                        return Normal(game)
                    }
                    Aim(game, gun)
                }
                ',', 'g' -> {
                    val pos = game.player[Position::class]!!
                    val items = pos.neighbors.filter { it.has(Item::class) }
                    if (items.isEmpty()) {
                        log.add("There is no items to pickup(((")
                        return Normal(game)
                    }
                    Pickup(game, items)
                }
                'd' -> {
                    Drop(game)
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

    //TODO: pop up window
    //TODO: Why it's not an action??
    class Pickup(game: Game, private val items: List<Entity>) : UiState(game) {
        private val select = BooleanArray(items.size) { false }

        init {
            log.add("Pick up what?")
            items.forEachIndexed { index, entity ->
                log.add("\t${if (select[index]) '+' else '-'}${'a' + index}: $entity")
            }
        }

        override fun handleChar(char: Char): UiState {
            if (char in 'a'..'z') {
                val index = char - 'a'
                select[index] = !select[index]
                return this
            }
            if (char.toInt() == 10) { //FIXME: ENTER HACK??
                val inventory = game.player[Inventory::class]!!
                items.forEachIndexed { index, entity ->
                    if (select[index]) {
                        entity.remove(entity[Position::class]!!)
                        inventory.add(entity[Item::class]!!)
                    }
                }
                return Normal(game)
            }
            return this
        }
    }

    class Drop(game: Game) : UiState(game) {
        private val items = game.player[Inventory::class]!!.all()
        private val select = BooleanArray(items.size) { false }

        init {
            log.add("Drop what?")
            items.forEachIndexed { index, item ->
                log.add("\t${if (select[index]) '+' else '-'}${'a' + index}: ${item.entity}")
            }
        }

        override fun handleChar(char: Char): UiState {
            if (char in 'a'..'z') {
                val index = char - 'a'
                select[index] = !select[index]
                return this
            }
            if (char.toInt() == 10) { //FIXME: ENTER HACK??
                val pos = game.player[Position::class]!!
                val inventory = game.player[Inventory::class]!!
                items.forEachIndexed { index, item ->
                    if (select[index]) {
                        inventory.remove(item)
                        pos.level.spawn(item.entity, pos.x, pos.y)
                    }
                }
                return Normal(game)
            }
            return this
        }
    }
}
