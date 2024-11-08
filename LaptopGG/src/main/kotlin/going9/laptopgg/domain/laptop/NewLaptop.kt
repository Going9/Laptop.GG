package going9.laptopgg.domain.laptop

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id

@Entity
class NewLaptop(

    val name: String,
    val screenSize: Double,  // 화면 크기 (인치)
    val resolution: String?, // 해상도
    val brightness: Int?,    // 밝기 (니트)
    val refreshRate: Int?,   // 주사율 (Hz)
    val ramSize: Int,        // 램 용량 (GB)
    val ramType: String,     // 램 타입 (예: DDR4)
    val ramClock: Int?,      // 램 클럭 속도 (MHz)
    val isRamReplaceable: Boolean,  // 램 교체 가능 여부
    val graphicsType: String,       // 그래픽 타입 (내장/외장)
    val tgp: Int?,                  // TGP (W) (외장 그래픽에만 적용)
    val videoOutput: String?,       // 영상 출력 포트
    val thunderboltCount: Int?,     // 썬더볼트 포트 개수
    val usbCCount: Int?,            // USB-C 포트 개수
    val usbACount: Int?,            // USB-A 포트 개수
    val hasSdCardSlot: Boolean,     // SD 카드 슬롯 유무
    val batteryCapacity: Int?,      // 배터리 용량 (Wh)
    val supportsPdCharging: Boolean, // USB PD 충전 지원 여부
    val storageCapacity: Int,       // 저장 장치 용량 (GB)
    val storageSlotCount: Int?,     // 저장 장치 슬롯 수
    val weight: Double,             // 무게 (kg)
    val operatingSystem: String,    // 운영체제
    val usage: String,              // 용도

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long

) {
}