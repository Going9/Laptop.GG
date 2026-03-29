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
) : ApplicationRunner {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun run(args: ApplicationArguments) {
        val defaultLimit = defaultLimitRaw.toIntOrNull()
            ?.takeIf { it > 0 }

        val limit = args.getOptionValues("app.crawler.limit")
            ?.firstOrNull()
            ?.toIntOrNull()
            ?: defaultLimit

        val exitCode = runCatching {
            val summary = crawlerService.crawlAll(limit)
            logger.info(
                "Crawler run finished. processedCount={}, createdCount={}, updatedCount={}",
                summary.processedCount,
                summary.createdCount,
                summary.updatedCount,
            )
            0
        }.getOrElse { exception ->
            logger.error("Crawler run failed.", exception)
            1
        }

        exitProcess(SpringApplication.exit(applicationContext, { exitCode }))
    }
}
