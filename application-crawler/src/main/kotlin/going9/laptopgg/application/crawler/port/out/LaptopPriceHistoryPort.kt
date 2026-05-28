package going9.laptopgg.application.crawler.port.out

import going9.laptopgg.application.crawler.price.RecordPriceHistoryCommand

interface LaptopPriceHistoryPort {
    fun save(command: RecordPriceHistoryCommand)
}
