package going9.laptopgg.application.common

data class PageQuery(
    val page: Int,
    val size: Int,
    val sort: List<SortOrder> = emptyList(),
) {
    val offset: Int
        get() = (page * size).coerceAtLeast(0)
}

data class SortOrder(
    val property: String,
    val direction: SortDirection,
) {
    val isAscending: Boolean
        get() = direction == SortDirection.ASC
}

enum class SortDirection {
    ASC,
    DESC,
}
