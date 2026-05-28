package going9.laptopgg.application.common

import kotlin.math.ceil

data class PagedResult<T>(
    val content: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val sort: List<SortOrder> = emptyList(),
) {
    val totalPages: Int
        get() = if (size <= 0) 0 else ceil(totalElements.toDouble() / size.toDouble()).toInt()

    val hasNext: Boolean
        get() = page + 1 < totalPages

    val hasPrevious: Boolean
        get() = page > 0 && totalPages > 0
}
