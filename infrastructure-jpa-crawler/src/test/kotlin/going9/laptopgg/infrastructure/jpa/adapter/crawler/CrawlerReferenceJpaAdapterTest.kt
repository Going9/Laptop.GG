package going9.laptopgg.infrastructure.jpa.adapter.crawler

import going9.laptopgg.application.crawler.price.RecordPriceHistoryCommand
import going9.laptopgg.application.crawler.profile.LaptopProfileSnapshot
import going9.laptopgg.application.crawler.profile.UpsertCrawledLaptopProfileCommand
import going9.laptopgg.infrastructure.jpa.repository.crawler.CrawlerLaptopProfileRepository
import going9.laptopgg.infrastructure.jpa.repository.crawler.LaptopPriceHistoryRepository
import going9.laptopgg.persistence.model.crawler.LaptopPriceHistory
import going9.laptopgg.persistence.model.laptop.Laptop
import going9.laptopgg.persistence.model.laptop.LaptopProfile
import going9.laptopgg.taxonomy.BatteryTier
import going9.laptopgg.taxonomy.CpuClass
import going9.laptopgg.taxonomy.GpuClass
import going9.laptopgg.taxonomy.PortabilityTier
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito
import java.time.LocalDateTime

class CrawlerReferenceJpaAdapterTest {
    @Test
    fun `price history adapter saves laptop reference through entity manager`() {
        val repository = Mockito.mock(LaptopPriceHistoryRepository::class.java)
        val entityManager = Mockito.mock(EntityManager::class.java)
        val laptopReference = laptop()
        Mockito.`when`(entityManager.getReference(Laptop::class.java, 30L)).thenReturn(laptopReference)
        val adapter = LaptopPriceHistoryJpaAdapter(repository, entityManager)

        adapter.save(
            RecordPriceHistoryCommand(
                laptopId = 30L,
                price = 1_390_000,
                capturedAt = LocalDateTime.of(2026, 5, 29, 14, 0),
            ),
        )

        val historyCaptor = ArgumentCaptor.forClass(LaptopPriceHistory::class.java)
        Mockito.verify(repository).save(historyCaptor.capture())
        assertThat(historyCaptor.value.laptop).isSameAs(laptopReference)
        assertThat(historyCaptor.value.price).isEqualTo(1_390_000)
        Mockito.verify(entityManager).getReference(Laptop::class.java, 30L)
    }

    @Test
    fun `profile adapter creates laptop reference through entity manager for new profile`() {
        val repository = Mockito.mock(CrawlerLaptopProfileRepository::class.java)
        val entityManager = Mockito.mock(EntityManager::class.java)
        val laptopReference = laptop()
        Mockito.`when`(repository.findByLaptopId(30L)).thenReturn(null)
        Mockito.`when`(entityManager.getReference(Laptop::class.java, 30L)).thenReturn(laptopReference)
        Mockito.`when`(repository.save(Mockito.any(LaptopProfile::class.java)))
            .thenAnswer { invocation -> invocation.arguments[0] as LaptopProfile }
        val adapter = CrawledLaptopProfileJpaAdapter(repository, entityManager)

        val result = adapter.upsert(
            UpsertCrawledLaptopProfileCommand(
                laptopId = 30L,
                profile = profileSnapshot(),
            ),
        )

        val profileCaptor = ArgumentCaptor.forClass(LaptopProfile::class.java)
        Mockito.verify(repository).save(profileCaptor.capture())
        assertThat(profileCaptor.value.laptop).isSameAs(laptopReference)
        assertThat(result.laptopId).isEqualTo(30L)
        Mockito.verify(entityManager).getReference(Laptop::class.java, 30L)
    }

    private fun laptop(): Laptop {
        return Laptop(
            name = "Laptop 30",
            imageUrl = "https://img.example.com/30.jpg",
            detailPage = "https://prod.danawa.com/info/?pcode=30",
            productCode = "30",
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
            id = 30L,
        )
    }

    private fun profileSnapshot(): LaptopProfileSnapshot {
        return LaptopProfileSnapshot(
            cpuClass = CpuClass.PERFORMANCE,
            gpuClass = GpuClass.INTEGRATED_MAINSTREAM,
            batteryTier = BatteryTier.HIGH,
            portabilityTier = PortabilityTier.LIGHT,
            officeScore = 80,
            batteryScore = 70,
            casualGameScore = 60,
            onlineGameScore = 50,
            aaaGameScore = 40,
            creatorScore = 65,
            cpuPerformanceScore = 85,
            lowPowerCpuScore = 75,
            gpuPerformanceScore = 55,
            gpuCreatorBonus = 5,
            portabilityScore = 70,
            displayScore = 80,
            ramScore = 75,
            tgpScore = 0,
        )
    }
}
