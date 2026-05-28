package going9.laptopgg.application.crawler

import going9.laptopgg.application.crawler.port.out.CrawlerTransactionPort

class InMemoryCrawlerTransactionPort : CrawlerTransactionPort {
    override fun <T> read(block: () -> T): T {
        return block()
    }

    override fun <T> write(block: () -> T): T {
        return block()
    }
}
