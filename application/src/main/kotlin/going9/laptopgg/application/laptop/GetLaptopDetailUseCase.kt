package going9.laptopgg.application.laptop

import going9.laptopgg.application.common.InvalidCommandException
import going9.laptopgg.application.common.LaptopDisplayTextPolicy
import going9.laptopgg.application.common.ResourceNotFoundException
import going9.laptopgg.application.common.port.ApplicationTransactionPort
import going9.laptopgg.application.laptop.port.LaptopDetailRecord
import going9.laptopgg.application.laptop.port.LaptopPort

interface GetLaptopDetailUseCase {
    fun get(laptopId: Long): LaptopDetailResult
}

internal class DefaultGetLaptopDetailUseCase(
    private val laptopPort: LaptopPort,
    private val transactionPort: ApplicationTransactionPort,
) : GetLaptopDetailUseCase {
    override fun get(laptopId: Long): LaptopDetailResult {
        validateLaptopId(laptopId)
        return transactionPort.read {
            val laptop = laptopPort.findDetailById(laptopId) ?: throw ResourceNotFoundException("Laptop", laptopId)
            laptop.toResult()
        }
    }

    private fun validateLaptopId(laptopId: Long) {
        if (laptopId <= 0) {
            throw InvalidCommandException("laptopId must be positive.")
        }
    }

    private fun LaptopDetailRecord.toResult(): LaptopDetailResult {
        return LaptopDetailResult(
            id = id,
            name = name,
            imageUrl = imageUrl,
            manufacturer = LaptopDisplayTextPolicy.manufacturerName(name),
            detailPage = detailPage,
            price = price,
            cpuManufacturer = cpuManufacturer,
            cpu = cpu,
            os = LaptopDisplayTextPolicy.humanizeOs(os),
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
            usage = usage,
        )
    }
}
