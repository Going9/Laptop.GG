package going9.laptopgg.job.crawler.danawa.detail

internal data class DetailRequestContext(
    val makerName: String?,
    val productName: String?,
    val prodType: String?,
)

internal data class ParsedSpecTable(
    val values: Map<String, String>,
    val usages: List<String>,
)

internal data class SummaryFallback(
    val cpuManufacturer: String? = null,
    val cpu: String? = null,
    val os: String? = null,
    val screenSize: Int? = null,
    val resolution: String? = null,
    val brightness: Int? = null,
    val refreshRate: Int? = null,
    val ramSize: Int? = null,
    val ramType: String? = null,
    val isRamReplaceable: Boolean? = null,
    val graphicsKind: String? = null,
    val graphicsModel: String? = null,
    val tgp: Int? = null,
    val isSupportsPdCharging: Boolean? = null,
    val batteryCapacity: Double? = null,
    val storageCapacity: Int? = null,
    val storageSlotCount: Int? = null,
    val weight: Double? = null,
    val usages: List<String> = emptyList(),
) {
    fun isEmpty(): Boolean {
        return cpuManufacturer == null &&
            cpu == null &&
            os == null &&
            screenSize == null &&
            resolution == null &&
            brightness == null &&
            refreshRate == null &&
            ramSize == null &&
            ramType == null &&
            isRamReplaceable == null &&
            graphicsKind == null &&
            graphicsModel == null &&
            tgp == null &&
            isSupportsPdCharging == null &&
            batteryCapacity == null &&
            storageCapacity == null &&
            storageSlotCount == null &&
            weight == null &&
            usages.isEmpty()
    }
}
