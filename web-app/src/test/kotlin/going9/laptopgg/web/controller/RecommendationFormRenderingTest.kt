package going9.laptopgg.web.controller

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.context.bean.override.mockito.MockitoBean
import going9.laptopgg.application.recommendation.RecommendLaptopsUseCase
import going9.laptopgg.application.common.PagedResult
import going9.laptopgg.application.recommendation.LaptopRecommendationResult
import org.mockito.Mockito

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RecommendationFormRenderingTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @MockitoBean
    lateinit var recommendLaptopsUseCase: RecommendLaptopsUseCase

    @Test
    fun `condition editing renders unprefixed request fields and retained selections`() {
        val html = mockMvc.perform(
            get("/recommends")
                .param("budget", "1234567")
                .param("maxWeightKg", "1.4")
                .param("screenSizeMode", "SELECT")
                .param("screenSizes", "14", "16")
                .param("useCase", "OFFICE_STUDY"),
        ).andExpect(status().isOk).andReturn().response.contentAsString

        assertThat(html).contains("name=\"budget\"", "name=\"maxWeightKg\"", "name=\"screenSizes\"", "value=\"1234567\"", "value=\"1.4\"")
        assertThat(html).doesNotContain("name=\"laptopRecommendationRequest.")
        assertThat(html).containsPattern("id=\"screenSizeMode_SELECT\"[^>]*checked=\"checked\"")
        assertThat(html).containsPattern("id=\"useCase_OFFICE_STUDY\"[^>]*checked=\"checked\"")
    }

    @Test
    fun `HTTP sort retains comma direction for page and API requests`() {
        val request = going9.laptopgg.web.dto.request.LaptopRecommendationRequest()
        val query = request.toQuery()
        val pageQuery = pageQueryFrom(0, 10, listOf("price,desc"))
        Mockito.`when`(recommendLaptopsUseCase.recommend(query, pageQuery))
            .thenReturn(PagedResult(emptyList<LaptopRecommendationResult>(), 0, 10, 0))

        val html = mockMvc.perform(post("/recommends").param("sort", "price,desc"))
            .andExpect(status().isOk).andReturn().response.contentAsString
        assertThat(html).contains("sort=price,desc")
        mockMvc.perform(
            post("/api/recommends").param("sort", "price,desc")
                .contentType("application/json").content("{}"),
        ).andExpect(status().isOk)
        Mockito.verify(recommendLaptopsUseCase, Mockito.times(2)).recommend(query, pageQuery)
    }
}
