package going9.laptopgg.domain.repository

import going9.laptopgg.domain.laptop.Storage
import org.springframework.data.jpa.repository.JpaRepository

interface StorageRepository : JpaRepository<Storage, Long> {
}