package going9.laptopgg

import org.junit.jupiter.api.Test
import org.assertj.core.api.Assertions.assertThat
import org.springframework.beans.factory.ListableBeanFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("test")
class LaptopGgApplicationTests {
	@Autowired
	lateinit var beanFactory: ListableBeanFactory

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
		)
	}
}
