package gehenna.exception

import kotlin.reflect.KType

class FactoryReadException(file: String, cause: Throwable) : GehennaException("Can't read $file: ${cause.message}", cause)

class UnknownSuperException(parent: String) : GehennaException("Unknown super entity: $parent")

class NoSuchBuilderException(entity: String) : GehennaException("No such entity: $entity")

class UnknownTypeException(type: KType) : GehennaException("Unknown component parameter type: $type")

class UnknownArgumentException(name: String) : GehennaException("Unknown component argument name: $name")

class BadComponentException(name: String) : GehennaException("Bad component: $name")

class BadMutatorException(name: String) : GehennaException("Bad hook: $name")

class NotAnItemException(name: String) : GehennaException("Is not an <Entity> or entity is not an item: $name")

class ReadException(name: String, cause: Throwable) : GehennaException("Can't read $name: ${cause.message}", cause)

class UnknownEntityConfigException(config: String) : GehennaException("Can't config entity spawn with $config")
