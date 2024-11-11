package going9.laptopgg.domain.repository

import going9.laptopgg.domain.laptop.NewLaptop
import org.springframework.data.jpa.repository.JpaRepository

interface NewLaptopRepository : JpaRepository<NewLaptop, Long> {
    fun findByName(name: String): NewLaptop?
}