package going9.laptopgg.domain.repository

import com.linecorp.kotlinjdsl.support.spring.data.jpa.repository.KotlinJdslJpqlExecutor
import going9.laptopgg.domain.laptop.NewLaptop
import org.springframework.data.jpa.repository.JpaRepository

interface NewLaptopRepository : JpaRepository<NewLaptop, Long>, KotlinJdslJpqlExecutor {
    fun findByName(name: String): NewLaptop?
}