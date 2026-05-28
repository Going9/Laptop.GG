package going9.laptopgg.infrastructure.jpa.repository.crawler

import going9.laptopgg.domain.crawler.CrawlerRun
import org.springframework.data.jpa.repository.JpaRepository

interface CrawlerRunRepository : JpaRepository<CrawlerRun, Long>
