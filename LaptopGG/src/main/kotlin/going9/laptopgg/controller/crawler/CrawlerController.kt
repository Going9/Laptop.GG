package going9.laptopgg.controller.crawler

import org.springframework.batch.core.Job
import org.springframework.batch.core.JobParameters
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/crawl")
class CrawlerController(
    private val jobLauncher: JobLauncher,
    private val laptopJob: Job,
) {

    @GetMapping("/laptops")
    fun startCrawling(): ResponseEntity<String> {
        return try {
            // 크롤링 잡 실행
            val jobParameters = JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .toJobParameters()

            jobLauncher.run(laptopJob, jobParameters)

            ResponseEntity.ok("크롤링 시작")
        } catch (e: Exception) {
            ResponseEntity.status(500).body("${e}")
        }
    }
}