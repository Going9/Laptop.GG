package going9.laptopgg

import org.junit.jupiter.api.Test
import org.assertj.core.api.Assertions.assertThat
import org.springframework.beans.factory.ListableBeanFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LaptopGgApplicationTests {
	@Autowired
	lateinit var beanFactory: ListableBeanFactory

	@Autowired
	lateinit var mockMvc: MockMvc

	@Test
	fun contextLoads() {
	}

	@Test
	fun `web context does not load crawler beans`() {
		val beanNames = beanFactory.beanDefinitionNames.toSet()

		assertThat(beanNames).doesNotContain(
			"crawlerService",
			"danawaClient",
			"crawlerStartupRunner",
			"saveCrawledLaptopService",
			"trackCrawlerRunService",
			"crawledCpuModelResolver",
			"laptopProfileService",
			"laptopPriceHistoryService",
			"recommendationScoreService",
			"crawledLaptopJpaAdapter",
			"crawledLaptopProfileJpaAdapter",
			"crawlerRunJpaAdapter",
			"laptopPriceHistoryJpaAdapter",
			"recommendationScoreJpaAdapter",
			"crawlerRunRepository",
			"laptopPriceHistoryRepository",
			"recommendationScoreRepository",
		)
	}

	@Test
	fun `web context keeps crawler http api out of public surface`() {
		mockMvc.perform(get("/api/crawl/laptops"))
			.andExpect(status().isNotFound)
	}

	@Test
	fun `web context exposes health but not other actuator endpoints`() {
		mockMvc.perform(get("/actuator/health/readiness"))
			.andExpect(status().isOk)
		mockMvc.perform(get("/actuator/env"))
			.andExpect(status().isNotFound)
	}
}
