package going9.laptopgg.web.dto.response

import going9.laptopgg.application.common.PagedResult
import going9.laptopgg.application.common.SortDirection
import going9.laptopgg.application.common.SortOrder

data class PagedResponse<T>(
    val content: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val hasNext: Boolean,
    val hasPrevious: Boolean,
    val sort: List<SortOrderResponse>,
) {
    companion object {
        fun <T> from(result: PagedResult<T>): PagedResponse<T> {
            return PagedResponse(
                content = result.content,
                page = result.page,
                size = result.size,
                totalElements = result.totalElements,
                totalPages = result.totalPages,
                hasNext = result.hasNext,
                hasPrevious = result.hasPrevious,
                sort = result.sort.map(SortOrderResponse::from),
            )
        }
    }
}

data class SortOrderResponse(
    val property: String,
    val direction: SortDirection,
) {
    companion object {
        fun from(order: SortOrder): SortOrderResponse {
            return SortOrderResponse(
                property = order.property,
                direction = order.direction,
            )
        }
    }
}
