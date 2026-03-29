package going9.laptopgg.util

import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.findByIdOrNull

fun <T, ID> CrudRepository<T, ID>.findByOrThrow(id: ID): T {
    return this.findByIdOrNull(id) ?: throw IllegalArgumentException()
}