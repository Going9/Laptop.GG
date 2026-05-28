package going9.laptopgg.infrastructure.jpa.adapter.crawler

import going9.laptopgg.application.port.out.CrawlerRunPort
import going9.laptopgg.domain.crawler.CrawlerRun
import going9.laptopgg.infrastructure.jpa.repository.CrawlerRunRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component

@Component
class CrawlerRunJpaAdapter(
    private val crawlerRunRepository: CrawlerRunRepository,
) : CrawlerRunPort {
    override fun save(crawlerRun: CrawlerRun): CrawlerRun {
        return crawlerRunRepository.save(crawlerRun)
    }

    override fun findById(runId: Long): CrawlerRun? {
        return crawlerRunRepository.findByIdOrNull(runId)
    }
}
