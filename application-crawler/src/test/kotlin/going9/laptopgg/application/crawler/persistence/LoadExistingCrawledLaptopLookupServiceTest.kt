package going9.laptopgg.application.crawler.persistence

import going9.laptopgg.application.crawler.common.CrawlerInvalidCommandException
import going9.laptopgg.application.crawler.common.port.CrawlerTransactionPort
import going9.laptopgg.application.crawler.persistence.port.ExistingCrawledLaptopLookupPort
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class LoadExistingCrawledLaptopLookupServiceTest {
    private val transactionPort = RecordingTransactionPort()
    private val lookupPort = RecordingExistingCrawledLaptopLookupPort()
    private val service = LoadExistingCrawledLaptopLookupService(
        existingLookupLoader = ExistingCrawledLaptopLookupLoader(lookupPort),
        transactionPort = transactionPort,
    )

    @Test
    fun `load normalizes product cards before lookup`() {
        service.load(
            listOf(
                crawledProductCard(
                    productCode = " TX001 ",
                    productName = " Transaction Boundary 14 ",
                    detailPage = " https://prod.danawa.com/info/?pcode=TX001&cate=112758 ",
                    imageUrl = " https://example.com/transaction-boundary.jpg ",
                ),
            ),
        )

        assertThat(lookupPort.productCodeLookups).containsExactly(listOf("TX001"))
        assertThat(lookupPort.detailPageLookups)
            .containsExactly(listOf("https://prod.danawa.com/info/?pcode=TX001&cate=112758"))
        assertThat(transactionPort.readCount).isEqualTo(1)
    }

    @Test
    fun `loadExistingLookup rejects invalid product card before persistence`() {
        assertThatThrownBy {
            service.load(listOf(crawledProductCard(productCode = "")))
        }.isInstanceOf(CrawlerInvalidCommandException::class.java)

        assertThat(transactionPort.readCount).isZero()
    }

    private fun crawledProductCard(
        productCode: String = "TX001",
        productName: String = "Transaction Boundary 14",
        detailPage: String = "https://prod.danawa.com/info/?pcode=TX001&cate=112758",
        imageUrl: String = "https://example.com/transaction-boundary.jpg",
        price: Int? = 1_490_000,
    ): CrawledProductCardCommand {
        return CrawledProductCardCommand(
            productName = productName,
            imageUrl = imageUrl,
            detailPage = detailPage,
            productCode = productCode,
            price = price,
        )
    }

    private class RecordingTransactionPort : CrawlerTransactionPort {
        var readCount = 0
            private set

        override fun <T> read(block: () -> T): T {
            readCount++
            return block()
        }

        override fun <T> write(block: () -> T): T = block()
    }

    private class RecordingExistingCrawledLaptopLookupPort : ExistingCrawledLaptopLookupPort {
        val productCodeLookups = mutableListOf<List<String>>()
        val detailPageLookups = mutableListOf<List<String>>()

        override fun findExistingByProductCodes(productCodes: Collection<String>): List<ExistingCrawledLaptopSnapshot> {
            productCodeLookups += productCodes.toList()
            return emptyList()
        }

        override fun findExistingByDetailPages(detailPages: Collection<String>): List<ExistingCrawledLaptopSnapshot> {
            detailPageLookups += detailPages.toList()
            return emptyList()
        }
    }
}
