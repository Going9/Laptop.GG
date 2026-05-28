package going9.laptopgg.job.crawler.detail

import java.io.Closeable
import java.time.Duration
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
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

        return workItems.map { workItem ->
            executorService.submit<DetailRefreshOutcome> {
                task(workItem)
            }
        }.map { future ->
            future.get()
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
