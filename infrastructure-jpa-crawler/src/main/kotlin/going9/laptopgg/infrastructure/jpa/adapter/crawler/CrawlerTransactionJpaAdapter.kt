package going9.laptopgg.infrastructure.jpa.adapter.crawler

import going9.laptopgg.application.crawler.common.port.CrawlerTransactionPort
import going9.laptopgg.infrastructure.jpa.transaction.JpaTransactionExecutor
import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager

@Component
internal class CrawlerTransactionJpaAdapter(
    transactionManager: PlatformTransactionManager,
) : CrawlerTransactionPort {
    private val transactionExecutor = JpaTransactionExecutor(transactionManager)

    override fun <T> read(block: () -> T): T {
        return transactionExecutor.read(block)
    }

    override fun <T> write(block: () -> T): T {
        return transactionExecutor.write(block)
    }
}
