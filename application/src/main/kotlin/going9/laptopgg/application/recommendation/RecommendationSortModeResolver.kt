package going9.laptopgg.application.recommendation

import going9.laptopgg.application.common.PageQuery
import going9.laptopgg.application.common.SortProperty
import going9.laptopgg.application.recommendation.port.RecommendationCandidateSortMode

class RecommendationSortModeResolver {
    fun resolve(pageQuery: PageQuery): RecommendationCandidateSortMode {
        val firstOrder = pageQuery.sort.firstOrNull() ?: return RecommendationCandidateSortMode.RECOMMENDED

        return when (firstOrder.property) {
            SortProperty.RECOMMENDED -> RecommendationCandidateSortMode.RECOMMENDED
            SortProperty.PRICE -> if (firstOrder.isAscending) {
                RecommendationCandidateSortMode.PRICE_ASC
            } else {
                RecommendationCandidateSortMode.PRICE_DESC
            }
            SortProperty.WEIGHT -> if (firstOrder.isAscending) {
                RecommendationCandidateSortMode.WEIGHT_ASC
            } else {
                RecommendationCandidateSortMode.WEIGHT_DESC
            }
        }
    }
}
