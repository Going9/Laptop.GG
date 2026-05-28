package going9.laptopgg.application.crawler.port.out

interface CrawlerTransactionPort {
    fun <T> read(block: () -> T): T
    fun <T> write(block: () -> T): T
}
