abstract class Effect : WaitTime() {
    abstract var duration: Long
    abstract val action: Action
}

data class RunAndGun(
    override val entity: Entity,
    private val dir: Pair<Int, Int>,
    override var duration: Long,
    override var time: Long = 100
) : Effect() {
    override val action: Action = Shoot(entity, dir)
}
