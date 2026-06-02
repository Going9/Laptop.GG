package going9.laptopgg.application.crawler.price.port

import going9.laptopgg.application.crawler.price.RecordPriceHistoryCommand

interface LaptopPriceHistoryPort {
    fun save(command: RecordPriceHistoryCommand)
}
