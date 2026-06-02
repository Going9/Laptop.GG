package going9.laptopgg.application.recommendation

import going9.laptopgg.application.common.PageQuery
import going9.laptopgg.application.common.PagedResult
import going9.laptopgg.application.common.InvalidCommandException
import going9.laptopgg.application.common.port.ApplicationTransactionPort
import going9.laptopgg.application.recommendation.port.RecommendationCandidatePageQuery
import going9.laptopgg.application.recommendation.port.RecommendationCandidatePort
import going9.laptopgg.application.recommendation.port.RecommendationCandidateRecord
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class RecommendLaptopsUseCaseTransactionTest {
    @Test
    fun `recommendation query runs inside application read transaction`() {
        val transactionPort = RecordingApplicationTransactionPort()
        val candidatePort = RecordingRecommendationCandidatePort(transactionPort)
        val useCase = RecommendationUseCaseAssembler.createRecommendLaptopsUseCase(
            recommendationCandidatePort = candidatePort,
            transactionPort = transactionPort,
        )

        useCase.recommend(LaptopRecommendationQuery(), PageQuery(page = 0, size = 10))

        assertThat(transactionPort.readCount).isEqualTo(1)
        assertThat(transactionPort.writeCount).isZero()
        assertThat(candidatePort.calledInsideReadTransaction).isTrue()
    }

    @Test
    fun `recommendation query rejects invalid page query before persistence`() {
        val transactionPort = RecordingApplicationTransactionPort()
        val candidatePort = RecordingRecommendationCandidatePort(transactionPort)
        val useCase = RecommendationUseCaseAssembler.createRecommendLaptopsUseCase(
            recommendationCandidatePort = candidatePort,
            transactionPort = transactionPort,
        )

        assertThatThrownBy {
            useCase.recommend(LaptopRecommendationQuery(), PageQuery(page = -1, size = 10))
        }.isInstanceOf(InvalidCommandException::class.java)
        assertThatThrownBy {
            useCase.recommend(LaptopRecommendationQuery(), PageQuery(page = 0, size = 0))
        }.isInstanceOf(InvalidCommandException::class.java)
        assertThatThrownBy {
            useCase.recommend(LaptopRecommendationQuery(), PageQuery(page = 0, size = PageQuery.MAX_SIZE + 1))
        }.isInstanceOf(InvalidCommandException::class.java)

        assertThat(transactionPort.readCount).isZero()
        assertThat(candidatePort.callCount).isZero()
    }

    @Test
    fun `recommendation query rejects invalid recommendation inputs before persistence`() {
        val transactionPort = RecordingApplicationTransactionPort()
        val candidatePort = RecordingRecommendationCandidatePort(transactionPort)
        val useCase = RecommendationUseCaseAssembler.createRecommendLaptopsUseCase(
            recommendationCandidatePort = candidatePort,
            transactionPort = transactionPort,
        )
        val invalidQueries = listOf(
            LaptopRecommendationQuery(budget = 0),
            LaptopRecommendationQuery(maxWeightKg = 0.0),
            LaptopRecommendationQuery(maxWeightKg = Double.NaN),
            LaptopRecommendationQuery(screenSizeMode = ScreenSizeMode.SELECT, screenSizes = emptyList()),
            LaptopRecommendationQuery(screenSizes = listOf(12)),
        )

        invalidQueries.forEach { query ->
            assertThatThrownBy {
                useCase.recommend(query, PageQuery(page = 0, size = 10))
            }.isInstanceOf(InvalidCommandException::class.java)
        }

        assertThat(transactionPort.readCount).isZero()
        assertThat(candidatePort.callCount).isZero()
    }

    private class RecordingApplicationTransactionPort : ApplicationTransactionPort {
        var readCount = 0
            private set
        var writeCount = 0
            private set
        var insideReadTransaction = false
            private set

        override fun <T> read(block: () -> T): T {
            readCount++
            insideReadTransaction = true
            return try {
                block()
            } finally {
                insideReadTransaction = false
            }
        }

        override fun <T> write(block: () -> T): T {
            writeCount++
            return block()
        }
    }

    private class RecordingRecommendationCandidatePort(
        private val transactionPort: RecordingApplicationTransactionPort,
    ) : RecommendationCandidatePort {
        var callCount = 0
            private set
        var calledInsideReadTransaction = false
            private set

        override fun findRecommendationCandidatePage(
            query: RecommendationCandidatePageQuery,
        ): PagedResult<RecommendationCandidateRecord> {
            callCount++
            calledInsideReadTransaction = transactionPort.insideReadTransaction
            return PagedResult(
                content = listOf(recommendationCandidate()),
                page = query.pageQuery.page,
                size = query.pageQuery.size,
                totalElements = 1,
                sort = query.pageQuery.sort,
            )
        }

        private fun recommendationCandidate(): RecommendationCandidateRecord {
            return RecommendationCandidateRecord(
                id = 1L,
                name = "추천 테스트 노트북",
                imageUrl = "https://example.com/laptop.jpg",
                price = 1_000_000,
                weight = 1.2,
                screenSize = 14,
                cpu = "Core Ultra 7",
                graphicsType = "Arc",
                resolution = "1920x1200",
                portabilityScore = 80,
                displayScore = 80,
                ramScore = 80,
                tgpScore = 0,
                cpuPerformanceScore = 80,
                lowPowerCpuScore = 80,
                gpuPerformanceScore = 60,
                gpuCreatorBonus = 0,
                officeScore = 90,
                batteryScore = 85,
                casualGameScore = 70,
                onlineGameScore = 60,
                aaaGameScore = 40,
                creatorScore = 70,
            )
        }
    }
}
