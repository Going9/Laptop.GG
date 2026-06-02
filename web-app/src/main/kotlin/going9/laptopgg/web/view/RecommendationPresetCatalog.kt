package going9.laptopgg.web.view

import org.springframework.stereotype.Component

@Component
internal class RecommendationPresetCatalog {
    fun budgetPresets(): List<Int> {
        return (500_000..5_000_000 step 500_000).toList()
    }

    fun weightPresets(): List<Double> {
        return (1..8).map { it * 0.5 }
    }
}
