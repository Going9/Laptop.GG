package going9.laptopgg.application.crawler

class CrawledGraphicsModelResolver(
    private val gpuClassifier: GpuClassifier = GpuClassifier(),
) {
    fun isIntegrated(graphicsKind: String?, graphicsModel: String?): Boolean {
        return gpuClassifier.isIntegratedGraphics(graphicsKind, graphicsModel)
    }
}
