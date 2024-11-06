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
        options.addArguments("--headless")
        return ChromeDriver(options)
    }
}