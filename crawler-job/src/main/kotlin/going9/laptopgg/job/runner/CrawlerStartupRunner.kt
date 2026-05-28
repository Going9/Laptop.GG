package going9.laptopgg.job.runner

import going9.laptopgg.job.config.CrawlerJobProperties
import kotlin.system.exitProcess
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
    private val crawlerJobExecutor: CrawlerJobExecutor,
    private val crawlerJobProperties: CrawlerJobProperties,
) : ApplicationRunner {
    override fun run(args: ApplicationArguments) {
        val exitCode = crawlerJobExecutor.execute(
            CrawlerJobRequest(
                limit = crawlerJobProperties.resolvedLimit(),
                startPage = crawlerJobProperties.resolvedStartPage(),
                filterProfile = crawlerJobProperties.resolvedFilterProfile(),
            ),
        )
        exitProcess(SpringApplication.exit(applicationContext, { exitCode }))
    }
}
