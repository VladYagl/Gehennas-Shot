package gehenna.ui.panel

import gehenna.component.Inventory
import gehenna.component.Position
import gehenna.ui.*

class ConsolePanel(private val context: UIContext) : GehennaPanel(100, 2, context.settings), InputListener {
    override val keyHandler = TextInput(this)

    override fun onInput(input: Input) = when (input) {
        is Input.Char -> {
            command += input.char
            writeLine(command, 0, alignment = Alignment.left)
            true
        }
        is Input.Accept -> {
            try {
                val words = command.split(' ')
                when (words[0]) {
                    "spawn" -> context.player.one<Position>().spawnHere(context.factory.new(words[1]))
                    "give" -> context.player.one<Inventory>().add(context.factory.new(words[1])()!!)
                    "find" -> context.log.addTemp(
                            context.player.one<Position>().level.getAll().find { it.name == words[1] }?.invoke<Position>().toString()
                    )
                }
            } catch (e: Throwable) {
                context.printException(e)
            }
            context.removeWindow(this)
            true
        }
        is Input.Backspace -> {
            command = command.dropLast(1)
            writeLine(command, 0, alignment = Alignment.left)
            true
        }
        is Input.Cancel -> {
            context.removeWindow(this)
            true
        }
        else -> false
    }

    private var command: String = ""
}
