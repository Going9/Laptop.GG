package going9.laptopgg.domain.repository

import going9.laptopgg.domain.laptop.Laptop
import org.springframework.data.jpa.repository.JpaRepository


interface LaptopRepository : JpaRepository<Laptop, Long> {

}