package gehenna.ui

import com.beust.klaxon.internal.firstNotNullResult
import gehenna.action.*
import gehenna.component.*
import gehenna.component.behaviour.Behaviour
import gehenna.component.behaviour.CharacterBehaviour
import gehenna.component.behaviour.PlayerBehaviour
import gehenna.core.Action.Companion.oneTurn
import gehenna.core.Entity
import gehenna.exceptions.GehennaException
import gehenna.level.Level
import gehenna.ui.panel.ConsolePanel
import gehenna.ui.panel.MenuPanel
import gehenna.ui.panel.MultiSelectPanel
import gehenna.ui.panel.SelectPanel
import gehenna.utils.*
import java.awt.Color
import kotlin.random.Random

abstract class State {
    open fun handleInput(input: Input): Pair<State, Boolean> = this to false

    companion object {
        fun create(context: UIContext): State = Normal(context)
    }
}

private abstract class Direction(protected val context: UIContext) : State() {

    init {
        context.log.addTemp("In which direction?")
    }

    abstract fun onDir(dir: Dir): State
    open fun onCancel(): State {
        context.log.addTemp("Never mind")
        return Normal(context)
    }

    final override fun handleInput(input: Input) = when (input) {
        is Input.Direction -> onDir(input.dir) to true
        Input.Cancel -> onCancel() to true
        else -> this to false
    }
}

private abstract class Target(
        protected val context: UIContext,
        protected val onlyVisible: Boolean = true,
        protected val autoAim: Boolean = false,
        protected val drawLine: Boolean = false)
    : State() {

    protected var cursor: Point
    protected var error: Int = 0
    protected val dir: LineDir
        get() {
            val diff = cursor - context.player.one<Position>()
            return LineDir(diff.x, diff.y, error)
        }

    protected val level: Level = context.player.one<Position>().level

    private fun hideCursor() {
        context.focusPlayer()
        context.hud.clear(EMPTY_CHAR)
    }

    private fun isVisible(point: Point): Boolean {
        return context.player.all<Senses>().any { it.isVisible(point) }
    }

    private fun LineDir.drawLine(
            start: Point,
            nSteps: Int,
            level: Level?,
            initColor: Color = context.hud.fgColor * 0.5,
            fgColor: Color = Color.gray,
            colorDegrade: Double = 0.975
    ) {
        var color = initColor
        this.walkLine(start, nSteps, level) { point ->
            if (level != null && !isVisible(point) && level.memory(point) == null) {
                false
            } else {
                context.putCharOnHUD(EMPTY_CHAR, point.x, point.y, fg = fgColor, bg = max(color, context.hud.bgColor))
                color *= colorDegrade
                true
            }
        }
    }

    private fun print() {
        context.moveFocus(cursor)
        context.hud.clear(EMPTY_CHAR)
        if (level.inBounds(cursor) && isVisible(cursor)) {
            context.log.addTemp("Here is: " + level.safeGet(cursor).packEntities().joinToString(separator = ", ") { it.name })
        } else {
            context.log.addTemp("You can't see this shit")
        }

        if (drawLine) {
            val playerPos: Position = context.player.one()
            val hand = context.player.one<MainHandSlot>()
            val gun = hand.gun ?: throw GehennaException("Targeting without a gun, why?")
            val ammo: Ammo? = gun.magazine.firstOrNull()
            val range = context.player<Senses.Sight>()?.range ?: 100

            //Calculating chance to hit the most retarded way possible: shooting 100 bullets and count how many hit
            val rand = Random(1488) // create new seeded random -> so results are always the same
            var successCount = 0
            if (dir.max > 0) {
                for (i in (1..100)) { // TODO: this might be too much
                    val randDir = rand.nextLineDir(dir, gun.spread)
                    randDir.walkLine(playerPos, dir.max, playerPos.level) {
                        if (it equals (playerPos + dir.point)) {
                            successCount++
                            false
                        } else {
                            true
                        }
                    }
                }
            }

            val time = Behaviour.scaleTime(dir.max.toLong() * oneTurn, gun.speed + (ammo?.speed ?: 0))
            context.log.addTemp("Bullet will reach its destination in $time with ${successCount}% chance")

            //            val color = context.hud.fgColor * 0.8 // TODO: constants
            val color = Color(128, 160, 210) * 0.5
            if (dir.max > 0) {
                // TODO: this error shit allows you do "Wanted \ Особо опасен" type shots! Added it to the game!
                if (gun.spread > 0) { // TODO: meh, it just hides the fact that lines are not precise (same in action <Shoot>)
                    (dir.angle + gun.spread).toLineDir(dir.errorShift).drawLine(playerPos, range, null, color)
                    (dir.angle - gun.spread).toLineDir(dir.errorShift).drawLine(playerPos, range, null, color)
                }

                dir.drawLine(playerPos, range, if (ammo?.bounce == true) playerPos.level else null)
                println("Line Dir : $dir, errorSift: ${dir.errorShift}, angle: ${dir.angle}")
            }
        }
        context.putCharOnHUD(EMPTY_CHAR, cursor.x, cursor.y, fg = Color.gray, bg = Color(96, 32, 32))
    }

    init {
        val playerPos = context.player.one<Position>()
        cursor = if (!autoAim) {
            playerPos
        } else {
            var target: Point = playerPos
            context.player<PlayerBehaviour>()?.let { player ->
                context.player.all<Senses>().forEach { sense ->
                    sense.visitFov { entity, point ->
                        if (entity.any<CharacterBehaviour>()?.faction?.isEnemy(player.faction) == true) {
                            if ((target equals playerPos) || (target - playerPos).max > (point - playerPos).max) {
                                target = point
                            }
                        }
                    }
                }
            }
            target
        }
        error = dir.findBestError(playerPos) ?: dir.defaultError
        print()
    }

    protected abstract fun select(): State

    final override fun handleInput(input: Input) = when (input) {
        is Input.Direction -> {
            cursor += input.dir
            error = dir.findBestError(context.player.one()) ?: dir.defaultError
            print()
            this to true
        }
        is Input.Increase -> {
            if (error < dir.maxError) {
                error += 1
                print()
            } else {
                context.log.addTemp("Can't move your hand any further")
            }
            this to true
        }
        is Input.Decrease -> {
            if (error > dir.minError) {
                error -= 1
                print()
            } else {
                context.log.addTemp("Can't move your hand further")
            }
            this to true
        }
        is Input.Run -> {
            cursor += input.dir * 5
            print()
            this to true
        }
        Input.Cancel -> {
            hideCursor()
            Normal(context) to true
        }
        Input.Accept, Input.Fire -> {
            if (level.inBounds(cursor) && (isVisible(cursor) || !onlyVisible)) {
                val state = select()
                if (state != this) hideCursor()
                state to true
            } else {
                context.log.addTemp("Can't examine what you can't see")
                this to true
            }
        }
        else -> {
            this to false
        }
    }
}

