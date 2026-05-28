package going9.laptopgg.infrastructure.jpa.repository.crawler

import going9.laptopgg.persistence.model.laptop.Laptop
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

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
        select l.id as id,
               l.productCode as productCode,
               l.detailPage as detailPage,
               l.cpuManufacturer as cpuManufacturer,
               l.cpu as cpu,
               l.os as os,
               l.screenSize as screenSize,
               l.resolution as resolution,
               l.ramSize as ramSize,
               l.graphicsType as graphicsType,
               l.storageCapacity as storageCapacity,
               l.batteryCapacity as batteryCapacity,
               l.weight as weight,
               l.lastDetailedCrawledAt as lastDetailedCrawledAt,
               (select count(lu.id) from LaptopUsage lu where lu.laptop = l) as usageCount
        from Laptop l
        where l.productCode in :productCodes
        """,
    )
    fun findExistingByProductCodeIn(@Param("productCodes") productCodes: Collection<String>): List<ExistingCrawledLaptopProjection>

    @Query(
        """
        select l.id as id,
               l.productCode as productCode,
               l.detailPage as detailPage,
               l.cpuManufacturer as cpuManufacturer,
               l.cpu as cpu,
               l.os as os,
               l.screenSize as screenSize,
               l.resolution as resolution,
               l.ramSize as ramSize,
               l.graphicsType as graphicsType,
               l.storageCapacity as storageCapacity,
               l.batteryCapacity as batteryCapacity,
               l.weight as weight,
               l.lastDetailedCrawledAt as lastDetailedCrawledAt,
               (select count(lu.id) from LaptopUsage lu where lu.laptop = l) as usageCount
        from Laptop l
        where l.detailPage in :detailPages
        """,
    )
    fun findExistingByDetailPageIn(@Param("detailPages") detailPages: Collection<String>): List<ExistingCrawledLaptopProjection>

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
}

interface CrawledListSnapshotProjection {
    val id: Long?
    val name: String
    val imageUrl: String
    val detailPage: String
    val productCode: String?
    val price: Int?
}

interface ExistingCrawledLaptopProjection {
    val id: Long?
    val productCode: String?
    val detailPage: String
    val cpuManufacturer: String?
    val cpu: String?
    val os: String?
    val screenSize: Int?
    val resolution: String?
    val ramSize: Int?
    val graphicsType: String?
    val storageCapacity: Int?
    val batteryCapacity: Double?
    val weight: Double?
    val lastDetailedCrawledAt: java.time.LocalDateTime?
    val usageCount: Long?
}
