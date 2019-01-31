package gehenna.exceptions

import kotlin.reflect.KType

class FactoryReadException(file: String, cause: Throwable) : GehennaException("Can't read $file: ${cause.message}", cause)

class UnknownSuperException(parent: String) : GehennaException("Unknown super entity: $parent")

class NoSuchEntityException(entity: String) : GehennaException("No such entity: $entity")

class UnknownTypeException(type: KType) : GehennaException("Unknown component parameter type: $type")

class UnknownArgumentException(name: String) : GehennaException("Unknown component argument name: $name")

class BadComponentException(name: String) : GehennaException("Bad component: $name")

class NotAnItemException(name: String) : GehennaException("Is not an <Entity> or entity is not an item: $name")

class EntityReadException(entity: String, cause: Throwable) : GehennaException("Can't read $entity: ${cause.message}", cause)
