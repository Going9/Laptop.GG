package going9.laptopgg.recommendation

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
