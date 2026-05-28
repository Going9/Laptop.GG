package going9.laptopgg.application.crawler.port.out

import going9.laptopgg.domain.laptop.LaptopPriceHistory

interface LaptopPriceHistoryPort {
    fun save(priceHistory: LaptopPriceHistory): LaptopPriceHistory
}
