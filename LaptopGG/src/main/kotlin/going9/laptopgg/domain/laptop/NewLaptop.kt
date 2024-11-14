package going9.laptopgg.domain.laptop

import jakarta.persistence.CollectionTable
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn

@Entity
data class NewLaptop(
    val name: String,
    val imageUrl: String,
    var price: Int,
    val cpuManufacturer: String?,
    val cpu: String?,
    val os: String?,
    val screenSize: Double?,  // 화면 크기 (인치)
    val resolution: Int?, // 해상도
    val brightness: Int?,    // 밝기 (니트)
    val refreshRate: Int?,   // 주사율 (Hz)
    val ramSize: Int?,        // 램 용량 (GB)
    val ramType: String?,     // 램 타입 (예: DDR4)
    val isRamReplaceable: Boolean?,    // 램 교체 가능 여부
    val graphicsType: String?,       // 그래픽 타입 (내장/외장)
    val tgp: Int?,                  // TGP (W) (외장 그래픽에만 적용)
    val thunderboltCount: Int?,     // 썬더볼트 포트 개수
    val usbCCount: Int?,            // USB-C 포트 개수
    val usbACount: Int?,            // USB-A 포트 개수
    val sdCard: String?,
    val isSupportsPdCharging: Boolean?, // USB PD 충전 지원 여부
    val batteryCapacity: Double?,      // 배터리 용량 (Wh)
    val storageCapacity: Int?,       // 저장 장치 용량 (GB)
    val storageSlotCount: Int?,     // 저장 장치 슬롯 수
    val weight: Double?,             // 무게 (kg)

    @ElementCollection
    @CollectionTable(name = "laptop_usage", joinColumns = [JoinColumn(name = "new_laptop_id")])
    val laptopUsage: List<String>?,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null
)