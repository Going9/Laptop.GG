package going9.laptopgg.application.common

sealed class ApplicationException(message: String) : RuntimeException(message)

class ResourceNotFoundException(
    val resourceName: String,
    val resourceId: Any,
) : ApplicationException("$resourceName not found: $resourceId")

class InvalidCommandException(message: String) : ApplicationException(message)

class AuthenticationFailedException(message: String) : ApplicationException(message)

class ApplicationInvalidStateException(message: String) : ApplicationException(message)
