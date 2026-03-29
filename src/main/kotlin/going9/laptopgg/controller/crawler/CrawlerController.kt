package going9.laptopgg.controller.crawler

import going9.laptopgg.service.crawler.CrawlerService
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@Profile("!deploy")
@RequestMapping("/api/crawl")
class CrawlerController(
    private val crawlerService: CrawlerService,
) {

    @GetMapping("/laptops")
    fun startCrawling(
        @RequestParam(required = false) limit: Int?,
    ): CrawlerService.CrawlSummary {
        return crawlerService.crawlAll(limit)
    }
}
