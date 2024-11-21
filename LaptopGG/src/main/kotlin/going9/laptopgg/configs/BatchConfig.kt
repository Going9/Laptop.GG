package going9.laptopgg.config

import going9.laptopgg.batch.LaptopCrawlerTasklet
import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager

@Configuration
class BatchConfig(
    private val jobRepository: JobRepository,
    private val transactionManager: PlatformTransactionManager,
    private val laptopCrawlerTasklet: LaptopCrawlerTasklet
) {

    @Bean
    fun laptopJob(): Job {
        return JobBuilder("laptopJob", jobRepository)
            .start(laptopStep())
            .build()
    }

    @Bean
    fun laptopStep(): Step {
        return StepBuilder("laptopStep", jobRepository)
            .tasklet(laptopCrawlerTasklet, transactionManager)
            .build()
    }
}
