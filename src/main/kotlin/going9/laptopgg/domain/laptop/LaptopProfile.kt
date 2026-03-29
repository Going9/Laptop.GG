package going9.laptopgg.domain.laptop

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table

@Entity
@Table(name = "laptop_profile")
class LaptopProfile(
    @OneToOne(optional = false)
    @JoinColumn(name = "laptop_id", nullable = false, unique = true)
    var laptop: Laptop,

    @Enumerated(EnumType.STRING)
    var cpuClass: CpuClass,

    @Enumerated(EnumType.STRING)
    var gpuClass: GpuClass,

    @Enumerated(EnumType.STRING)
    var batteryTier: BatteryTier,

    @Enumerated(EnumType.STRING)
    var portabilityTier: PortabilityTier,

    var officeScore: Int,
    var batteryScore: Int,
    var casualGameScore: Int,
    var onlineGameScore: Int,
    var aaaGameScore: Int,
    var creatorScore: Int,
    var cpuPerformanceScore: Int,
    var lowPowerCpuScore: Int,
    var gpuPerformanceScore: Int,
    var gpuCreatorBonus: Int,
    var portabilityScore: Int,
    var displayScore: Int,
    var ramScore: Int,
    var tgpScore: Int,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
)

enum class CpuClass {
    ULTRA_LOW_POWER,
    LOW_POWER,
    BALANCED,
    PERFORMANCE,
    ENTHUSIAST,
    WORKSTATION,
    UNKNOWN,
}

enum class GpuClass {
    INTEGRATED_ENTRY,
    INTEGRATED_MAINSTREAM,
    INTEGRATED_HIGH,
    DISCRETE_ENTRY,
    DISCRETE_MAINSTREAM,
    DISCRETE_HIGH,
    DISCRETE_ENTHUSIAST,
    WORKSTATION,
    UNKNOWN,
}

enum class BatteryTier {
    VERY_LOW,
    LOW,
    MEDIUM,
    HIGH,
    VERY_HIGH,
    UNKNOWN,
}

enum class PortabilityTier {
    TABLET_LIGHT,
    ULTRALIGHT,
    LIGHT,
    BALANCED,
    HEAVY,
    UNKNOWN,
}
