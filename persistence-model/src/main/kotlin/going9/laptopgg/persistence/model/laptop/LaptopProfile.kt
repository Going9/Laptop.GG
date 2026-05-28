package going9.laptopgg.persistence.model.laptop

import going9.laptopgg.recommendation.BatteryTier
import going9.laptopgg.recommendation.CpuClass
import going9.laptopgg.recommendation.GpuClass
import going9.laptopgg.recommendation.PortabilityTier
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
