package going9.laptopgg

import org.springframework.batch.core.Job
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled

@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
@EnableJpaRepositories
@EnableScheduling  // 스케줄링 활성화
class LaptopGgApplication(
	private val jobLauncher: JobLauncher,
	private val laptopJob: Job,
) {
	// 새벽 4시마다 크롤링 작업을 실행하는 메서드
	@Scheduled(cron = "0 0 4 * * *") // 매일 새벽 4시 정각에 실행
	fun runScheduledJob() {
		val jobParameters = JobParametersBuilder()
			.addLong("time", System.currentTimeMillis())
			.toJobParameters()
		jobLauncher.run(laptopJob, jobParameters)
	}
}

fun main(args: Array<String>) {
	runApplication<LaptopGgApplication>(*args)
}
