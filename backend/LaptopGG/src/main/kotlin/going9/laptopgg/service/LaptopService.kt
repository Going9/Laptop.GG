package going9.laptopgg.service

import going9.laptopgg.domain.laptop.*
import going9.laptopgg.domain.repository.LaptopRepository
import going9.laptopgg.domain.repository.StorageRepository
import going9.laptopgg.dto.request.LaptopRequest
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
    private val storageRepository: StorageRepository,
) {
    @Transactional
    fun saveLaptop(request: LaptopRequest) {
        // Laptop 엔티티 생성 및 저장
        val laptop = Laptop(
            imgLink = request.imgLink,
            price = request.price,
            priceLink = request.priceLink,
            name = request.name,
            manufacturer = request.manufacturer,
            mainCategory = request.mainCategory,
            subCategory = request.subCategory,
            weight = request.weight,
            thunderVoltPorts = request.thunderVoltPorts,
            usb4Ports = request.usb4Ports,
            batteryCapacity = request.batteryCapacity,
            sdCardType = request.sdCardType
        )

        laptopRepository.save(laptop)

        // 사용자가 입력한 CPU, GPU
        // 사용자에게 목록을 먼저 제공하고 해당 목록에서 선택된 cpu, gpu의 아이디로 중개 테이블에 등록
        val cpus: List<Cpu> = cpuService.findByIds(request.cpus)
        val gpus: List<Gpu> = gpuService.findByIds(request.gpus)

        // cpu
        cpus.forEach { cpu ->
            val laptopCpu = LaptopCpu(laptop = laptop, cpu = cpu)
            laptopCpuService.saveLaptopCpu(laptopCpu)
        }

        // gpu
        gpus.forEach { gpu ->
            val laptopGpu = LaptopGpu(laptop, gpu, request.tgp, request.isMux)
            laptopGpuService.saveLaptopGpu(laptopGpu)
        }

        // ram
        request.ramCapacity.forEach { ramCapacity ->
            val ram = Ram(laptop, ramCapacity, request.ramSlot, request.clockSpeed, request.ddrType)
            ramService.saveRam(ram)
        }

        // storage
        request.storageCapacity.forEach { storageCapacity ->
            val storage = Storage(laptop, storageCapacity, request.storageSlot)
            storageRepository.save(storage)
        }
    }
}
