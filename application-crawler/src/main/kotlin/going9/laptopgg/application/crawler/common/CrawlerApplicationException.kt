package going9.laptopgg.application.crawler.common

sealed class CrawlerApplicationException(message: String) : RuntimeException(message)

class CrawlerResourceNotFoundException(
    val resourceName: String,
    val resourceId: Any,
) : CrawlerApplicationException("$resourceName not found: $resourceId")

class CrawlerInvalidCommandException(message: String) : CrawlerApplicationException(message)

class CrawlerInvalidStateException(message: String) : CrawlerApplicationException(message)
