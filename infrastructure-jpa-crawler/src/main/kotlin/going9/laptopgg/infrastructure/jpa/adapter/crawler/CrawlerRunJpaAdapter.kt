package going9.laptopgg.infrastructure.jpa.adapter.crawler

import going9.laptopgg.application.crawler.common.CrawlerInvalidStateException
import going9.laptopgg.application.crawler.run.CreateCrawlerRunCommand
import going9.laptopgg.application.crawler.run.CrawlerRunState
import going9.laptopgg.application.crawler.run.CrawlerRunStatusResult
import going9.laptopgg.application.crawler.run.UpdateCrawlerRunCommand
import going9.laptopgg.application.crawler.run.port.CrawlerRunPort
import going9.laptopgg.persistence.model.crawler.CrawlerRun
import going9.laptopgg.persistence.model.crawler.CrawlerRunStatus
import going9.laptopgg.infrastructure.jpa.repository.crawler.CrawlerRunRepository
import org.springframework.stereotype.Component

@Component
internal class CrawlerRunJpaAdapter(
    private val crawlerRunRepository: CrawlerRunRepository,
) : CrawlerRunPort {
    override fun create(command: CreateCrawlerRunCommand): CrawlerRunState {
        return crawlerRunRepository.save(
            CrawlerRun(
                filterProfile = command.filterProfile.storageValue,
                startPage = command.startPage,
                limitCount = command.limitCount,
                status = command.status.toEntityStatus(),
                startedAt = command.startedAt,
                endedAt = command.endedAt,
                errorMessage = command.errorMessage,
            ),
        ).toState()
    }

    override fun update(command: UpdateCrawlerRunCommand): CrawlerRunState? {
        val updatedRows = if (command.hasCompletionCounts()) {
            crawlerRunRepository.updateCompletionById(
                runId = command.runId,
                status = command.status.toEntityStatus(),
                processedCount = command.processedCountValue(),
                createdCount = command.createdCountValue(),
                updatedCount = command.updatedCountValue(),
                detailRefreshCount = command.detailRefreshCountValue(),
                priceOnlyUpdatedCount = command.priceOnlyUpdatedCountValue(),
                degradedCount = command.degradedCountValue(),
                failedCount = command.failedCountValue(),
                failureSamples = command.failureSamples,
                errorMessage = command.errorMessage,
                endedAt = command.endedAt,
            )
        } else {
            crawlerRunRepository.updateFailureById(
                runId = command.runId,
                status = command.status.toEntityStatus(),
                errorMessage = command.errorMessage,
                endedAt = command.endedAt,
            )
        }

        if (updatedRows == 0) {
            return null
        }
        return CrawlerRunState(id = command.runId, status = command.status)
    }

    private fun CrawlerRunStatusResult.toEntityStatus(): CrawlerRunStatus {
        return when (this) {
            CrawlerRunStatusResult.RUNNING -> CrawlerRunStatus.RUNNING
            CrawlerRunStatusResult.SUCCEEDED -> CrawlerRunStatus.SUCCEEDED
            CrawlerRunStatusResult.FAILED -> CrawlerRunStatus.FAILED
            CrawlerRunStatusResult.SKIPPED_LOCKED -> CrawlerRunStatus.SKIPPED_LOCKED
        }
    }

    private fun CrawlerRun.toState(): CrawlerRunState {
        return CrawlerRunState(
            id = id ?: throw CrawlerInvalidStateException("Persisted crawler run id must not be null."),
            status = when (status) {
                CrawlerRunStatus.RUNNING -> CrawlerRunStatusResult.RUNNING
                CrawlerRunStatus.SUCCEEDED -> CrawlerRunStatusResult.SUCCEEDED
                CrawlerRunStatus.FAILED -> CrawlerRunStatusResult.FAILED
                CrawlerRunStatus.SKIPPED_LOCKED -> CrawlerRunStatusResult.SKIPPED_LOCKED
            },
        )
    }

    private fun UpdateCrawlerRunCommand.hasCompletionCounts(): Boolean {
        val counts = listOf(
            processedCount,
            createdCount,
            updatedCount,
            detailRefreshCount,
            priceOnlyUpdatedCount,
            degradedCount,
            failedCount,
        )
        if (counts.all { it != null }) {
            return true
        }
        if (counts.all { it == null }) {
            return false
        }
        throw CrawlerInvalidStateException("Crawler run completion counts must be all present or all absent.")
    }

    private fun UpdateCrawlerRunCommand.processedCountValue(): Int {
        return processedCount ?: throw missingCompletionCount("processedCount")
    }

    private fun UpdateCrawlerRunCommand.createdCountValue(): Int {
        return createdCount ?: throw missingCompletionCount("createdCount")
    }

    private fun UpdateCrawlerRunCommand.updatedCountValue(): Int {
        return updatedCount ?: throw missingCompletionCount("updatedCount")
    }

    private fun UpdateCrawlerRunCommand.detailRefreshCountValue(): Int {
        return detailRefreshCount ?: throw missingCompletionCount("detailRefreshCount")
    }

    private fun UpdateCrawlerRunCommand.priceOnlyUpdatedCountValue(): Int {
        return priceOnlyUpdatedCount ?: throw missingCompletionCount("priceOnlyUpdatedCount")
    }

    private fun UpdateCrawlerRunCommand.degradedCountValue(): Int {
        return degradedCount ?: throw missingCompletionCount("degradedCount")
    }

    private fun UpdateCrawlerRunCommand.failedCountValue(): Int {
        return failedCount ?: throw missingCompletionCount("failedCount")
    }

    private fun missingCompletionCount(fieldName: String): CrawlerInvalidStateException {
        return CrawlerInvalidStateException("Crawler run completion count is missing: $fieldName.")
    }
}
