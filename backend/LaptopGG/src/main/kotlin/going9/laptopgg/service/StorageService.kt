package going9.laptopgg.service

import going9.laptopgg.domain.laptop.Storage
import going9.laptopgg.domain.repository.StorageRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class StorageService(
    private val storageRepository: StorageRepository
) {

    @Transactional
    fun saveStorage(storage: Storage) {
        storageRepository.save(storage)
    }
}