package going9.laptopgg.application.crawler

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Profile("crawler")
@Component
class CrawledCpuModelResolver(
    private val cpuClassifier: CpuClassifier = CpuClassifier(),
) {
    fun resolve(rawCpu: String?, cpuManufacturer: String?, productName: String): String? {
        return cpuClassifier.resolveCpuToken(rawCpu, cpuManufacturer, productName)
            ?: rawCpu?.trim()?.takeIf { it.isNotBlank() }
    }
}
