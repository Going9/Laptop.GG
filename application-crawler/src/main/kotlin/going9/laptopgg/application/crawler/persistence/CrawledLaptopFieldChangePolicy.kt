package going9.laptopgg.application.crawler.persistence

internal class CrawledLaptopFieldChangePolicy {
    fun changedText(currentValue: String?, newValue: String?): String? {
        val normalizedValue = normalizeOptionalText(newValue) ?: return null
        if (currentValue?.trim() == normalizedValue) {
            return null
        }
        return normalizedValue
    }

    fun normalizeRequiredText(value: String): String {
        return value.trim()
    }

    fun normalizeOptionalText(value: String?): String? {
        return value?.trim()?.takeIf { it.isNotBlank() }
    }

    fun <T : Any> changedPresent(currentValue: T?, newValue: T?): T? {
        val normalizedValue = newValue ?: return null
        if (currentValue == normalizedValue) {
            return null
        }
        return normalizedValue
    }

    fun changedUsages(currentUsages: List<String>, newUsages: List<String>): List<String>? {
        val normalizedUsages = normalizeUsages(newUsages)
        if (normalizedUsages.isEmpty() || currentUsages.sorted() == normalizedUsages.sorted()) {
            return null
        }

        return normalizedUsages
    }

    fun normalizeUsages(usages: List<String>): List<String> {
        return usages
            .map { usage -> usage.trim() }
            .filter { usage -> usage.isNotBlank() }
            .distinct()
    }

    fun hasChanges(updateCommand: UpdateCrawledLaptopCommand): Boolean {
        return listOf(
            updateCommand.name,
            updateCommand.imageUrl,
            updateCommand.detailPage,
            updateCommand.productCode,
            updateCommand.price,
            updateCommand.cpuManufacturer,
            updateCommand.cpu,
            updateCommand.os,
            updateCommand.screenSize,
            updateCommand.resolution,
            updateCommand.brightness,
            updateCommand.refreshRate,
            updateCommand.ramSize,
            updateCommand.ramType,
            updateCommand.isRamReplaceable,
            updateCommand.graphicsType,
            updateCommand.tgp,
            updateCommand.thunderboltCount,
            updateCommand.usbCCount,
            updateCommand.usbACount,
            updateCommand.sdCard,
            updateCommand.isSupportsPdCharging,
            updateCommand.batteryCapacity,
            updateCommand.storageCapacity,
            updateCommand.storageSlotCount,
            updateCommand.weight,
            updateCommand.lastDetailedCrawledAt,
            updateCommand.usages,
        ).any { value -> value != null }
    }
}
