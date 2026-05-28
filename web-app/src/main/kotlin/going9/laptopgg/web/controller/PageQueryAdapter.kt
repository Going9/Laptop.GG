package going9.laptopgg.web.controller

import going9.laptopgg.application.common.PageQuery
import going9.laptopgg.application.common.SortDirection
import going9.laptopgg.application.common.SortOrder
import going9.laptopgg.application.common.SortProperty

internal fun pageQueryFrom(
    page: Int?,
    size: Int?,
    sort: List<String>?,
): PageQuery {
    return PageQuery(
        page = page?.coerceAtLeast(0) ?: 0,
        size = (size ?: PageQuery.DEFAULT_SIZE).coerceIn(1, PageQuery.MAX_SIZE),
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
        property = parts.firstOrNull()
            ?.let(SortProperty::fromExternalName)
            ?: SortProperty.RECOMMENDED,
        direction = when (parts.getOrNull(1)?.lowercase()) {
            "desc", "descending" -> SortDirection.DESC
            else -> SortDirection.ASC
        },
    )
}
