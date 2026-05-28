package going9.laptopgg

import org.junit.jupiter.api.Test
import org.assertj.core.api.Assertions.assertThat
import org.springframework.beans.factory.ListableBeanFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.env.Environment
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LaptopGgApplicationTests {
	@Autowired
	lateinit var beanFactory: ListableBeanFactory

	@Autowired
	lateinit var mockMvc: MockMvc

	@Autowired
	lateinit var environment: Environment

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
			"crawledLaptopPersistenceJpaAdapter",
			"crawledLaptopProfileJpaAdapter",
			"crawledLaptopProfileSourceJpaAdapter",
			"crawlerAdvisoryLockJpaAdapter",
			"crawlerRunJpaAdapter",
			"crawlerTransactionJpaAdapter",
			"laptopPriceHistoryJpaAdapter",
			"recommendationScoreJpaAdapter",
			"crawlerLaptopRepository",
			"crawlerLaptopProfileRepository",
			"crawlerRunRepository",
			"laptopPriceHistoryRepository",
			"recommendationScoreRepository",
		)
	}

	@Test
	fun `web context scans only web persistence entities`() {
		val entityTypes = entitySimpleNames()

		assertThat(entityTypes).contains(
			"Laptop",
			"LaptopProfile",
			"LaptopUsage",
			"RecommendationScore",
			"Comment",
		)
		assertThat(entityTypes).doesNotContain(
			"CrawlerRun",
			"LaptopPriceHistory",
		)
	}

	private fun entitySimpleNames(): Set<String> {
		val entityManagerFactory = beanFactory.getBean("entityManagerFactory")
		val metamodel = entityManagerFactory.zeroArgMethod("getMetamodel").invoke(entityManagerFactory)
		val entities = metamodel.zeroArgMethod("getEntities").invoke(metamodel) as Collection<*>

		return entities.mapNotNull { entityType ->
			val javaType = entityType?.zeroArgMethod("getJavaType")?.invoke(entityType) as? Class<*>
			javaType?.simpleName
		}.toSet()
	}

	private fun Any.zeroArgMethod(name: String) = javaClass.methods.first { method ->
		method.name == name && method.parameterCount == 0
	}

	@Test
	fun `web context keeps crawler http api out of public surface`() {
		mockMvc.perform(get("/api/crawl/laptops"))
			.andExpect(status().isNotFound)
	}

	@Test
	fun `web context keeps legacy spec form out of public surface`() {
		mockMvc.perform(get("/spec-form"))
			.andExpect(status().isNotFound)
	}

	@Test
	fun `web context exposes health but not other actuator endpoints`() {
		mockMvc.perform(get("/actuator/health/readiness"))
			.andExpect(status().isOk)
		mockMvc.perform(get("/actuator/env"))
			.andExpect(status().isNotFound)
	}

	@Test
	fun `web runtime uses graceful shutdown settings`() {
		assertThat(environment.getProperty("server.shutdown")).isEqualTo("graceful")
		assertThat(environment.getProperty("spring.lifecycle.timeout-per-shutdown-phase")).isEqualTo("20s")
	}

	@Test
	fun `web api maps missing application resources to 404 response`() {
		mockMvc.perform(get("/api/laptops").param("id", "999999"))
			.andExpect(status().isNotFound)
			.andExpect(jsonPath("$.code").value("not_found"))
	}

	@Test
	fun `web api maps invalid application commands to 400 response`() {
		mockMvc.perform(
			post("/api/comments")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"laptopId":0,"author":"","content":"","passWord":""}"""),
		)
			.andExpect(status().isBadRequest)
			.andExpect(jsonPath("$.code").value("bad_request"))
	}
}
