package going9.laptopgg.infrastructure.jpa.repository.crawler

import going9.laptopgg.persistence.model.laptop.Laptop
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface CrawlerLaptopRepository : JpaRepository<Laptop, Long> {
    @Query(
        """
        select l.id as id,
               l.name as name,
               l.imageUrl as imageUrl,
               l.detailPage as detailPage,
               l.productCode as productCode,
               l.price as price
        from Laptop l
        where l.id = :id
        """,
    )
    fun findListSnapshotById(@Param("id") id: Long): CrawledListSnapshotProjection?

    @Query(
        """
        select distinct l
        from Laptop l
        left join fetch l.laptopUsage
        where l.productCode in :productCodes
        """,
    )
    fun findAllWithUsageByProductCodeIn(@Param("productCodes") productCodes: Collection<String>): List<Laptop>

    @Query(
        """
        select distinct l
        from Laptop l
        left join fetch l.laptopUsage
        where l.detailPage in :detailPages
        """,
    )
    fun findAllWithUsageByDetailPageIn(@Param("detailPages") detailPages: Collection<String>): List<Laptop>

    @Query(
        """
        select distinct l
        from Laptop l
        left join fetch l.laptopUsage
        where l.id = :id
        """,
    )
    fun findWithUsageById(@Param("id") id: Long): Laptop?

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        update Laptop l
        set l.name = coalesce(:name, l.name),
            l.imageUrl = coalesce(:imageUrl, l.imageUrl),
            l.detailPage = coalesce(:detailPage, l.detailPage),
            l.productCode = coalesce(:productCode, l.productCode),
            l.price = coalesce(:price, l.price)
        where l.id = :id
        """,
    )
    fun updateListSnapshotById(
        @Param("id") id: Long,
        @Param("name") name: String?,
        @Param("imageUrl") imageUrl: String?,
        @Param("detailPage") detailPage: String?,
        @Param("productCode") productCode: String?,
        @Param("price") price: Int?,
    ): Int

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        update Laptop l
        set l.name = coalesce(:name, l.name),
            l.imageUrl = coalesce(:imageUrl, l.imageUrl),
            l.detailPage = coalesce(:detailPage, l.detailPage),
            l.productCode = coalesce(:productCode, l.productCode),
            l.price = coalesce(:price, l.price),
            l.cpuManufacturer = coalesce(:cpuManufacturer, l.cpuManufacturer),
            l.cpu = coalesce(:cpu, l.cpu),
            l.os = coalesce(:os, l.os),
            l.screenSize = coalesce(:screenSize, l.screenSize),
            l.resolution = coalesce(:resolution, l.resolution),
            l.brightness = coalesce(:brightness, l.brightness),
            l.refreshRate = coalesce(:refreshRate, l.refreshRate),
            l.ramSize = coalesce(:ramSize, l.ramSize),
            l.ramType = coalesce(:ramType, l.ramType),
            l.isRamReplaceable = coalesce(:isRamReplaceable, l.isRamReplaceable),
            l.graphicsType = coalesce(:graphicsType, l.graphicsType),
            l.tgp = coalesce(:tgp, l.tgp),
            l.thunderboltCount = coalesce(:thunderboltCount, l.thunderboltCount),
            l.usbCCount = coalesce(:usbCCount, l.usbCCount),
            l.usbACount = coalesce(:usbACount, l.usbACount),
            l.sdCard = coalesce(:sdCard, l.sdCard),
            l.isSupportsPdCharging = coalesce(:isSupportsPdCharging, l.isSupportsPdCharging),
            l.batteryCapacity = coalesce(:batteryCapacity, l.batteryCapacity),
            l.storageCapacity = coalesce(:storageCapacity, l.storageCapacity),
            l.storageSlotCount = coalesce(:storageSlotCount, l.storageSlotCount),
            l.weight = coalesce(:weight, l.weight),
            l.lastDetailedCrawledAt = coalesce(:lastDetailedCrawledAt, l.lastDetailedCrawledAt)
        where l.id = :id
        """,
    )
    fun updateDetailSnapshotById(
        @Param("id") id: Long,
        @Param("name") name: String?,
        @Param("imageUrl") imageUrl: String?,
        @Param("detailPage") detailPage: String?,
        @Param("productCode") productCode: String?,
        @Param("price") price: Int?,
        @Param("cpuManufacturer") cpuManufacturer: String?,
        @Param("cpu") cpu: String?,
        @Param("os") os: String?,
        @Param("screenSize") screenSize: Int?,
        @Param("resolution") resolution: String?,
        @Param("brightness") brightness: Int?,
        @Param("refreshRate") refreshRate: Int?,
        @Param("ramSize") ramSize: Int?,
        @Param("ramType") ramType: String?,
        @Param("isRamReplaceable") isRamReplaceable: Boolean?,
        @Param("graphicsType") graphicsType: String?,
        @Param("tgp") tgp: Int?,
        @Param("thunderboltCount") thunderboltCount: Int?,
        @Param("usbCCount") usbCCount: Int?,
        @Param("usbACount") usbACount: Int?,
        @Param("sdCard") sdCard: String?,
        @Param("isSupportsPdCharging") isSupportsPdCharging: Boolean?,
        @Param("batteryCapacity") batteryCapacity: Double?,
        @Param("storageCapacity") storageCapacity: Int?,
        @Param("storageSlotCount") storageSlotCount: Int?,
        @Param("weight") weight: Double?,
        @Param("lastDetailedCrawledAt") lastDetailedCrawledAt: LocalDateTime?,
    ): Int
}

interface CrawledListSnapshotProjection {
    val id: Long?
    val name: String
    val imageUrl: String
    val detailPage: String
    val productCode: String?
    val price: Int?
}
