package going9.laptopgg.application.laptop

import going9.laptopgg.application.common.LaptopDisplayTextPolicy
import going9.laptopgg.application.laptop.port.LaptopDetailRecord

internal fun LaptopDetailRecord.toLaptopDetailResult(): LaptopDetailResult {
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
