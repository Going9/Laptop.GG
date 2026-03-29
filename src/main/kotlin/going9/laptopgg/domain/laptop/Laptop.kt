package going9.laptopgg.domain.laptop

import jakarta.persistence.*

@Entity
data class Laptop(
    var name: String,
    var imageUrl: String,
    var detailPage: String,
    var price: Int?,
    var cpuManufacturer: String?,
    var cpu: String?,
    var os: String?,
    var screenSize: Int?,  // 화면 크기 (인치)
    var resolution: String?, // 해상도
    var brightness: Int?,    // 밝기 (니트)
    var refreshRate: Int?,   // 주사율 (Hz)
    var ramSize: Int?,        // 램 용량 (GB)
    var ramType: String?,     // 램 타입 (예: DDR4)
    var isRamReplaceable: Boolean?,    // 램 교체 가능 여부
    var graphicsType: String?,       // 그래픽 칩셋
    var tgp: Int?,                  // TGP (W) (외장 그래픽에만 적용)
    var thunderboltCount: Int?,     // 썬더볼트 포트 개수
    var usbCCount: Int?,            // USB-C 포트 개수
    var usbACount: Int?,            // USB-A 포트 개수
    var sdCard: String?,
    var isSupportsPdCharging: Boolean?, // USB PD 충전 지원 여부
    var batteryCapacity: Double?,      // 배터리 용량 (Wh)
    var storageCapacity: Int?,       // 저장 장치 용량 (GB)
    var storageSlotCount: Int?,     // 저장 장치 슬롯 수
    var weight: Double?,             // 무게 (kg)

    @OneToMany(mappedBy = "laptop", cascade = [CascadeType.ALL], orphanRemoval = true)
    var laptopUsage: MutableList<LaptopUsage> = mutableListOf(),

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null
)
