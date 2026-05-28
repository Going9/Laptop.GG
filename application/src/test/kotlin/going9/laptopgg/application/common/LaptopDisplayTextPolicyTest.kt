package going9.laptopgg.application.common

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class LaptopDisplayTextPolicyTest {
    @Test
    fun `manufacturer name is derived consistently from trimmed display name`() {
        assertThat(LaptopDisplayTextPolicy.manufacturerName("  LG   그램 Pro  ")).isEqualTo("LG")
        assertThat(LaptopDisplayTextPolicy.manufacturerName("   ")).isEqualTo("브랜드 확인 불가")
    }

    @Test
    fun `resolution label normalizes common text and pixel formats`() {
        assertThat(LaptopDisplayTextPolicy.resolutionLabel("2560x1600")).isEqualTo("QHD")
        assertThat(LaptopDisplayTextPolicy.resolutionLabel("3840 X 2160")).isEqualTo("UHD")
        assertThat(LaptopDisplayTextPolicy.resolutionLabel("1920x1080")).isEqualTo("FHD")
        assertThat(LaptopDisplayTextPolicy.resolutionLabel(null)).isNull()
    }

    @Test
    fun `os display text is normalized without leaking empty strings`() {
        assertThat(LaptopDisplayTextPolicy.humanizeOs("freedos")).isEqualTo("프리도스")
        assertThat(LaptopDisplayTextPolicy.humanizeOs("Windows 11 Home")).isEqualTo("윈도우 11 홈")
        assertThat(LaptopDisplayTextPolicy.humanizeOs("  ")).isNull()
    }
}
