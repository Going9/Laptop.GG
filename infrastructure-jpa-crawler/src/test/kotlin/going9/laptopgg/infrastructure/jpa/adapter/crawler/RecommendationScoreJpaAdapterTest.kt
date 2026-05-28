package going9.laptopgg.infrastructure.jpa.adapter.crawler

import going9.laptopgg.application.crawler.common.CrawlerInvalidCommandException
import going9.laptopgg.application.crawler.recommendation.UpsertRecommendationScoreCommand
import going9.laptopgg.infrastructure.jpa.repository.crawler.RecommendationScoreRepository
import going9.laptopgg.persistence.model.laptop.Laptop
import going9.laptopgg.persistence.model.recommendation.RecommendationScore
import going9.laptopgg.recommendation.RecommendationUseCase
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito
import java.time.LocalDateTime

class RecommendationScoreJpaAdapterTest {
    private val repository = Mockito.mock(RecommendationScoreRepository::class.java)
    private val entityManager = Mockito.mock(EntityManager::class.java)
    private val adapter = RecommendationScoreJpaAdapter(repository, entityManager)

    @Test
    fun `saveAll updates existing recommendation score without loading score entities`() {
        val command = command(useCase = RecommendationUseCase.OFFICE_STUDY)
        Mockito.`when`(
            repository.updateByLaptopIdAndUseCase(
                laptopId = 30L,
                useCase = "OFFICE_STUDY",
                gateScore = 80,
                staticScore = 72.5,
                budgetWeight = 0.2,
                updatedAt = command.updatedAt,
            ),
        ).thenReturn(1)

        adapter.saveAll(listOf(command))

        Mockito.verify(repository).updateByLaptopIdAndUseCase(
            laptopId = 30L,
            useCase = "OFFICE_STUDY",
            gateScore = 80,
            staticScore = 72.5,
            budgetWeight = 0.2,
            updatedAt = command.updatedAt,
        )
        Mockito.verify(repository, Mockito.never()).save(Mockito.any(RecommendationScore::class.java))
        Mockito.verifyNoInteractions(entityManager)
    }

    @Test
    fun `saveAll inserts recommendation score when direct update finds no row`() {
        val command = command(useCase = RecommendationUseCase.AAA_GAME)
        val laptopReference = laptop()
        Mockito.`when`(
            repository.updateByLaptopIdAndUseCase(
                laptopId = 30L,
                useCase = "AAA_GAME",
                gateScore = 80,
                staticScore = 72.5,
                budgetWeight = 0.2,
                updatedAt = command.updatedAt,
            ),
        ).thenReturn(0)
        Mockito.`when`(entityManager.getReference(Laptop::class.java, 30L)).thenReturn(laptopReference)

        adapter.saveAll(listOf(command))

        val scoreCaptor = ArgumentCaptor.forClass(RecommendationScore::class.java)
        Mockito.verify(repository).save(scoreCaptor.capture())
        assertThat(scoreCaptor.value.laptop).isSameAs(laptopReference)
        assertThat(scoreCaptor.value.useCase).isEqualTo("AAA_GAME")
        assertThat(scoreCaptor.value.gateScore).isEqualTo(80)
        assertThat(scoreCaptor.value.staticScore).isEqualTo(72.5)
        assertThat(scoreCaptor.value.budgetWeight).isEqualTo(0.2)
        assertThat(scoreCaptor.value.updatedAt).isEqualTo(command.updatedAt)
    }

    @Test
    fun `saveAll rejects commands for multiple laptops before persistence`() {
        assertThatThrownBy {
            adapter.saveAll(
                listOf(
                    command(laptopId = 30L),
                    command(laptopId = 31L),
                ),
            )
        }.isInstanceOf(CrawlerInvalidCommandException::class.java)
            .hasMessageContaining("single laptop")

        Mockito.verifyNoInteractions(repository, entityManager)
    }

    private fun command(
        laptopId: Long = 30L,
        useCase: RecommendationUseCase = RecommendationUseCase.NOT_SURE,
    ): UpsertRecommendationScoreCommand {
        return UpsertRecommendationScoreCommand(
            laptopId = laptopId,
            useCase = useCase,
            gateScore = 80,
            staticScore = 72.5,
            budgetWeight = 0.2,
            updatedAt = LocalDateTime.of(2026, 5, 29, 13, 10),
        )
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
}
