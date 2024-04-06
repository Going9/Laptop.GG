package Going9.LaptopGG.domain.laptop

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

    val mux: Boolean,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
) {
}