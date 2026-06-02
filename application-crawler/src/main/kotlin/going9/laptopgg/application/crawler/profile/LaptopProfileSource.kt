package going9.laptopgg.application.crawler.profile

internal data class LaptopProfileSource(
    val name: String,
    val cpuManufacturer: String?,
    val cpu: String?,
    val resolution: String?,
    val brightness: Int?,
    val refreshRate: Int?,
    val ramSize: Int?,
    val graphicsType: String?,
    val tgp: Int?,
    val batteryCapacity: Double?,
    val weight: Double?,
    val usages: List<String>,
)
