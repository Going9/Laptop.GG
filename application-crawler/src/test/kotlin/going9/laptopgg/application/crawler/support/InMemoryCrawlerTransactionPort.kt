package going9.laptopgg.application.crawler.support

import going9.laptopgg.application.crawler.common.port.CrawlerTransactionPort

class InMemoryCrawlerTransactionPort : CrawlerTransactionPort {
    override fun <T> read(block: () -> T): T {
        return block()
    }

    override fun <T> write(block: () -> T): T {
        return block()
    }
}
