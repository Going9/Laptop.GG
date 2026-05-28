package going9.laptopgg.application.crawler.profile

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class GpuModelKeywordCatalogTest {
    @Test
    fun `keyword catalog distinguishes integrated and discrete model names`() {
        assertThat(GpuModelKeywordCatalog.isIntegratedModel("RADEON 890M")).isTrue()
        assertThat(GpuModelKeywordCatalog.isIntegratedModel("ADRENO X2-90")).isTrue()
        assertThat(GpuModelKeywordCatalog.isDiscreteModel("GEFORCE RTX 4060")).isTrue()
        assertThat(GpuModelKeywordCatalog.isDiscreteModel("RADEON RX 7600S")).isTrue()
    }
}
