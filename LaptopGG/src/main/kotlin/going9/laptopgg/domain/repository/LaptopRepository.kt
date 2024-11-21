package going9.laptopgg.domain.repository

import com.linecorp.kotlinjdsl.support.spring.data.jpa.repository.KotlinJdslJpqlExecutor
import going9.laptopgg.domain.laptop.Laptop
import org.springframework.data.jpa.repository.JpaRepository

interface LaptopRepository : JpaRepository<Laptop, Long>, KotlinJdslJpqlExecutor {
    fun findByName(name: String): Laptop?
}