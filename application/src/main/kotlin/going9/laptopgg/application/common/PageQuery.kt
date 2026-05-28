package going9.laptopgg.application.common

data class PageQuery(
    val page: Int,
    val size: Int,
    val sort: List<SortOrder> = emptyList(),
) {
    companion object {
        const val DEFAULT_SIZE = 10
        const val MAX_SIZE = 100
    }
}

data class SortOrder(
    val property: SortProperty,
    val direction: SortDirection,
) {
    val isAscending: Boolean
        get() = direction == SortDirection.ASC

    fun toQueryParameter(): String {
        if (property == SortProperty.RECOMMENDED) {
            return property.externalName
        }
        return "${property.externalName},${direction.name.lowercase()}"
    }
}

enum class SortDirection {
    ASC,
    DESC,
}

enum class SortProperty(
    val externalName: String,
) {
    RECOMMENDED("recommended"),
    PRICE("price"),
    WEIGHT("weight"),
    ;

    companion object {
        fun fromExternalName(value: String?): SortProperty? {
            return entries.firstOrNull { it.externalName == value }
        }
    }
}
