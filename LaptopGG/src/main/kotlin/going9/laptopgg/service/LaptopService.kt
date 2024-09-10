package going9.laptopgg.service

import going9.laptopgg.domain.laptop.Laptop
import going9.laptopgg.domain.repository.LaptopRepository
import going9.laptopgg.dto.request.*
import going9.laptopgg.dto.response.DisplayResponse
import going9.laptopgg.dto.response.LaptopRecommendationDetailResponse
import going9.laptopgg.util.findByOrThrow
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class LaptopService(
    private val laptopRepository: LaptopRepository,
    private val cpuService: CpuService,
    private val laptopCpuService: LaptopCpuService,
    private val gpuService: GpuService,
    private val laptopGpuService: LaptopGpuService,
    private val ramService: RamService,
    private val displayService: DisplayService,
    private val storageService: StorageService,
) {

    @Transactional
    fun saveLaptop(request: LaptopRequest) {
        // Laptop 엔티티 생성 및 저장
        val laptop = createLaptopFromRequest(request)
        laptopRepository.save(laptop)

        // 각 컴포넌트의 매핑 처리
        mapCpusToLaptop(laptop, request.cpus)
        mapGpusToLaptop(laptop, request.gpus)
        mapRamsToLaptop(laptop, request.rams)
        mapDisplaysToLaptop(laptop, request.displays)
        mapStoragesToLaptop(laptop, request.storages)
    }

    private fun createLaptopFromRequest(request: LaptopRequest): Laptop {
        println(
            request.toString()
        )
        return Laptop(
            imgLink = request.imgLink,
            price = request.price,
            priceLink = request.priceLink,
            name = request.name,
            manufacturer = request.manufacturer,
            category = request.category,
            weight = request.weight,
            thunderBoltPorts = request.thunderBoltPorts,
            usb4Ports = request.usb4Ports,
            batteryCapacity = request.batteryCapacity,
            sdCardType = request.sdCardType,
            isTenKey = request.isTenKey,
        )
    }

    private fun mapCpusToLaptop(laptop: Laptop, cpuIds: List<Long>) {
        val cpus = cpuService.findByIds(cpuIds)
        cpus.forEach { cpu ->
            laptopCpuService.saveLaptopCpu(laptop, cpu)
        }
    }

    private fun mapGpusToLaptop(laptop: Laptop, gpuRequests: List<LaptopGpuRequest>) {
        gpuRequests.forEach { laptopGpuInfo ->
            val gpu = gpuService.findById(laptopGpuInfo.gpuId)
            laptopGpuService.saveLaptopGpu(laptop, gpu, laptopGpuInfo)
        }
    }

    private fun mapRamsToLaptop(laptop: Laptop, ramRequests: List<RamRequest>) {
        ramRequests.forEach { ramRequest ->
            ramService.saveRam(laptop, ramRequest)
        }
    }

    private fun mapDisplaysToLaptop(laptop: Laptop, displayRequests: List<DisplayRequest>) {
        displayRequests.forEach { displayRequest ->
            displayService.saveDisplay(laptop, displayRequest)
        }
    }

    private fun mapStoragesToLaptop(laptop: Laptop, storageRequests: List<StorageRequest>) {
        storageRequests.forEach { storageRequest ->
            storageService.saveStorage(laptop, storageRequest)
        }
    }

    @Transactional
    fun findLaptopById(id: Long): LaptopRecommendationDetailResponse {
        val laptop =  laptopRepository.findByOrThrow(id)

        return LaptopRecommendationDetailResponse(
            id = laptop.id!!,
            imgLink = laptop.imgLink,
            priceLink = laptop.priceLink,
            batteryCapacity = laptop.batteryCapacity,
            manufacturer = laptop.manufacturer,
            price = laptop.price,
            name = laptop.name,
            weight = laptop.weight,
            thunderBoltPorts = laptop.thunderBoltPorts,
            usb4Ports = laptop.usb4Ports,
            sdCardType = laptop.sdCardType,
            isTenKey = laptop.isTenKey,
            cpus = laptop.cpus.map { it.cpu.name },
            gpus = laptop.gpus.map { it.gpu.name },
            ramSlot = laptop.rams.first().slot,
            ramCapacity = laptop.rams.map { it.capacity },
            ramClockSpeed = laptop.rams.first().clockSpeed,
            ramDdrType = laptop.rams.first().ddrType,
            displays = laptop.displays.map { DisplayResponse.of(it) },
            storageSlot = laptop.storages.first().slot,
            storageCapacity = laptop.storages.map { it.capacity },
        )
    }
}
