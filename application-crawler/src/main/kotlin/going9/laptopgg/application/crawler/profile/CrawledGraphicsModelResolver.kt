package going9.laptopgg.application.crawler.profile

class CrawledGraphicsModelResolver {
    private val gpuClassifier = GpuClassifier()

    fun isIntegrated(graphicsKind: String?, graphicsModel: String?): Boolean {
        return gpuClassifier.isIntegratedGraphics(graphicsKind, graphicsModel)
    }
}
