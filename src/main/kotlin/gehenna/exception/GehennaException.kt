package gehenna.exception

import gehenna.core.Component
import kotlin.reflect.KClass

open class GehennaException(message: String, cause: Throwable? = null) : Exception(message, cause)

class EntityMustHaveOneException(entity: String, component: KClass<out Component>) :
        GehennaException("Entity $entity expected to have exactly one component of class: $component")