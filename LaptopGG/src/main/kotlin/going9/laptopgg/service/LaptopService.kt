package going9.laptopgg.service

import going9.laptopgg.domain.laptop.Laptop
import going9.laptopgg.domain.repository.LaptopRepository
import going9.laptopgg.dto.request.LaptopRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class LaptopService(
    private val laptopRepository: LaptopRepository,
) {

    @Transactional
    fun saveLaptop(request: LaptopRequest) {
        val laptop = Laptop()
    }


}