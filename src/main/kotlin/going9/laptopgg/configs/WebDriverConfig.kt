package going9.laptopgg.configs

import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Lazy
import org.springframework.context.annotation.Scope

@Configuration
class WebDriverConfig {

    @Bean
    @Lazy
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    fun webDriver(): WebDriver {
        val options = ChromeOptions()
        options.addArguments("--headless=new") // GUI 없이 실행
        options.addArguments("--no-sandbox") // 권한 문제 회피
        options.addArguments("--disable-dev-shm-usage") // /dev/shm 사용하지 않음
        options.addArguments("--disable-gpu") // GPU 비활성화 (리눅스 환경에서는 불필요)
        options.addArguments("--remote-allow-origins=*") // CORS 문제 회피
        options.addArguments("--window-size=1920,1080")
        options.addArguments("--lang=ko-KR")

        return ChromeDriver(options)
    }
}
