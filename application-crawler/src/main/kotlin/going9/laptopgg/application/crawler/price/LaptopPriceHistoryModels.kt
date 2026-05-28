package going9.laptopgg.application.crawler.price

import java.time.LocalDateTime

data class RecordPriceHistoryCommand(
    val laptopId: Long,
    val price: Int,
    val capturedAt: LocalDateTime,
)
