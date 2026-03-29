package going9.laptopgg.service

import going9.laptopgg.domain.laptop.Laptop
import going9.laptopgg.domain.laptop.LaptopProfile
import going9.laptopgg.domain.repository.LaptopProfileRepository
import going9.laptopgg.domain.repository.LaptopRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class LaptopProfileService(
    private val laptopRepository: LaptopRepository,
    private val laptopProfileRepository: LaptopProfileRepository,
    private val laptopProfileFactory: LaptopProfileFactory,
) {
    @Transactional
    fun syncMissingProfiles() {
        val existingLaptopIds = laptopProfileRepository.findAllLaptopIds().toSet()
        val missingLaptops = laptopRepository.findAll()
            .filter { laptop -> laptop.id != null && laptop.id !in existingLaptopIds }

        missingLaptops.forEach { laptop ->
            syncProfile(laptop)
        }
    }

    @Transactional
    fun syncProfile(laptop: Laptop): LaptopProfile {
        val laptopId = requireNotNull(laptop.id) { "Laptop must be persisted before syncing a profile." }
        val snapshot = laptopProfileFactory.build(laptop)
        val existingProfile = laptopProfileRepository.findByLaptopId(laptopId)

        return if (existingProfile == null) {
            laptopProfileRepository.save(
                LaptopProfile(
                    laptop = laptop,
                    cpuClass = snapshot.cpuClass,
                    gpuClass = snapshot.gpuClass,
                    batteryTier = snapshot.batteryTier,
                    portabilityTier = snapshot.portabilityTier,
                    officeScore = snapshot.officeScore,
                    batteryScore = snapshot.batteryScore,
                    casualGameScore = snapshot.casualGameScore,
                    onlineGameScore = snapshot.onlineGameScore,
                    aaaGameScore = snapshot.aaaGameScore,
                    creatorScore = snapshot.creatorScore,
                ),
            )
        } else {
            existingProfile.cpuClass = snapshot.cpuClass
            existingProfile.gpuClass = snapshot.gpuClass
            existingProfile.batteryTier = snapshot.batteryTier
            existingProfile.portabilityTier = snapshot.portabilityTier
            existingProfile.officeScore = snapshot.officeScore
            existingProfile.batteryScore = snapshot.batteryScore
            existingProfile.casualGameScore = snapshot.casualGameScore
            existingProfile.onlineGameScore = snapshot.onlineGameScore
            existingProfile.aaaGameScore = snapshot.aaaGameScore
            existingProfile.creatorScore = snapshot.creatorScore
            laptopProfileRepository.save(existingProfile)
        }
    }
}
