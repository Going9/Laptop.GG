package going9.laptopgg.runner

import going9.laptopgg.application.crawler.CrawlerRunCompletionStatus
import going9.laptopgg.application.crawler.CrawlerRunStatusResult
import going9.laptopgg.application.crawler.CrawlerRunSummary
import going9.laptopgg.application.crawler.TrackCrawlerRunUseCase
import going9.laptopgg.job.crawler.CrawlerAdvisoryLockService
import going9.laptopgg.job.crawler.CrawlerService
import going9.laptopgg.job.crawler.CrawlSummary
import kotlin.system.exitProcess
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(name = ["app.crawler.run-on-startup"], havingValue = "true")
class CrawlerStartupRunner(
    private val applicationContext: ConfigurableApplicationContext,
    private val crawlerService: CrawlerService,
    private val crawlerAdvisoryLockService: CrawlerAdvisoryLockService,
    private val trackCrawlerRunUseCase: TrackCrawlerRunUseCase,
    @Value("\${app.crawler.limit:}") private val defaultLimitRaw: String,
    @Value("\${app.crawler.start-page:}") private val defaultStartPageRaw: String,
    @Value("\${app.crawler.filter-profile:core}") private val defaultFilterProfileRaw: String,
) : ApplicationRunner {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun run(args: ApplicationArguments) {
        val defaultLimit = defaultLimitRaw.toIntOrNull()
            ?.takeIf { it > 0 }
        val defaultStartPage = defaultStartPageRaw.toIntOrNull()
            ?.takeIf { it > 0 }

        val limit = args.getOptionValues("app.crawler.limit")
            ?.firstOrNull()
            ?.toIntOrNull()
            ?: defaultLimit
        val startPage = args.getOptionValues("app.crawler.start-page")
            ?.firstOrNull()
            ?.toIntOrNull()
            ?: defaultStartPage
            ?: 1
        val filterProfile = args.getOptionValues("app.crawler.filter-profile")
            ?.firstOrNull()
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: defaultFilterProfileRaw

        val lockResult = runCatching {
            crawlerAdvisoryLockService.withCrawlerLock {
                runTrackedCrawler(limit = limit, startPage = startPage, filterProfile = filterProfile)
            }
        }.getOrElse { exception ->
            logger.error("Crawler lock acquisition failed.", exception)
            exitProcess(SpringApplication.exit(applicationContext, { 1 }))
        }

        val exitCode = if (lockResult.acquired) {
            lockResult.value ?: 1
        } else {
            val skippedRun = trackCrawlerRunUseCase.skipLocked(
                filterProfile = filterProfile,
                startPage = startPage,
                limit = limit,
            )
            logger.warn(
                "CRAWLER_SUMMARY runId={} status={} filterProfile={} startPage={} limit={} processedCount=0 createdCount=0 updatedCount=0 degradedCount=0 failedCount=0",
                skippedRun.id,
                skippedRun.status,
                filterProfile,
                startPage,
                limit ?: "ALL",
            )
            0
        }

        exitProcess(SpringApplication.exit(applicationContext, { exitCode }))
    }

    private fun runTrackedCrawler(limit: Int?, startPage: Int, filterProfile: String): Int {
        val crawlerRun = trackCrawlerRunUseCase.start(
            filterProfile = filterProfile,
            startPage = startPage,
            limit = limit,
        )
        val runId = requireNotNull(crawlerRun.id)

        return runCatching {
            val summary = crawlerService.crawlAll(limit = limit, startPage = startPage, filterProfileRaw = filterProfile)
            val finishedStatus = if (summary.failedCount == 0) {
                CrawlerRunCompletionStatus.SUCCEEDED
            } else {
                CrawlerRunCompletionStatus.FAILED
            }
            val errorMessage = if (summary.failedCount == 0) {
                null
            } else {
                "Crawler finished with ${summary.failedCount} failed item(s)."
            }
            trackCrawlerRunUseCase.finish(
                runId = runId,
                summary = summary.toRunSummary(),
                status = finishedStatus,
                errorMessage = errorMessage,
            )
            logCrawlerSummary(runId, finishedStatus, filterProfile, startPage, limit, summary)
            if (summary.degradedSamples.isNotEmpty()) {
                logger.warn("Crawler degraded samples: {}", summary.degradedSamples)
            }
            if (summary.failureSamples.isNotEmpty()) {
                logger.warn("Crawler failure samples: {}", summary.failureSamples)
            }
            if (summary.failedCount == 0) 0 else 1
        }.getOrElse { exception ->
            trackCrawlerRunUseCase.fail(runId, exception)
            logger.error("Crawler run failed. runId={}", runId, exception)
            logger.error(
                "CRAWLER_SUMMARY runId={} status={} filterProfile={} startPage={} limit={} processedCount=0 createdCount=0 updatedCount=0 degradedCount=0 failedCount=1",
                runId,
                CrawlerRunStatusResult.FAILED,
                filterProfile,
                startPage,
                limit ?: "ALL",
            )
            1
        }
    }

    private fun logCrawlerSummary(
        runId: Long,
        status: CrawlerRunCompletionStatus,
        filterProfile: String,
        startPage: Int,
        limit: Int?,
        summary: CrawlSummary,
    ) {
        logger.info(
            "CRAWLER_SUMMARY runId={} status={} filterProfile={} startPage={} limit={} processedCount={} createdCount={} updatedCount={} degradedCount={} failedCount={}",
            runId,
            status,
            filterProfile,
            startPage,
            limit ?: "ALL",
            summary.processedCount,
            summary.createdCount,
            summary.updatedCount,
            summary.degradedCount,
            summary.failedCount,
        )
    }

    private fun CrawlSummary.toRunSummary(): CrawlerRunSummary {
        return CrawlerRunSummary(
            processedCount = processedCount,
            createdCount = createdCount,
            updatedCount = updatedCount,
            degradedCount = degradedCount,
            failedCount = failedCount,
            failureSamples = failureSamples,
        )
    }
}
