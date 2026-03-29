package going9.laptopgg.runner

import going9.laptopgg.service.crawler.CrawlerService
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
    @Value("\${app.crawler.limit:}") private val defaultLimitRaw: String,
    @Value("\${app.crawler.start-page:}") private val defaultStartPageRaw: String,
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

        val exitCode = runCatching {
            val summary = crawlerService.crawlAll(limit = limit, startPage = startPage)
            logger.info(
                "Crawler run finished. startPage={}, processedCount={}, createdCount={}, updatedCount={}, degradedCount={}, failedCount={}",
                startPage,
                summary.processedCount,
                summary.createdCount,
                summary.updatedCount,
                summary.degradedCount,
                summary.failedCount,
            )
            if (summary.degradedSamples.isNotEmpty()) {
                logger.warn("Crawler degraded samples: {}", summary.degradedSamples)
            }
            if (summary.failureSamples.isNotEmpty()) {
                logger.warn("Crawler failure samples: {}", summary.failureSamples)
            }
            require(summary.failedCount == 0) {
                "Crawler finished with ${summary.failedCount} failed item(s)."
            }
            0
        }.getOrElse { exception ->
            logger.error("Crawler run failed.", exception)
            1
        }

        exitProcess(SpringApplication.exit(applicationContext, { exitCode }))
    }
}
