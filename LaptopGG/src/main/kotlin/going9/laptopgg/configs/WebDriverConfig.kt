package going9.laptopgg.configs

import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class WebDriverConfig {

    @Bean
    fun webDriver(): WebDriver {
        val options = ChromeOptions()
        options.addArguments("--headless") // GUI 없이 실행
        options.addArguments("--no-sandbox") // 권한 문제 회피
        options.addArguments("--disable-dev-shm-usage") // /dev/shm 사용하지 않음
        options.addArguments("--disable-gpu") // GPU 비활성화 (리눅스 환경에서는 불필요)
        options.addArguments("--remote-allow-origins=*") // CORS 문제 회피

        return ChromeDriver(options)
    }
}