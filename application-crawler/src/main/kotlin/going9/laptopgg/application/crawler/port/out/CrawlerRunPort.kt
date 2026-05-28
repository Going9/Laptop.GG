package going9.laptopgg.application.crawler.port.out

import going9.laptopgg.application.crawler.CreateCrawlerRunCommand
import going9.laptopgg.application.crawler.CrawlerRunState
import going9.laptopgg.application.crawler.UpdateCrawlerRunCommand

interface CrawlerRunPort {
    fun create(command: CreateCrawlerRunCommand): CrawlerRunState
    fun update(command: UpdateCrawlerRunCommand): CrawlerRunState?
    fun findById(runId: Long): CrawlerRunState?
}
