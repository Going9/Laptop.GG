package going9.laptopgg.application.crawler.profile

class CrawledCpuModelResolver(
    private val cpuTokenResolver: CpuTokenResolver = CpuTokenResolver(),
) {
    fun resolve(rawCpu: String?, cpuManufacturer: String?, productName: String): String? {
        return cpuTokenResolver.resolve(rawCpu, cpuManufacturer, productName)
            ?: rawCpu?.trim()?.takeIf { it.isNotBlank() }
    }
}
