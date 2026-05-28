package going9.laptopgg.application.crawler.profile

import going9.laptopgg.taxonomy.GpuClass
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class GpuClassifierTest {
    private val classifier = GpuClassifier()

    @Test
    fun `catalog keeps specific gpu model ordering before broader matches`() {
        val result = classifier.classifyGraphics("GeForce RTX5070 Ti Laptop GPU")

        assertThat(result.gpuClass).isEqualTo(GpuClass.DISCRETE_HIGH)
        assertThat(result.performanceScore).isEqualTo(93)
        assertThat(result.isIntegrated).isFalse()
    }

    @Test
    fun `catalog keeps workstation creator bonus`() {
        val result = classifier.classifyGraphics("NVIDIA RTX PRO5000 Blackwell")

        assertThat(result.gpuClass).isEqualTo(GpuClass.WORKSTATION)
        assertThat(result.performanceScore).isEqualTo(92)
        assertThat(result.creatorBonus).isEqualTo(12)
        assertThat(result.isIntegrated).isFalse()
    }

    @Test
    fun `catalog supports multi token adreno performance hints`() {
        val result = classifier.classifyGraphics("Qualcomm Adreno GPU 4.6 TFLOPS")

        assertThat(result.gpuClass).isEqualTo(GpuClass.INTEGRATED_HIGH)
        assertThat(result.performanceScore).isEqualTo(60)
        assertThat(result.isIntegrated).isTrue()
    }

    @Test
    fun `integrated graphics lookup is delegated to catalog keywords`() {
        assertThat(classifier.isIntegratedGraphics(graphicsKind = null, graphicsModel = "Radeon 890M")).isTrue()
        assertThat(classifier.isIntegratedGraphics(graphicsKind = null, graphicsModel = "Radeon RX 7600S")).isFalse()
    }
}
