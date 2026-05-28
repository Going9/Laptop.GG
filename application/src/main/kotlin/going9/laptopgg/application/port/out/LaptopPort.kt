package going9.laptopgg.application.port.out

import going9.laptopgg.domain.laptop.Laptop

interface LaptopPort {
    fun findById(laptopId: Long): Laptop?
    fun findWithUsageById(laptopId: Long): Laptop?
}
