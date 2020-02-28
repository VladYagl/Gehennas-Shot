package gehenna.utils

import gehenna.exception.GehennaException
import java.io.Serializable

private const val runs = 100_000

abstract class Dice : Serializable {
    abstract fun roll(): Int

    /**
     * prints roll distribution, needed for testing
     */
    fun visualise(): String {
        val histogram = (1..runs).groupingBy {
            roll()
        }.eachCount()
        val max = histogram.values.max()!!
        return histogram.toSortedMap().map { (dice, value) ->
            "$dice:" + "#".repeat((75.toDouble() / max * value).toInt())
        }.joinToString(separator = "\n")
    }

    val mean: Double by lazy { (1..runs).map { roll() }.mean }
    val std: Double by lazy { (1..runs).map { roll() }.std }

    data class Const(val value: Int) : Dice() {
        override fun roll() = value

        override fun toString(): String {
            return "$value"
        }
    }

    data class SingleDice(val sides: Int) : Dice() {
        override fun roll() = random.nextInt(1, sides + 1)

        override fun toString(): String {
            return "d$sides"
        }
    }

    data class Multiplication(val dice: Dice, val count: Int) : Dice() {
        override fun roll() = (0 until count).map { dice.roll() }.sum()

        override fun toString(): String {
            return if (dice is SingleDice) {
                "$count$dice"
            } else {
                "$count($dice)"
            }
        }
    }

    data class Addition(val a: Dice, val b: Dice) : Dice() {
        override fun roll() = a.roll() + b.roll()

        override fun toString(): String {
            return "$a+$b"
        }
    }

    data class Subtraction(val a: Dice, val b: Dice) : Dice() {
        override fun roll() = a.roll() - b.roll()

        override fun toString(): String {
            return "$a-$b"
        }
    }

    operator fun plus(other: Dice): Dice {
        return when {
            //TODO: Tried rewriting it a little bit -> Pls finish it
            other is Addition -> (this + other.a) + other.b
            this is Addition && this.b is Const && other !is Const -> (this.a + other) + this.b
            this is Addition && this.b is Const && other is Const -> this.a + Const(this.b.value + other.value)
            this is Const && other !is Const -> other + this
            this is Const && other is Const -> Const(this.value + other.value)
            this is SingleDice && other is SingleDice && this.sides == other.sides -> this * 2
            this is Multiplication && this.dice is SingleDice && other is SingleDice && this.dice.sides == other.sides ->
                Multiplication(this.dice, this.count + 1)
            other is Multiplication && other.dice is SingleDice && this is SingleDice && other.dice.sides == this.sides ->
                Multiplication(other.dice, other.count + 1)
            this is Multiplication && this.dice is SingleDice &&
                    other is Multiplication && other.dice is SingleDice &&
                    this.dice.sides == other.dice.sides -> Multiplication(this.dice, this.count + other.count)
            other is Const && other.value == 0 -> this
            else -> Addition(this, other)
        }
    }

    operator fun minus(other: Dice) = Subtraction(this, other)

    operator fun plus(value: Int) = this + Const(value)

    operator fun minus(value: Int) = this - Const(value)

    operator fun times(value: Int): Dice {
        return when (this) {
            is Const -> {
                Const(this.value * value)
            }
            is Addition -> {
                this.a * value + this.b * value
            }
            is Subtraction -> {
                this.a * value - this.b * value
            }
            is Multiplication -> {
                Multiplication(this.dice, this.count * value)
            }
            else -> {
                Multiplication(this, value)
            }
        }
    }

    companion object {
        private val tokens = ArrayList<Any>()
        private var pos = 0
        private fun next(): Any? {
            return tokens.getOrNull(pos)
        }

        private fun readTokens(string: String) {
            tokens.clear()
            pos = 0
            var i = 0
            while (i < string.length) {
                when (val char = string[i]) {
                    '-', '+', '(', ')' -> {
                        tokens.add(char)
                    }
                    in '0'..'9' -> {
                        var number = char.toString()
                        while (i < string.length - 1 && string[i + 1].isDigit()) {
                            number += string[i + 1]
                            i++
                        }
                        tokens.add(number.toInt())
                    }
                    'd' -> {
                        var number = ""
                        while (i < string.length - 1 && string[i + 1].isDigit()) {
                            number += string[i + 1]
                            i++
                        }
                        tokens.add(SingleDice(number.toInt()))
                    }
                }
                i++
            }
        }

        private fun parseSum(): Dice {
            var dice = parseSingle()
            var next = next()
            while (next == '+' || next == '-') {
                pos++
                val b = parseSingle()
                if (next == '+') {
                    dice += b
                } else {
                    dice -= b
                }
                next = next()
            }
            return dice
        }

        private fun parseSingle(): Dice {
            return when (val next = next()) {
                is Dice -> {
                    pos++
                    next
                }
                '(' -> {
                    pos++
                    val dice = parseSum()
                    if (next() != ')') throw GehennaException("Bad brackets")
                    pos++
                    dice
                }
                is Int -> {
                    pos++
                    next * parseSingle()
                }
                else -> Const(1)
            }
        }

        fun fromString(string: String): Dice {
            readTokens(string)
            return parseSum()
        }
    }
}

private operator fun Int.times(dice: Dice) = dice * this

fun String.roll() = Dice.fromString(this).roll()
fun String.toDice(): Dice = Dice.fromString(this)

fun main() {
    println(Dice.fromString("3(2d6 + 5) + d6 + 4"))
//    println(Dice.fromString("10d5 + 12"))
//    println(Dice.fromString("101d12"))
//    println(Dice.fromString("d6"))
//    println(Dice.fromString("d6 + d8"))
//    println("d20 + d10 + d30 + d40".toDice())
//
//    println("d10".toDice().visualise())
//    println("2d5".toDice().visualise())
//    println("2d5".toDice().mean)
//    println("2d5".toDice().std)
}

