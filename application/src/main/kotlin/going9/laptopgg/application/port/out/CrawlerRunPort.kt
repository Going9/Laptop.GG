package going9.laptopgg.application.port.out

import going9.laptopgg.domain.crawler.CrawlerRun

interface CrawlerRunPort {
    fun save(crawlerRun: CrawlerRun): CrawlerRun
    fun findById(runId: Long): CrawlerRun?
}
