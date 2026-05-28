package going9.laptopgg.controller

import going9.laptopgg.application.common.PageQuery
import going9.laptopgg.application.common.SortDirection
import going9.laptopgg.application.common.SortOrder
import org.springframework.data.domain.Pageable

fun Pageable.toPageQuery(): PageQuery {
    return PageQuery(
        page = pageNumber,
        size = pageSize,
        sort = sort.map { order ->
            SortOrder(
                property = order.property,
                direction = if (order.isAscending) SortDirection.ASC else SortDirection.DESC,
            )
        }.toList(),
    )
}
