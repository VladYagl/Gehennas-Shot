package gehenna.core

import gehenna.component.Logger
import gehenna.component.Position
import gehenna.component.Senses
import gehenna.component.behaviour.PlayerBehaviour
import gehenna.core.Action.Companion.oneTurn
import gehenna.factory.Factory
import gehenna.factory.LevelPart
import gehenna.level.Level
import gehenna.level.LevelFactory
import gehenna.ui.UIContext
import gehenna.utils.SaveData

class Game(override val factory: Factory<Entity>, override val partFactory: Factory<LevelPart>) : Context {
    override lateinit var player: Entity
        private set
    private var globalTime: Long = 0
    override val actionQueue = ActionQueue
    override val time: Long get() = globalTime + (actionQueue.peek()?.waitTime ?: 0)
    override lateinit var levelFactory: LevelFactory<out Level>
    override val levels = ArrayList<Level>()

    fun init(levelFactory: LevelFactory<out Level>) {
        this.levelFactory = levelFactory
        player = factory.new("player")
//        levelFactory.size = Size(8 * 8, 7 * 8)
        val (level, pos) = levelFactory.new()
        level.spawn(player, pos)
    }

    fun initFromSave(saveData: SaveData, levelFactory: LevelFactory<out Level>) {
        this.levelFactory = levelFactory
        levels.addAll(saveData.levels)
        player = saveData.player
        globalTime = saveData.time
        levels.forEach {
            it.getAll().forEach { entity ->
                entity.any<ActiveComponent>()?.let { activeComponent ->
                    actionQueue.add(activeComponent)
                }
            }
        }
    }

    fun isPlayerNext(): Boolean = actionQueue.firstOrNull() == player<PlayerBehaviour>()

    // TODO: Think about energy randomization / but maybe i don't really need one / you can add this in scale time inside behaviour/characterBehaviour
    suspend fun update(context: UIContext) {
        actionQueue.firstOrNull()?.let { first ->
            val time = first.waitTime
            globalTime += time
            actionQueue.toList().forEach {
                it.waitTime -= time
                if (it is Effect && !it.endless) {
                    it.duration -= time
                    if (it.duration <= 0) {
                        it.detach()
                        if (it == first) {
                            return
                        }
                    }
                }
            }

            actionQueue.remove(first)
            if (first.entity<Position>()?.level?.equals(player<Position>()?.level) != false) {
                val result = first.action().perform(context)
                first.lastResult = result
                first.waitTime += result.time
                if (result.addToQueue) actionQueue.add(first)

                val sight = player<Senses.Sight>()
                if (first.entity == player) {
                    sight?.visitFov { _, _ -> } // update fov for player
                }
                result.entries.asSequence().filter { entry: ResultEntry ->
                    player.all<Senses>().any { entry.sense == it::class }
                }.forEach { entry ->
                    when (entry.sense) {
                        Senses.Sight::class -> {
                            val pos = entry.position
                            if (pos == null || sight?.isVisible(pos) == true) {
                                when (entry) {
                                    is LogEntry -> player<Logger>()?.add(entry.text)
                                    is AnimationEntry -> entry.animation()
                                }
                            }
                        }
                    }
                }
            } else {
                // Skip behaviours from other levels
                first.waitTime += oneTurn
                actionQueue.add(first)
            }
        }
    }
}