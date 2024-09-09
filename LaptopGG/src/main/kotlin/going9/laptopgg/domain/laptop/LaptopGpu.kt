package going9.laptopgg.domain.laptop

import jakarta.persistence.*

@Entity
class LaptopGpu(

    @ManyToOne
    @JoinColumn(name = "laptop_id")
    val laptop: Laptop,

    @ManyToOne
    @JoinColumn(name = "gpu_id")
    val gpu: Gpu,

    val tgp: Int? = null,
    val isMux: Boolean,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
) {
}