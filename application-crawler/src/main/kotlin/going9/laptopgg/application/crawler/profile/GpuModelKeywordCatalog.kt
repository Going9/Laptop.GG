package going9.laptopgg.application.crawler.profile

internal object GpuModelKeywordCatalog {
    fun isDiscreteModel(normalizedModel: String): Boolean {
        return DISCRETE_GPU_KEYWORDS.any { normalizedModel.contains(it) }
    }

    fun isIntegratedModel(normalizedModel: String): Boolean {
        return INTEGRATED_GPU_KEYWORDS.any { normalizedModel.contains(it) }
    }

    private val DISCRETE_GPU_KEYWORDS = listOf(
        "RTX",
        "GTX",
        "GEFORCE",
        "RTX PRO",
        "RTX A",
        "ARC B",
        "ARC A",
        "RADEON RX",
    )

    private val INTEGRATED_GPU_KEYWORDS = listOf(
        "INTEL GRAPHICS",
        "IRIS",
        "UHD",
        "HD GRAPHICS",
        "ARC 130T",
        "ARC 140T",
        "RADEON 890M",
        "RADEON 880M",
        "RADEON 860M",
        "RADEON 840M",
        "RADEON 820M",
        "RADEON 8060S",
        "RADEON 780M",
        "RADEON 760M",
        "RADEON 740M",
        "RADEON 680M",
        "RADEON 660M",
        "RADEON 610M",
        "RADEON GRAPHICS",
        "ADRENO",
    )
}
