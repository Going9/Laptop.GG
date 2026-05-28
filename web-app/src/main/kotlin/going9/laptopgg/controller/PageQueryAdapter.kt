package going9.laptopgg.controller

import going9.laptopgg.application.common.PageQuery
import going9.laptopgg.application.common.SortDirection
import going9.laptopgg.application.common.SortOrder

private const val DEFAULT_PAGE_SIZE = 10
private const val MAX_PAGE_SIZE = 2_000

fun pageQueryFrom(
    page: Int?,
    size: Int?,
    sort: List<String>?,
): PageQuery {
    return PageQuery(
        page = page?.coerceAtLeast(0) ?: 0,
        size = (size ?: DEFAULT_PAGE_SIZE).coerceIn(1, MAX_PAGE_SIZE),
        sort = sort.orEmpty()
            .filter { it.isNotBlank() }
            .map(::parseSortOrder),
    )
}

private fun parseSortOrder(rawSort: String): SortOrder {
    val parts = rawSort.split(",")
        .map { it.trim() }
        .filter { it.isNotBlank() }

    return SortOrder(
        property = parts.firstOrNull() ?: "recommended",
        direction = when (parts.getOrNull(1)?.lowercase()) {
            "desc", "descending" -> SortDirection.DESC
            else -> SortDirection.ASC
        },
    )
}
