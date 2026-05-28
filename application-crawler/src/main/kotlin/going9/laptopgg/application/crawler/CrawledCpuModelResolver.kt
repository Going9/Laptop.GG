package going9.laptopgg.application.crawler

class CrawledCpuModelResolver(
    private val cpuClassifier: CpuClassifier = CpuClassifier(),
) {
    fun resolve(rawCpu: String?, cpuManufacturer: String?, productName: String): String? {
        return cpuClassifier.resolveCpuToken(rawCpu, cpuManufacturer, productName)
            ?: rawCpu?.trim()?.takeIf { it.isNotBlank() }
    }
}
