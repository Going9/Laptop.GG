package going9.laptopgg.application.crawler

import going9.laptopgg.application.service.CpuClassifier
import org.springframework.stereotype.Component

@Component
class CrawledCpuModelResolver(
    private val cpuClassifier: CpuClassifier = CpuClassifier(),
) {
    fun resolve(rawCpu: String?, cpuManufacturer: String?, productName: String): String? {
        return cpuClassifier.resolveCpuToken(rawCpu, cpuManufacturer, productName)
            ?: rawCpu?.trim()?.takeIf { it.isNotBlank() }
    }
}
