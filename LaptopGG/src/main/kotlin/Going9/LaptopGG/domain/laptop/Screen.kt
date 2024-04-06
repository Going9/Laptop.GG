package Going9.LaptopGG.domain.laptop

import jakarta.persistence.*

@Entity
class Screen(

    val panel: String,

    @OneToMany(mappedBy = "screen")
    val laptops: MutableList<LaptopScreen>,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
) {
}