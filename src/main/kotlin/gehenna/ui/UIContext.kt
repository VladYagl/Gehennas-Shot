package gehenna.ui

import gehenna.component.Logger
import gehenna.component.behaviour.PlayerBehaviour
import gehenna.core.Action
import gehenna.core.Context

class UIContext(private val context: Context, private val ui: UI) : Context by context, UI by ui {
    val log get() = player<Logger>()!!

    var action: Action? = null
        set(value) {
            value?.let { action ->
                player<PlayerBehaviour>()?.set(action)
            }
        }
}