package going9.laptopgg.job.runner

import going9.laptopgg.job.config.CrawlerJobProperties
import kotlin.system.exitProcess
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(name = ["app.crawler.run-on-startup"], havingValue = "true")
internal class CrawlerStartupRunner(
    private val applicationContext: ConfigurableApplicationContext,
    private val crawlerJobExecutor: CrawlerJobExecutor,
    private val crawlerJobProperties: CrawlerJobProperties,
) : ApplicationRunner {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun run(args: ApplicationArguments) {
        crawlerJobProperties.validateForStartup()
        val filterProfileResolution = crawlerJobProperties.resolvedFilterProfileResolution()
        if (filterProfileResolution.usedDefaultForUnknownValue) {
            logger.warn(
                "알 수 없는 crawler filter profile='{}'. 기본값 core를 사용합니다.",
                filterProfileResolution.rawValue,
            )
        }

        val exitCode = crawlerJobExecutor.execute(
            CrawlerJobRequest(
                limit = crawlerJobProperties.resolvedLimit(),
                startPage = crawlerJobProperties.resolvedStartPage(),
                filterProfile = filterProfileResolution.profile,
            ),
        )
        exitProcess(SpringApplication.exit(applicationContext, { exitCode }))
    }
}
