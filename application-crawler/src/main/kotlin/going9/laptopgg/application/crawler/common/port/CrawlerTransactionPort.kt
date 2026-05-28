package going9.laptopgg.application.crawler.common.port

interface CrawlerTransactionPort {
    fun <T> read(block: () -> T): T
    fun <T> write(block: () -> T): T
}
