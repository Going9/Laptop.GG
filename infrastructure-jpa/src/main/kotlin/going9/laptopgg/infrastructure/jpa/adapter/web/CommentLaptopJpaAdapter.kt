package going9.laptopgg.infrastructure.jpa.adapter.web

import going9.laptopgg.application.comment.port.CommentLaptopPort
import going9.laptopgg.infrastructure.jpa.repository.web.WebLaptopRepository
import org.springframework.stereotype.Component

@Component
internal class CommentLaptopJpaAdapter(
    private val laptopRepository: WebLaptopRepository,
) : CommentLaptopPort {
    override fun existsById(laptopId: Long): Boolean {
        return laptopRepository.existsById(laptopId)
    }
}
