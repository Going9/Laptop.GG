package going9.laptopgg.infrastructure.jpa.adapter.crawler

import going9.laptopgg.application.crawler.common.CrawlerInvalidStateException
import going9.laptopgg.application.crawler.run.CompleteCrawlerRunCommand
import going9.laptopgg.application.crawler.run.CreateCrawlerRunCommand
import going9.laptopgg.application.crawler.run.CrawlerRunState
import going9.laptopgg.application.crawler.run.CrawlerRunStatusResult
import going9.laptopgg.application.crawler.run.FailCrawlerRunCommand
import going9.laptopgg.application.crawler.run.UpdateCrawlerRunCommand
import going9.laptopgg.application.crawler.run.port.CrawlerRunPort
import going9.laptopgg.infrastructure.jpa.repository.crawler.CrawlerRunRepository
import going9.laptopgg.persistence.model.crawler.CrawlerRun
import going9.laptopgg.persistence.model.crawler.CrawlerRunStatus
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
        val updatedRows = when (command) {
            is CompleteCrawlerRunCommand -> updateCompletion(command)
            is FailCrawlerRunCommand -> updateFailure(command)
        }

        if (updatedRows == 0) {
            return null
        }
        return CrawlerRunState(id = command.runId, status = command.status)
    }

    private fun updateCompletion(command: CompleteCrawlerRunCommand): Int {
        return crawlerRunRepository.updateCompletionById(
            runId = command.runId,
            status = command.status.toEntityStatus(),
            processedCount = command.processedCount,
            createdCount = command.createdCount,
            updatedCount = command.updatedCount,
            detailRefreshCount = command.detailRefreshCount,
            priceOnlyUpdatedCount = command.priceOnlyUpdatedCount,
            degradedCount = command.degradedCount,
            failedCount = command.failedCount,
            failureSamples = command.failureSamples,
            errorMessage = command.errorMessage,
            endedAt = command.endedAt,
        )
    }

    private fun updateFailure(command: FailCrawlerRunCommand): Int {
        return crawlerRunRepository.updateFailureById(
            runId = command.runId,
            status = command.status.toEntityStatus(),
            errorMessage = command.errorMessage,
            endedAt = command.endedAt,
        )
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

}
