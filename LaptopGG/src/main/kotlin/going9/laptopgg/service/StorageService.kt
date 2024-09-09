package going9.laptopgg.service

import going9.laptopgg.domain.laptop.Laptop
import going9.laptopgg.domain.laptop.Storage
import going9.laptopgg.domain.repository.StorageRepository
import going9.laptopgg.dto.request.StorageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class StorageService(
    private val storageRepository: StorageRepository
) {

    @Transactional
    fun saveStorage(laptop: Laptop, request: StorageRequest) {
        val storage = Storage(
            laptop,
            request.capacity,
            request.slot
        )
        storageRepository.save(storage)
    }
}