package going9.laptopgg.application.crawler.run

import going9.laptopgg.application.crawler.common.CrawlerResourceNotFoundException
import going9.laptopgg.application.crawler.common.port.CrawlerTransactionPort
import going9.laptopgg.application.crawler.run.port.CrawlerRunPort

internal class TrackCrawlerRunService(
    private val crawlerRunPort: CrawlerRunPort,
    private val transactionPort: CrawlerTransactionPort,
    private val commandFactory: CrawlerRunCommandFactory = CrawlerRunCommandFactory(),
) : TrackCrawlerRunUseCase {
    override fun start(filterProfile: String, startPage: Int, limit: Int?): CrawlerRunRecord {
        return transactionPort.write {
            crawlerRunPort.create(commandFactory.start(filterProfile, startPage, limit)).toRecord()
        }
    }

    override fun skipLocked(filterProfile: String, startPage: Int, limit: Int?): CrawlerRunRecord {
        return transactionPort.write {
            crawlerRunPort.create(commandFactory.skipLocked(filterProfile, startPage, limit)).toRecord()
        }
    }

    override fun finish(
        runId: Long,
        summary: CrawlerRunSummary,
        status: CrawlerRunCompletionStatus,
        errorMessage: String?,
    ): CrawlerRunRecord {
        return transactionPort.write {
            (
                crawlerRunPort.update(commandFactory.finish(runId, summary, status, errorMessage))
                    ?: throw CrawlerResourceNotFoundException("CrawlerRun", runId)
            ).toRecord()
        }
    }

    override fun fail(runId: Long, exception: Exception): CrawlerRunRecord {
        return transactionPort.write {
            (
                crawlerRunPort.update(commandFactory.fail(runId, exception))
                    ?: throw CrawlerResourceNotFoundException("CrawlerRun", runId)
            ).toRecord()
        }
    }

    private fun CrawlerRunState.toRecord(): CrawlerRunRecord {
        return CrawlerRunRecord(
            id = id,
            status = status,
        )
    }
}
