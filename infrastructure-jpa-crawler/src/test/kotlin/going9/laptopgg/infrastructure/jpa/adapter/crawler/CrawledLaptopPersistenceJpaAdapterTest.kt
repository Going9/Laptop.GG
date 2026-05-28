package going9.laptopgg.infrastructure.jpa.adapter.crawler

import going9.laptopgg.application.crawler.common.CrawlerInvalidStateException
import going9.laptopgg.infrastructure.jpa.repository.crawler.CrawlerLaptopRepository
import going9.laptopgg.persistence.model.laptop.Laptop
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.Mockito

class CrawledLaptopPersistenceJpaAdapterTest {
    @Test
    fun `findByProductCode rejects duplicate crawler identities with explicit state error`() {
        val repository = Mockito.mock(CrawlerLaptopRepository::class.java)
        Mockito.`when`(repository.findAllByProductCodeIn(listOf("P10")))
            .thenReturn(
                listOf(
                    laptop(id = 10L, productCode = "P10", detailPage = "https://prod.danawa.com/info/?pcode=P10"),
                    laptop(id = 11L, productCode = "P10", detailPage = "https://prod.danawa.com/info/?pcode=P10-dup"),
                ),
            )
        val adapter = CrawledLaptopPersistenceJpaAdapter(repository)

        assertThatThrownBy {
            adapter.findByProductCode("P10")
        }.isInstanceOf(CrawlerInvalidStateException::class.java)
            .hasMessageContaining("productCode=P10")
            .hasMessageContaining("10")
            .hasMessageContaining("11")
    }

    @Test
    fun `findByDetailPage rejects duplicate crawler identities with explicit state error`() {
        val detailPage = "https://prod.danawa.com/info/?pcode=P20"
        val repository = Mockito.mock(CrawlerLaptopRepository::class.java)
        Mockito.`when`(repository.findAllByDetailPageIn(listOf(detailPage)))
            .thenReturn(
                listOf(
                    laptop(id = 20L, productCode = "P20", detailPage = detailPage),
                    laptop(id = 21L, productCode = "P20-dup", detailPage = detailPage),
                ),
            )
        val adapter = CrawledLaptopPersistenceJpaAdapter(repository)

        assertThatThrownBy {
            adapter.findByDetailPage(detailPage)
        }.isInstanceOf(CrawlerInvalidStateException::class.java)
            .hasMessageContaining("detailPage=$detailPage")
            .hasMessageContaining("20")
            .hasMessageContaining("21")
    }

    private fun laptop(id: Long, productCode: String, detailPage: String): Laptop {
        return Laptop(
            name = "Laptop $id",
            imageUrl = "https://img.example.com/$id.jpg",
            detailPage = detailPage,
            productCode = productCode,
            price = 1_000_000,
            cpuManufacturer = "인텔",
            cpu = "Core Ultra",
            os = "윈도우11",
            screenSize = 14,
            resolution = "1920x1200",
            brightness = 300,
            refreshRate = 60,
            ramSize = 16,
            ramType = "LPDDR5X",
            isRamReplaceable = false,
            graphicsType = "Intel Graphics",
            tgp = 0,
            thunderboltCount = 1,
            usbCCount = 2,
            usbACount = 1,
            sdCard = null,
            isSupportsPdCharging = true,
            batteryCapacity = 60.0,
            storageCapacity = 512,
            storageSlotCount = 1,
            weight = 1.2,
            id = id,
        )
    }
}