private class Normal(private val context: UIContext) : State() {

    private fun askForAmmo(ammoType: AmmoType, func: (Collection<Ammo>) -> Unit) {
        context.addWindow(SelectPanel(
                context,
                context.player.one<Inventory>().contents
                        .filter { it.entity<Ammo>()?.type == ammoType }
                        .packStacks(),
                title = "Load what?",
                toString = { it.entity.name }) { func(it.entity.one<ItemStack>().items.map { el -> el.entity.one<Ammo>() }) }
        )
    }

    override fun handleInput(input: Input) = when (input) {
        is Input.Direction -> {
            if (input.dir == Dir.zero) {
                context.action = Wait
                this to true
            } else {
                val playerPos = context.player.one<Position>()

                // check for closed do
                playerPos.level[playerPos + input.dir].firstNotNullResult {
                    if (it<Door>()?.closed == true) it<Door>() else null
                }?.let { door ->
                    context.action = UseDoor(door, close = false)
                    return this to true
                }

                // check for melee attack
                playerPos.level[playerPos + input.dir].firstNotNullResult {
                    it.any<CharacterBehaviour>()?.faction?.let { faction ->
                        if (context.player.one<PlayerBehaviour>().faction.isEnemy(faction)) it else null
                    }
                }?.let { _ ->
                    context.action = Attack(context.player, input.dir)
                    return this to true
                }

                context.action = Move(context.player, input.dir)
                this to true
            }
        }
        is Input.Run -> {
            if (input.dir == Dir.zero) {
                context.action = Move(context.player, input.dir)
            } else {
                context.player<PlayerBehaviour>()?.walk(input.dir)
            }
            this to true
        }
        Input.Cancel -> {
            context.player<PlayerBehaviour>()?.cancel()
            this to true
        }
        Input.Fire -> {
            val gun = context.player.one<MainHandSlot>().gun
            if (gun == null) {
                context.log.addTemp("You don't have a gun equipped")
                this to true
            } else Aim(context, gun) to true
        }
        Input.Pickup -> {
            val pos = context.player.one<Position>()
            val neighbors = pos.neighbors.mapNotNull { it<Item>() }
            if (neighbors.isEmpty()) {
                context.log.addTemp("There is no items to pickup(((")
                this to true
            } else {
                context.addWindow(MultiSelectPanel(
                        context,
                        neighbors.packStacks(),
                        "Pick up what?",
                        toString = { it.entity.name }
                ) { items ->
                    context.action = Pickup(context.player, items.unpackStacks())
                })
                this to true
            }
        }
        Input.Drop -> {
            context.addWindow(MultiSelectPanel(
                    context,
                    context.player.one<Inventory>().stacks,
                    "Drop what?",
                    toString = { it.entity.toString() }
            ) { items -> context.action = Drop(context.player, items.unpackStacks()) })
            this to true
        }
        Input.Use -> {
            context.addWindow(SelectPanel(
                    context,
                    context.player.one<Inventory>().stacks.filter { it.entity.has<Consumable>() },
                    "Use what?",
                    toString = { it.entity.toString() }
            ) {
                context.action = it.entity.any<Consumable>()?.apply(context.player)
            })
            this to true
        }
        Input.Inventory -> {
            context.addWindow(SelectPanel(
                    context,
                    context.player.one<Inventory>().stacks,
                    "Inventory ${context.player.one<Inventory>().currentVolume}/${context.player.one<Inventory>().maxVolume}",
                    toString = { it.entity.toString() }
            ) { selectedItem ->
                context.addWindow(MenuPanel(100, 30, context.settings).apply {
                    setOnCancel { context.removeWindow(this) }
                    addItem(TextItem("${selectedItem.entity} -- vol.: ${selectedItem.volume}"))
                    selectedItem.entity<Consumable>()?.let { usableItem ->
                        addItem(ButtonItem("Use", {
                            context.action = usableItem.apply(context.player)
                            context.removeWindow(this)
                        }, 'u'))
                    }
                    selectedItem.entity<Gun>()?.let { gun ->
                        addItem(ButtonItem("Equip", {
                            context.action = Equip(context.player, context.player.one<MainHandSlot>(), gun.item)
                            context.removeWindow(this)
                        }, 'e'))
                        addItem(ButtonItem("Load", {
                            askForAmmo(gun.ammoType) {
                                context.action = gun.load(context.player, it)
                                context.removeWindow(this)
                            }
                        }, 'L'))
                        addItem(ButtonItem("Unload", {
                            context.action = gun.unload(context.player)
                            context.removeWindow(this)
                        }, 'U'))
                    }
                    addItem(ButtonItem("Drop", {
                        context.action = Drop(context.player, listOf(selectedItem))
                        context.removeWindow(this)
                    }, 'd'))
                    addItem(TextItem(""))
                    addItem(TextItem("  ** About **"))
                    selectedItem.entity.components.values.filterNot { it is Glyph }.forEach { component ->
                        addItem(TextItem("| ${component::class.simpleName}"))
                        component.toString().split(", ", ",", "(", ")").drop(1).filterNot {
                            it.contains("entity=")
                        }.forEach {
                            addItem(TextItem("|   $it"))
                        }
                    }
                })
            })
            this to true
        }
        Input.Equip -> {
            val select = SelectPanel(
                    context,
                    context.player.one<Inventory>().stacks + (null as Item?),
                    title = "Equip what?",
                    toString = { it?.entity?.toString() ?: " - unequip current" }) {
                context.action = Equip(context.player, context.player.one<MainHandSlot>(), it)
            }
            context.addWindow(select)
            this to true
        }
        Input.Reload -> {
            val gun = context.player.one<MainHandSlot>().gun
            if (gun == null) {
                context.log.addTemp("You don't have a gun to reload")
            } else {
                askForAmmo(gun.ammoType) { context.action = gun.load(context.player, it) }
            }
            this to true
        }
        Input.ClimbStairs -> {
            val pos = context.player.one<Position>()
            pos.neighbors.firstNotNullResult { it<Stairs>() }?.let { stairs ->
                context.action = ClimbStairs(context.player, stairs)
            } ?: context.log.addTemp("There is no stairs here")
            this to true
        }
        Input.Open -> UseDoor(context, false) to true
        Input.Close -> UseDoor(context, true) to true
        Input.Console -> {
            context.addWindow(ConsolePanel(context))
            this to true
        }
        Input.Examine -> Examine(context) to true
        else -> this to false
    }
}

