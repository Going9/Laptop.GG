package going9.laptopgg.infrastructure.jpa.adapter.web

import going9.laptopgg.application.port.out.LaptopDetailRecord
import going9.laptopgg.application.port.out.LaptopPort
import going9.laptopgg.domain.laptop.Laptop
import going9.laptopgg.infrastructure.jpa.repository.web.WebLaptopRepository
import org.springframework.stereotype.Component

@Component
class LaptopJpaAdapter(
    private val laptopRepository: WebLaptopRepository,
) : LaptopPort {
    override fun existsById(laptopId: Long): Boolean {
        return laptopRepository.existsById(laptopId)
    }

    override fun findDetailById(laptopId: Long): LaptopDetailRecord? {
        return laptopRepository.findWithUsageById(laptopId)?.toDetailRecord()
    }

    private fun Laptop.toDetailRecord(): LaptopDetailRecord {
        return LaptopDetailRecord(
            id = requireNotNull(id) { "Persisted laptop id must not be null." },
            name = name,
            imageUrl = imageUrl,
            detailPage = detailPage,
            price = price,
            cpuManufacturer = cpuManufacturer,
            cpu = cpu,
            os = os,
            screenSize = screenSize,
            resolution = resolution,
            brightness = brightness,
            refreshRate = refreshRate,
            ramSize = ramSize,
            ramType = ramType,
            isRamReplaceable = isRamReplaceable,
            graphicsType = graphicsType,
            tgp = tgp,
            thunderboltCount = thunderboltCount,
            usbCCount = usbCCount,
            usbACount = usbACount,
            sdCard = sdCard,
            isSupportsPdCharging = isSupportsPdCharging,
            batteryCapacity = batteryCapacity,
            storageCapacity = storageCapacity,
            storageSlotCount = storageSlotCount,
            weight = weight,
            usage = laptopUsage.map { it.usage },
        )
    }
}
