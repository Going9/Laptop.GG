package going9.laptopgg.application.crawler.support

import going9.laptopgg.application.crawler.common.port.CrawlerTransactionPort

class InMemoryCrawlerTransactionPort : CrawlerTransactionPort {
    var readCount = 0
        private set
    var writeCount = 0
        private set

    override fun <T> read(block: () -> T): T {
        readCount++
        return block()
    }

    override fun <T> write(block: () -> T): T {
        writeCount++
        return block()
    }
}
