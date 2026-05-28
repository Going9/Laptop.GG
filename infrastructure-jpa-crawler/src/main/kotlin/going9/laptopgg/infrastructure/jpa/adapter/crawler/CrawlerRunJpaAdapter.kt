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
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component

@Component
internal class CrawlerRunJpaAdapter(
    private val crawlerRunRepository: CrawlerRunRepository,
) : CrawlerRunPort {
    override fun create(command: CreateCrawlerRunCommand): CrawlerRunState {
        return crawlerRunRepository.save(
            CrawlerRun(
                filterProfile = command.filterProfile,
                startPage = command.startPage,
                limitCount = command.limitCount,
                status = command.status.toEntityStatus(),
                endedAt = command.endedAt,
                errorMessage = command.errorMessage,
            ),
        ).toState()
    }

    override fun update(command: UpdateCrawlerRunCommand): CrawlerRunState? {
        val crawlerRun = crawlerRunRepository.findByIdOrNull(command.runId) ?: return null
        crawlerRun.status = command.status.toEntityStatus()
        command.processedCount?.let { crawlerRun.processedCount = it }
        command.createdCount?.let { crawlerRun.createdCount = it }
        command.updatedCount?.let { crawlerRun.updatedCount = it }
        command.degradedCount?.let { crawlerRun.degradedCount = it }
        command.failedCount?.let { crawlerRun.failedCount = it }
        crawlerRun.failureSamples = command.failureSamples
        crawlerRun.errorMessage = command.errorMessage
        crawlerRun.endedAt = command.endedAt
        return crawlerRunRepository.save(crawlerRun).toState()
    }

    override fun findById(runId: Long): CrawlerRunState? {
        return crawlerRunRepository.findByIdOrNull(runId)?.toState()
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