private class UseDoor(context: UIContext, private val close: Boolean) : Direction(context) {
    override fun onDir(dir: Dir): State {
        val playerPos = context.player.one<Position>()
        playerPos.level[playerPos + dir].firstNotNullResult { it<Door>() }?.let { door ->
            context.action = UseDoor(door, close)
        } ?: context.log.addTemp("There is no door.")
        return Normal(context)
    }
}

private class Aim(context: UIContext, private val gun: Gun) : Target(context, onlyVisible = false, autoAim = true, drawLine = true) {
    override fun select(): State {
        val diff = cursor - context.player.one<Position>()
        context.action = gun.fire(context.player, LineDir(diff.x, diff.y, error))
        return Normal(context)
    }
}

private class Examine(context: UIContext) : Target(context) {
    private fun examine(entity: Entity) {
        context.addWindow(MenuPanel(100, 30, context.settings).apply {
            addItem(TextItem("This is a $entity"))
            setOnCancel { context.removeWindow(this) }
            entity.components.values.filterNot {
                //                it is Glyph || it is Inventory || it is DirectionalGlyph || it is Position
                it is Glyph || it is Inventory || it is DirectionalGlyph
            }.forEach { component ->
                addItem(TextItem("${component::class.simpleName}"))
                component.toString().split(", ", ",", "(", ")").drop(1).filterNot {
                    it.contains("entity=")
                }.forEach {
                    addItem(TextItem("  $it"))
                }
            }
        })
    }

    override fun select(): State {
        val entities = level.safeGet(cursor).packEntities()
        when (entities.size) {
            0 -> context.log.addTemp("There is nothing to examine")
            1 -> examine(entities.first())
            else -> {
                context.addWindow(SelectPanel(context, entities, "There are: ") {
                    examine(it)
                })
            }
        }
        return this
    }
}
