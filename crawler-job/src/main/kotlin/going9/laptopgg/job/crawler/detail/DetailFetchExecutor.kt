package going9.laptopgg.job.crawler.detail

import going9.laptopgg.job.crawler.support.isCrawlerInterruptedFailure
import java.io.Closeable
import java.time.Duration
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

internal class DetailFetchExecutor private constructor(
    private val executorService: ExecutorService,
    private val shutdownTimeout: Duration,
) : Closeable {
    fun fetch(
        workItems: List<DetailRefreshWorkItem>,
        task: (DetailRefreshWorkItem) -> DetailRefreshOutcome,
    ): List<DetailRefreshOutcome> {
        if (workItems.isEmpty()) {
            return emptyList()
        }

        val futures = workItems.map { workItem ->
            executorService.submit<DetailRefreshOutcome> {
                task(workItem)
            }
        }

        return try {
            futures.map { future -> future.getOutcome() }
        } catch (failure: Throwable) {
            cancelIncompleteFutures(futures)
            throw failure
        }
    }

    private fun cancelIncompleteFutures(futures: List<Future<DetailRefreshOutcome>>) {
        futures
            .filterNot(Future<DetailRefreshOutcome>::isDone)
            .forEach { future -> future.cancel(true) }
    }

    private fun Future<DetailRefreshOutcome>.getOutcome(): DetailRefreshOutcome {
        return try {
            get()
        } catch (exception: ExecutionException) {
            val cause = exception.cause ?: exception
            cause.isCrawlerInterruptedFailure()
            throw cause
        } catch (exception: InterruptedException) {
            Thread.currentThread().interrupt()
            throw exception
        }
    }

    override fun close() {
        executorService.shutdown()
        try {
            if (!executorService.awaitTermination(shutdownTimeout.toMillis(), TimeUnit.MILLISECONDS)) {
                executorService.shutdownNow()
            }
        } catch (exception: InterruptedException) {
            executorService.shutdownNow()
            Thread.currentThread().interrupt()
        }
    }

    companion object {
        private val threadSequence = AtomicInteger(1)
        private val defaultShutdownTimeout: Duration = Duration.ofSeconds(30)

        fun fixed(concurrency: Int): DetailFetchExecutor {
            val threadPool = Executors.newFixedThreadPool(concurrency) { runnable ->
                Thread(runnable, "crawler-detail-fetch-${threadSequence.getAndIncrement()}")
            }
            return DetailFetchExecutor(threadPool, defaultShutdownTimeout)
        }
    }
}
