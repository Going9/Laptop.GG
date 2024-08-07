package going9.laptopgg.domain.laptop

import jakarta.persistence.*

@Entity
class LaptopRam(

    @ManyToOne
    @JoinColumn(name = "laptop_id")
    val laptop: Laptop,

    val capacity: Int,  // RAM 용량

    val slot: Int? = null,  // RAM 슬롯 수, 업그레이드 가능 시 슬롯 수 입력 불가능하면 null

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
) {
}
