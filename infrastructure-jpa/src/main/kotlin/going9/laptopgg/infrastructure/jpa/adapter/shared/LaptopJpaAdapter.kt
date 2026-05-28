package going9.laptopgg.infrastructure.jpa.adapter.shared

import going9.laptopgg.application.port.out.LaptopPort
import going9.laptopgg.domain.laptop.Laptop
import going9.laptopgg.infrastructure.jpa.repository.shared.LaptopRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component

@Component
class LaptopJpaAdapter(
    private val laptopRepository: LaptopRepository,
) : LaptopPort {
    override fun findById(laptopId: Long): Laptop? {
        return laptopRepository.findByIdOrNull(laptopId)
    }

    override fun findWithUsageById(laptopId: Long): Laptop? {
        return laptopRepository.findWithUsageById(laptopId)
    }
}
