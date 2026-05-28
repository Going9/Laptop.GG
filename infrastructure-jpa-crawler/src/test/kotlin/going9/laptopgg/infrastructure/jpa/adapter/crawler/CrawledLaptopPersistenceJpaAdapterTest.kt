package going9.laptopgg.infrastructure.jpa.adapter.crawler

import going9.laptopgg.application.crawler.common.CrawlerInvalidStateException
import going9.laptopgg.application.crawler.persistence.UpdateCrawledListSnapshotCommand
import going9.laptopgg.infrastructure.jpa.repository.crawler.CrawlerLaptopRepository
import going9.laptopgg.infrastructure.jpa.repository.crawler.CrawledListSnapshotProjection
import going9.laptopgg.infrastructure.jpa.repository.crawler.ExistingCrawledLaptopProjection
import going9.laptopgg.persistence.model.laptop.Laptop
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.time.LocalDateTime

class CrawledLaptopPersistenceJpaAdapterTest {
    @Test
    fun `findListSnapshotById maps list projection without loading full laptop graph`() {
        val repository = Mockito.mock(CrawlerLaptopRepository::class.java)
        Mockito.`when`(repository.findListSnapshotById(10L))
            .thenReturn(
                listProjection(
                    id = 10L,
                    name = "List Laptop",
                    imageUrl = "https://img.example.com/list.jpg",
                    detailPage = "https://prod.danawa.com/info/?pcode=P10",
                    productCode = "P10",
                    price = 1_190_000,
                ),
            )
        val adapter = CrawledLaptopPersistenceJpaAdapter(repository)

        val snapshot = adapter.findListSnapshotById(10L)

        assertThat(snapshot?.id).isEqualTo(10L)
        assertThat(snapshot?.name).isEqualTo("List Laptop")
        assertThat(snapshot?.price).isEqualTo(1_190_000)
        Mockito.verify(repository).findListSnapshotById(10L)
        Mockito.verifyNoMoreInteractions(repository)
    }

    @Test
    fun `updateListSnapshot delegates to direct update query without loading full laptop graph`() {
        val repository = Mockito.mock(CrawlerLaptopRepository::class.java)
        Mockito.`when`(
            repository.updateListSnapshotById(
                id = 10L,
                name = "New Name",
                imageUrl = "https://img.example.com/new.jpg",
                detailPage = null,
                productCode = null,
                price = 1_090_000,
            ),
        ).thenReturn(1)
        val adapter = CrawledLaptopPersistenceJpaAdapter(repository)

        val updated = adapter.updateListSnapshot(
            laptopId = 10L,
            command = UpdateCrawledListSnapshotCommand(
                name = "New Name",
                imageUrl = "https://img.example.com/new.jpg",
                price = 1_090_000,
            ),
        )

        assertThat(updated).isTrue()
        Mockito.verify(repository).updateListSnapshotById(
            id = 10L,
            name = "New Name",
            imageUrl = "https://img.example.com/new.jpg",
            detailPage = null,
            productCode = null,
            price = 1_090_000,
        )
        Mockito.verify(repository, Mockito.never()).findWithUsageById(Mockito.anyLong())
    }

    @Test
    fun `findByProductCode rejects duplicate crawler identities with explicit state error`() {
        val repository = Mockito.mock(CrawlerLaptopRepository::class.java)
        Mockito.`when`(repository.findAllWithUsageByProductCodeIn(listOf("P10")))
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
        Mockito.`when`(repository.findAllWithUsageByDetailPageIn(listOf(detailPage)))
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

    @Test
    fun `findExistingByProductCodes maps lookup projection without loading full laptop graph`() {
        val repository = Mockito.mock(CrawlerLaptopRepository::class.java)
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
        val adapter = CrawledLaptopPersistenceJpaAdapter(repository)

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
        val repository = Mockito.mock(CrawlerLaptopRepository::class.java)
        Mockito.`when`(repository.findExistingByProductCodeIn(listOf("P10")))
            .thenReturn(listOf(existingProjection(id = null, productCode = "P10")))
        val adapter = CrawledLaptopPersistenceJpaAdapter(repository)

        assertThatThrownBy {
            adapter.findExistingByProductCodes(listOf("P10"))
        }.isInstanceOf(CrawlerInvalidStateException::class.java)
            .hasMessageContaining("Persisted laptop id must not be null")
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

    private fun listProjection(
        id: Long?,
        name: String,
        imageUrl: String,
        detailPage: String,
        productCode: String?,
        price: Int?,
    ): CrawledListSnapshotProjection {
        return object : CrawledListSnapshotProjection {
            override val id: Long? = id
            override val name: String = name
            override val imageUrl: String = imageUrl
            override val detailPage: String = detailPage
            override val productCode: String? = productCode
            override val price: Int? = price
        }
    }
}
