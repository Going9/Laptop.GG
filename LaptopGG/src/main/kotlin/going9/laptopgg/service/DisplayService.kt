package going9.laptopgg.service

import going9.laptopgg.domain.laptop.Display
import going9.laptopgg.domain.laptop.Laptop
import going9.laptopgg.domain.repository.DisplayRepository
import going9.laptopgg.dto.request.DisplayRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class DisplayService(
    val displayRepository: DisplayRepository,
) {
    @Transactional
    fun saveDisplay(laptop: Laptop, request: DisplayRequest) {
        val display = Display(
            laptop,
            request.panel,
            request.resolutionWidth,
            request.resolutionHeight,
            request.brightness,
            request.colorAccuracy,
            request.refreshRate,
            request.glareType,
            request.screenSize,
            request.isTouch,
            request.aspectRatio
        )
        displayRepository.save(display)
    }
}