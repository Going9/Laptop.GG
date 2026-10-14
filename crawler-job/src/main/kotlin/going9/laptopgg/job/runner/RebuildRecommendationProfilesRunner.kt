package going9.laptopgg.job.runner

import going9.laptopgg.application.crawler.profile.RebuildRecommendationProfilesUseCase
import going9.laptopgg.application.crawler.run.CrawlerRunLockUseCase
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component
import kotlin.system.exitProcess

@Component
@ConditionalOnProperty(name = ["app.crawler.rebuild-profiles"], havingValue = "true")
internal class RebuildRecommendationProfilesRunner(
    private val rebuildProfiles: RebuildRecommendationProfilesUseCase,
    private val lock: CrawlerRunLockUseCase,
    private val context: ConfigurableApplicationContext,
    private val environment: Environment,
) : ApplicationRunner {
    override fun run(args: ApplicationArguments) {
        check(!environment.getProperty("app.crawler.run-on-startup", Boolean::class.java, false)) {
            "Disable app.crawler.run-on-startup when rebuilding profiles."
        }
        val result = lock.runLocked { rebuildProfiles.rebuild() }
        check(result.acquired) { "Crawler is running; retry the profile rebuild after it finishes." }
        LoggerFactory.getLogger(javaClass).info("RECOMMENDATION_PROFILES_REBUILT count={}", result.value)
        exitProcess(SpringApplication.exit(context, { 0 }))
    }
}
