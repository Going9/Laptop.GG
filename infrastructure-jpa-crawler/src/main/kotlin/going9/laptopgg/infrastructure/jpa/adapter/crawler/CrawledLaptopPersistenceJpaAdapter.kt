package going9.laptopgg.infrastructure.jpa.adapter.crawler

import going9.laptopgg.application.crawler.common.CrawlerInvalidStateException
import going9.laptopgg.application.crawler.persistence.CrawledLaptopCommand
import going9.laptopgg.application.crawler.persistence.ExistingCrawledLaptopSnapshot
import going9.laptopgg.application.crawler.persistence.PersistedCrawledListSnapshot
import going9.laptopgg.application.crawler.persistence.PersistedCrawledLaptopSnapshot
import going9.laptopgg.application.crawler.persistence.UpdateCrawledListSnapshotCommand
import going9.laptopgg.application.crawler.persistence.UpdateCrawledLaptopCommand
import going9.laptopgg.application.crawler.persistence.port.CrawledLaptopPersistencePort
import going9.laptopgg.infrastructure.jpa.repository.crawler.CrawlerLaptopRepository
import going9.laptopgg.infrastructure.jpa.repository.crawler.CrawlerLaptopUsageRepository
import going9.laptopgg.persistence.model.laptop.Laptop
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Component

@Component
internal class CrawledLaptopPersistenceJpaAdapter(
    private val laptopRepository: CrawlerLaptopRepository,
    private val laptopUsageRepository: CrawlerLaptopUsageRepository,
    private val entityManager: EntityManager,
) : CrawledLaptopPersistencePort {
    override fun findListSnapshotById(laptopId: Long): PersistedCrawledListSnapshot? {
        return laptopRepository.findListSnapshotById(laptopId)?.toPersistedCrawledListSnapshot()
    }

    override fun findWithUsageById(laptopId: Long): PersistedCrawledLaptopSnapshot? {
        return laptopRepository.findWithUsageById(laptopId)?.toPersistedCrawledLaptopSnapshot()
    }

    override fun findByProductCode(productCode: String): PersistedCrawledLaptopSnapshot? {
        return laptopRepository.findAllWithUsageByProductCodeIn(listOf(productCode))
            .singleByCrawlerIdentity(identityName = "productCode", identityValue = productCode)
            ?.toPersistedCrawledLaptopSnapshot()
    }

    override fun findByDetailPage(detailPage: String): PersistedCrawledLaptopSnapshot? {
        return laptopRepository.findAllWithUsageByDetailPageIn(listOf(detailPage))
            .singleByCrawlerIdentity(identityName = "detailPage", identityValue = detailPage)
            ?.toPersistedCrawledLaptopSnapshot()
    }

    override fun findExistingByProductCodes(productCodes: Collection<String>): List<ExistingCrawledLaptopSnapshot> {
        if (productCodes.isEmpty()) {
            return emptyList()
        }
        return laptopRepository.findExistingByProductCodeIn(productCodes)
            .map { projection -> projection.toExistingCrawledLaptopSnapshot() }
    }

    override fun findExistingByDetailPages(detailPages: Collection<String>): List<ExistingCrawledLaptopSnapshot> {
        if (detailPages.isEmpty()) {
            return emptyList()
        }
        return laptopRepository.findExistingByDetailPageIn(detailPages)
            .map { projection -> projection.toExistingCrawledLaptopSnapshot() }
    }

    override fun create(command: CrawledLaptopCommand): PersistedCrawledLaptopSnapshot {
        return laptopRepository.save(CrawledLaptopEntityMapper.newLaptop(command)).toPersistedCrawledLaptopSnapshot()
    }

    override fun updateListSnapshot(laptopId: Long, command: UpdateCrawledListSnapshotCommand): Boolean {
        return laptopRepository.updateListSnapshotById(
            id = laptopId,
            name = command.name,
            imageUrl = command.imageUrl,
            detailPage = command.detailPage,
            productCode = command.productCode,
            price = command.price,
        ) > 0
    }

    override fun updateDetailSnapshot(laptopId: Long, command: UpdateCrawledLaptopCommand): Boolean {
        val updatedRows = laptopRepository.updateDetailSnapshotById(
            id = laptopId,
            name = command.name,
            imageUrl = command.imageUrl,
            detailPage = command.detailPage,
            productCode = command.productCode,
            price = command.price,
            cpuManufacturer = command.cpuManufacturer,
            cpu = command.cpu,
            os = command.os,
            screenSize = command.screenSize,
            resolution = command.resolution,
            brightness = command.brightness,
            refreshRate = command.refreshRate,
            ramSize = command.ramSize,
            ramType = command.ramType,
            isRamReplaceable = command.isRamReplaceable,
            graphicsType = command.graphicsType,
            tgp = command.tgp,
            thunderboltCount = command.thunderboltCount,
            usbCCount = command.usbCCount,
            usbACount = command.usbACount,
            sdCard = command.sdCard,
            isSupportsPdCharging = command.isSupportsPdCharging,
            batteryCapacity = command.batteryCapacity,
            storageCapacity = command.storageCapacity,
            storageSlotCount = command.storageSlotCount,
            weight = command.weight,
            lastDetailedCrawledAt = command.lastDetailedCrawledAt,
        )
        if (updatedRows == 0) {
            return false
        }
        command.usages?.let { usages -> replaceUsages(laptopId, usages) }
        return true
    }

    private fun replaceUsages(laptopId: Long, usages: List<String>) {
        laptopUsageRepository.deleteByLaptopId(laptopId)
        val laptop = entityManager.getReference(Laptop::class.java, laptopId)
        laptopUsageRepository.saveAll(CrawledLaptopEntityMapper.newLaptopUsages(laptop, usages))
    }

    private fun List<Laptop>.singleByCrawlerIdentity(identityName: String, identityValue: String): Laptop? {
        val uniqueLaptops = distinctBy { laptop -> laptop.id }
        if (uniqueLaptops.size > 1) {
            val ids = uniqueLaptops
                .map { laptop -> laptop.id?.toString() ?: "<unpersisted>" }
                .sorted()
                .joinToString(prefix = "[", postfix = "]")
            throw CrawlerInvalidStateException(
                "Multiple laptops found for $identityName=$identityValue; clean duplicate crawler identities before crawling: $ids",
            )
        }
        return uniqueLaptops.singleOrNull()
    }
}
