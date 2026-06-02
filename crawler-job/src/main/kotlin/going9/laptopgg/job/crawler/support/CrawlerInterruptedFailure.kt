package going9.laptopgg.job.crawler.support

internal fun Throwable.isCrawlerInterruptedFailure(): Boolean {
    if (Thread.currentThread().isInterrupted) {
        return true
    }

    var current: Throwable? = this
    while (current != null) {
        if (current is InterruptedException) {
            Thread.currentThread().interrupt()
            return true
        }
        current = current.cause
    }

    return false
}
