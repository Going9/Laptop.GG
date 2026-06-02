package going9.laptopgg.job.config

import going9.laptopgg.application.crawler.assembly.CrawlerRunAssembler
import going9.laptopgg.application.crawler.common.port.CrawlerTransactionPort
import going9.laptopgg.application.crawler.run.CrawlerRunLockUseCase
import going9.laptopgg.application.crawler.run.TrackCrawlerRunUseCase
import going9.laptopgg.application.crawler.run.port.CrawlerRunLockPort
import going9.laptopgg.application.crawler.run.port.CrawlerRunPort
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
internal class CrawlerRunUseCaseConfig {
    @Bean
    fun trackCrawlerRunService(
        crawlerRunPort: CrawlerRunPort,
        transactionPort: CrawlerTransactionPort,
    ): TrackCrawlerRunUseCase {
        return CrawlerRunAssembler.createTrackCrawlerRunUseCase(
            crawlerRunPort = crawlerRunPort,
            transactionPort = transactionPort,
        )
    }

    @Bean
    fun crawlerRunLockUseCase(crawlerRunLockPort: CrawlerRunLockPort): CrawlerRunLockUseCase {
        return CrawlerRunAssembler.createCrawlerRunLockUseCase(crawlerRunLockPort)
    }
}
