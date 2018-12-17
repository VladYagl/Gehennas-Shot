import org.junit.jupiter.api.BeforeAll

open class ManagerTest {
    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            ComponentManager.clear()
        }
    }
}