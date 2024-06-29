package going9.laptopgg.domain.laptop

import jakarta.persistence.*

@Entity
class LaptopCpu(

    @ManyToOne
    @JoinColumn(name = "laptop_id")
    val laptop: Laptop,

    @ManyToOne
    @JoinColumn(name = "cpu_id")
    val cpu: Cpu,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
) {

}