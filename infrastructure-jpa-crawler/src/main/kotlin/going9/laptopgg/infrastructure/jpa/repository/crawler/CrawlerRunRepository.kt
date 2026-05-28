package going9.laptopgg.infrastructure.jpa.repository.crawler

import going9.laptopgg.persistence.model.crawler.CrawlerRun
import going9.laptopgg.persistence.model.crawler.CrawlerRunStatus
import java.time.LocalDateTime
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface CrawlerRunRepository : JpaRepository<CrawlerRun, Long> {
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        update CrawlerRun c
        set c.status = :status,
            c.processedCount = :processedCount,
            c.createdCount = :createdCount,
            c.updatedCount = :updatedCount,
            c.detailRefreshCount = :detailRefreshCount,
            c.priceOnlyUpdatedCount = :priceOnlyUpdatedCount,
            c.degradedCount = :degradedCount,
            c.failedCount = :failedCount,
            c.failureSamples = :failureSamples,
            c.errorMessage = :errorMessage,
            c.endedAt = :endedAt
        where c.id = :runId
        """,
    )
    fun updateCompletionById(
        @Param("runId") runId: Long,
        @Param("status") status: CrawlerRunStatus,
        @Param("processedCount") processedCount: Int,
        @Param("createdCount") createdCount: Int,
        @Param("updatedCount") updatedCount: Int,
        @Param("detailRefreshCount") detailRefreshCount: Int,
        @Param("priceOnlyUpdatedCount") priceOnlyUpdatedCount: Int,
        @Param("degradedCount") degradedCount: Int,
        @Param("failedCount") failedCount: Int,
        @Param("failureSamples") failureSamples: String?,
        @Param("errorMessage") errorMessage: String?,
        @Param("endedAt") endedAt: LocalDateTime,
    ): Int

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        update CrawlerRun c
        set c.status = :status,
            c.failureSamples = null,
            c.errorMessage = :errorMessage,
            c.endedAt = :endedAt
        where c.id = :runId
        """,
    )
    fun updateFailureById(
        @Param("runId") runId: Long,
        @Param("status") status: CrawlerRunStatus,
        @Param("errorMessage") errorMessage: String?,
        @Param("endedAt") endedAt: LocalDateTime,
    ): Int
}
