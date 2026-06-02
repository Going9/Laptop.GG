package going9.laptopgg.application.crawler.run.port

import going9.laptopgg.application.crawler.run.CreateCrawlerRunCommand
import going9.laptopgg.application.crawler.run.CrawlerRunState
import going9.laptopgg.application.crawler.run.UpdateCrawlerRunCommand

interface CrawlerRunPort {
    fun create(command: CreateCrawlerRunCommand): CrawlerRunState
    fun update(command: UpdateCrawlerRunCommand): CrawlerRunState?
}
