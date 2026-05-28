package going9.laptopgg.infrastructure.jpa.adapter.crawler

import going9.laptopgg.application.crawler.common.CrawlerInvalidStateException
import going9.laptopgg.infrastructure.jpa.repository.crawler.ExistingCrawledLaptopProjection
import going9.laptopgg.infrastructure.jpa.repository.crawler.ExistingCrawledLaptopLookupRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.time.LocalDateTime

class ExistingCrawledLaptopLookupJpaAdapterTest {
    @Test
    fun `findExistingByProductCodes maps lookup projection without loading full laptop graph`() {
        val repository = Mockito.mock(ExistingCrawledLaptopLookupRepository::class.java)
        val detailedAt = LocalDateTime.of(2026, 5, 29, 12, 0)
        Mockito.`when`(repository.findExistingByProductCodeIn(listOf("P10")))
            .thenReturn(
                listOf(
                    existingProjection(
                        id = 10L,
                        productCode = "P10",
                        detailPage = "https://prod.danawa.com/info/?pcode=P10",
                        lastDetailedCrawledAt = detailedAt,
                        usageCount = 2L,
                    ),
                ),
            )
        val adapter = ExistingCrawledLaptopLookupJpaAdapter(repository)

        val snapshots = adapter.findExistingByProductCodes(listOf("P10"))

        assertThat(snapshots).hasSize(1)
        val snapshot = snapshots.single()
        assertThat(snapshot.id).isEqualTo(10L)
        assertThat(snapshot.productCode).isEqualTo("P10")
        assertThat(snapshot.detailPage).isEqualTo("https://prod.danawa.com/info/?pcode=P10")
        assertThat(snapshot.lastDetailedCrawledAt).isEqualTo(detailedAt)
        assertThat(snapshot.usageCount).isEqualTo(2)
        Mockito.verify(repository).findExistingByProductCodeIn(listOf("P10"))
        Mockito.verifyNoMoreInteractions(repository)
    }

    @Test
    fun `findExistingByProductCodes rejects projection without generated id with explicit state error`() {
        val repository = Mockito.mock(ExistingCrawledLaptopLookupRepository::class.java)
        Mockito.`when`(repository.findExistingByProductCodeIn(listOf("P10")))
            .thenReturn(listOf(existingProjection(id = null, productCode = "P10")))
        val adapter = ExistingCrawledLaptopLookupJpaAdapter(repository)

        assertThatThrownBy {
            adapter.findExistingByProductCodes(listOf("P10"))
        }.isInstanceOf(CrawlerInvalidStateException::class.java)
            .hasMessageContaining("Persisted laptop id must not be null")
    }

    private fun existingProjection(
        id: Long?,
        productCode: String?,
        detailPage: String = "https://prod.danawa.com/info/?pcode=P10",
        lastDetailedCrawledAt: LocalDateTime? = null,
        usageCount: Long? = 0L,
    ): ExistingCrawledLaptopProjection {
        return object : ExistingCrawledLaptopProjection {
            override val id: Long? = id
            override val productCode: String? = productCode
            override val detailPage: String = detailPage
            override val cpuManufacturer: String? = "인텔"
            override val cpu: String? = "Core Ultra"
            override val os: String? = "윈도우11"
            override val screenSize: Int? = 14
            override val resolution: String? = "1920x1200"
            override val ramSize: Int? = 16
            override val graphicsType: String? = "Intel Graphics"
            override val storageCapacity: Int? = 512
            override val batteryCapacity: Double? = 60.0
            override val weight: Double? = 1.2
            override val lastDetailedCrawledAt: LocalDateTime? = lastDetailedCrawledAt
            override val usageCount: Long? = usageCount
        }
    }
}
