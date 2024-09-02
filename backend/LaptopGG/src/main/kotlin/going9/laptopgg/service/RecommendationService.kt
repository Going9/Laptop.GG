package going9.laptopgg.service

import going9.laptopgg.domain.laptop.Display
import going9.laptopgg.domain.laptop.Laptop
import going9.laptopgg.domain.laptop.LaptopCategory
import going9.laptopgg.domain.laptop.Storage
import going9.laptopgg.domain.repository.LaptopRepository
import going9.laptopgg.dto.request.LaptopRecommendationRequest
import going9.laptopgg.dto.request.PurposeDetail
import going9.laptopgg.dto.request.TenKeyPreference
import going9.laptopgg.dto.response.CpuResponse
import going9.laptopgg.dto.response.DisplayResponse
import going9.laptopgg.dto.response.GpuResponse
import going9.laptopgg.dto.response.LaptopRecommendationResponse
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class RecommendationService(
    val laptopRepository: LaptopRepository,
) {

    @Transactional
    fun recommendLaptop(request: LaptopRecommendationRequest): List<LaptopRecommendationResponse> {
        request.toString()
        val recommendedLaptops = laptopRepository.findAll {
            select(
                entity(Laptop::class)
            ).from(
                entity(Laptop::class),
                join(Laptop::displays)
            ).whereAnd(
                path(Laptop::price).le(request.budget),
                path(Laptop::weight).le(request.weight),
                path(Display::screenSize).`in`(request.displaySize),
                if (request.isTenKey != TenKeyPreference.DOSE_NOT_MATTER) {
                    path(Laptop::isTenKey).eq(request.isTenKey == TenKeyPreference.NEEDED)
                } else null,

                // 목적에 따른 조건 추가
                when(request.purpose) {
                    PurposeDetail.OFFICE -> path(Laptop::category).eq(LaptopCategory.OFFICE)
                    PurposeDetail.CREATOR -> path(Laptop::category).eq(LaptopCategory.CREATOR)
                    PurposeDetail.LIGHT_OFFICE -> and(
                        path(Laptop::category).eq(LaptopCategory.OFFICE),
                        path(Laptop::weight).le(1.5),
                    )
                    PurposeDetail.LIGHT_GAMING -> path(Laptop::category).eq(LaptopCategory.LIGHT_GAMING)
                    PurposeDetail.MAINSTREAM_GAMING -> path(Laptop::category).eq(LaptopCategory.MAINSTREAM_GAMING)
                    PurposeDetail.HEAVY_GAMING -> path(Laptop::category).eq(LaptopCategory.HEAVY_GAMING)
                    PurposeDetail.OFFICE_LOL -> path(Laptop::category).eq(LaptopCategory.OFFICE_LOL)
                }
            )
        }

        if (recommendedLaptops.isEmpty()) {
            return emptyList()
        }

        return recommendedLaptops.mapNotNull { laptop ->
            laptop?.let {
                LaptopRecommendationResponse(
                    manufacturer = it.manufacturer,
                    price = it.price,
                    name = it.name,
                    weight = it.weight,
                    thunderBoltPorts = it.thunderBoltPorts,
                    usb4Ports = it.usb4Ports,
                    sdCardType = it.sdCardType,
                    isTenKey = it.isTenKey,
                    cpu = it.cpus.map { cpu -> CpuResponse.of(cpu.cpu) },
                    gpu = it.gpus.map { gpu -> GpuResponse.of(gpu.gpu) },
                    ramSlot = it.rams[0].slot,
                    ramCapacity = it.rams.map { ram -> ram.capacity },
                    ramClockSpeed = it.rams[0].clockSpeed,
                    ramDdrType = it.rams[0].ddrType,
                    displays = it.displays.map { display -> DisplayResponse.of(display) },
                    storageSlot = it.storages[0].slot,
                    storageCapacity = it.storages.map { storage-> storage.capacity }
                )
            }
        }
    }
}

