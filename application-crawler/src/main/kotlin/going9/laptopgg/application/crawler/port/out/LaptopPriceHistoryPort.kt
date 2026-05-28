package going9.laptopgg.application.crawler.port.out

import going9.laptopgg.application.crawler.RecordPriceHistoryCommand

interface LaptopPriceHistoryPort {
    fun save(command: RecordPriceHistoryCommand)
}
