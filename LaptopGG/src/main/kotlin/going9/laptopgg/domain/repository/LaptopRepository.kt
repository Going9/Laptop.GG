package going9.laptopgg.domain.repository

import com.linecorp.kotlinjdsl.support.spring.data.jpa.repository.KotlinJdslJpqlExecutor
import going9.laptopgg.domain.laptop.Laptop
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param


interface LaptopRepository : JpaRepository<Laptop, Long>, KotlinJdslJpqlExecutor {
    fun findLaptopById(id: Long): Laptop?
}