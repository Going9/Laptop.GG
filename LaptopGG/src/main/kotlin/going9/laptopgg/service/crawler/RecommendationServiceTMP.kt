package going9.laptopgg.service.crawler

import going9.laptopgg.domain.laptop.*
import going9.laptopgg.domain.repository.NewLaptopRepository
import going9.laptopgg.dto.request.LaptopRecommendationRequest
import going9.laptopgg.dto.request.PurposeDetail
import going9.laptopgg.dto.response.LaptopRecommendationListResponse
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class RecommendationServiceTMP(
    val newLaptopRepository: NewLaptopRepository,
    private val scoreCalculatorServiceTMP: ScoreCalculatorServiceTMP,
) {

    @Transactional
    fun recommendLaptop(request: LaptopRecommendationRequest): List<LaptopRecommendationListResponse> {
        // 조건에 맞는 랩탑 목록 조회
        val filteredLaptops = newLaptopRepository.findAll() {
            select(
                entity(NewLaptop::class)
            ).from(
                entity(NewLaptop::class),
                join(NewLaptop::laptopUsage)
            ).whereAnd(
                path(NewLaptop::price).le(request.budget),
                path(NewLaptop::weight).le(request.weight),
                path(NewLaptop::screenSize).`in`(request.displaySize),

                when (request.purpose) {
                    PurposeDetail.OFFICE -> or(
                        path(LaptopUsage::usage).`in`("휴대용", "사무")
                    )
                    PurposeDetail.CREATOR -> path(LaptopUsage::usage).`in`("그래픽작업용")
                    PurposeDetail.LIGHT_OFFICE -> path(LaptopUsage::usage).`in`("휴대용")
                    PurposeDetail.LIGHT_GAMING -> and(
                        path(LaptopUsage::usage).`in`("게임용"),
                        path(NewLaptop::weight).le(2.0),
                    )
                    PurposeDetail.MAINSTREAM_GAMING -> and(
                        path(LaptopUsage::usage).`in`("게임용"),
                        path(NewLaptop::tgp).gt(100),
                        path(NewLaptop::graphicsType).`in`(listOf(
                            "RTX4090", "RTX4080", "RTX4070", "RTX4060", "RTX4050",
                            "RTX3080 Ti", "RTX3080", "RTX3070 Ti", "RTX3070",
                            "RTX3060", "RTX3050 Ti", "RTX3050",
                            "라데온 RX 7600M XT", "라데온 RX 7600S", "라데온 RX 6800S",
                            "라데온 RX 6700S", "라데온 RX 6850M XT", "라데온 RX 6700M",
                            "라데온 RX 6600M", "라데온 RX 6650M", "라데온 RX 6500M",
                        ))
                    )
                    PurposeDetail.HEAVY_GAMING -> and(
                        path(LaptopUsage::usage).`in`("게임용"),
                        path(NewLaptop::tgp).gt(120),
                        path(NewLaptop::graphicsType).`in`(listOf(
                            "RTX4090",
                            "RTX4080",
                            "RTX4070",
                            "RTX4060",
                            "RTX4050",
                            "RTX3080 Ti",
                            "RTX3080",
                            "RTX3070 Ti",
                            "RTX3070",
                            "RTX3060",
                            "라데온 RX 7600M XT",
                            "라데온 RX 7600S",
                            "라데온 RX 6800S",
                        ))
                    )
                    PurposeDetail.OFFICE_LOL -> and(
                        path(NewLaptop::graphicsType).`in`(
                            listOf(
                                "Radeon 890M",
                                "Radeon 880M",
                                "Radeon 780M",
                                "Radeon 760M",
                                "Radeon 740M",
                                "Radeon 680M",
                                "Radeon 660M",
                                "Radeon 610M",
                                "Iris Xe",
                                "Arc 130V",
                                "Arc 140V",
                                "Arc",
                                "MX570 A",
                                "MX570",
                                "MX550",
                                "MX450",
                                "MX350",
                                "MX330",
                                "MX250",
                                "MX230",
                                "MX150",
                                "MX130",
                            )
                        )
                    )
                }
            )
        }

        // Step 2: 점수 계산 후 정렬
        val scoredLaptops = filteredLaptops.mapNotNull { laptop ->
            laptop?.let {
                val score = scoreCalculatorServiceTMP.calculateScore(it, request)
                Pair(it, score)  // 각 랩톱과 점수를 함께 저장
            }
        }.sortedByDescending { it.second } // 점수를 기준으로 내림차순 정렬

        return scoredLaptops.map { (laptop, score) ->
            LaptopRecommendationListResponse(
                id = laptop.id!!,
                score = score,
                imgLink = laptop.imageUrl,
                price = laptop.price!!,
                name = laptop.name,
                manufacturer = laptop.name.substringBefore(" "),
                weight = laptop.weight!!
            )
        }
    }

    @Transactional
    fun recommendLaptops(request: LaptopRecommendationRequest, pageable: Pageable): Page<LaptopRecommendationListResponse> {
        // 조건에 맞는 랩탑 목록 조회 (페이징 추가)
        val filteredLaptopsPage = newLaptopRepository.findPage(pageable) {
            select(
                entity(NewLaptop::class)
            ).from(
                entity(NewLaptop::class),
                join(NewLaptop::laptopUsage)
            ).whereAnd(
                path(NewLaptop::price).le(request.budget),
                path(NewLaptop::weight).le(request.weight),
                path(NewLaptop::screenSize).`in`(request.displaySize),

                when (request.purpose) {
                    PurposeDetail.OFFICE -> or(
                        path(LaptopUsage::usage).`in`("휴대용", "사무")
                    )
                    PurposeDetail.CREATOR -> path(LaptopUsage::usage).`in`("그래픽작업용")
                    PurposeDetail.LIGHT_OFFICE -> path(LaptopUsage::usage).`in`("휴대용")
                    PurposeDetail.LIGHT_GAMING -> and(
                        path(LaptopUsage::usage).`in`("게임용"),
                        path(NewLaptop::weight).le(2.0),
                    )
                    PurposeDetail.MAINSTREAM_GAMING -> and(
                        path(LaptopUsage::usage).`in`("게임용"),
                        path(NewLaptop::tgp).gt(100),
                        path(NewLaptop::graphicsType).`in`(listOf(
                            "RTX4090", "RTX4080", "RTX4070", "RTX4060", "RTX4050",
                            "RTX3080 Ti", "RTX3080", "RTX3070 Ti", "RTX3070",
                            "RTX3060", "RTX3050 Ti", "RTX3050",
                            "라데온 RX 7600M XT", "라데온 RX 7600S", "라데온 RX 6800S",
                            "라데온 RX 6700S", "라데온 RX 6850M XT", "라데온 RX 6700M",
                            "라데온 RX 6600M", "라데온 RX 6650M", "라데온 RX 6500M",
                        ))
                    )
                    PurposeDetail.HEAVY_GAMING -> and(
                        path(LaptopUsage::usage).`in`("게임용"),
                        path(NewLaptop::tgp).gt(120),
                        path(NewLaptop::graphicsType).`in`(listOf(
                            "RTX4090",
                            "RTX4080",
                            "RTX4070",
                            "RTX4060",
                            "RTX4050",
                            "RTX3080 Ti",
                            "RTX3080",
                            "RTX3070 Ti",
                            "RTX3070",
                            "RTX3060",
                            "라데온 RX 7600M XT",
                            "라데온 RX 7600S",
                            "라데온 RX 6800S",
                        ))
                    )
                    PurposeDetail.OFFICE_LOL -> and(
                        path(NewLaptop::graphicsType).`in`(
                            listOf(
                                "Radeon 890M",
                                "Radeon 880M",
                                "Radeon 780M",
                                "Radeon 760M",
                                "Radeon 740M",
                                "Radeon 680M",
                                "Radeon 660M",
                                "Radeon 610M",
                                "Iris Xe",
                                "Arc 130V",
                                "Arc 140V",
                                "Arc",
                                "MX570 A",
                                "MX570",
                                "MX550",
                                "MX450",
                                "MX350",
                                "MX330",
                                "MX250",
                                "MX230",
                                "MX150",
                                "MX130",
                            )
                        )
                    )
                }
            )
        }

        // Step 2: 점수 계산 후 정렬
        // 점수 계산
        val scoredLaptops = filteredLaptopsPage.content.mapNotNull { laptop ->
            laptop?.let {
                val score = scoreCalculatorServiceTMP.calculateScore(it, request)
                Pair(it, score)
            }
        }

        // 정렬 적용
        val sortedLaptops = if (pageable.sort.isSorted) {
            val orders = pageable.sort.toList()
            scoredLaptops.sortedWith(Comparator { a, b ->
                for (order in orders) {
                    val property = order.property
                    val direction = order.direction
                    val comparison = when (property) {
                        "weight" -> a.first.weight!!.compareTo(b.first.weight!!)
                        "price" -> a.first.price!!.compareTo(b.first.price!!)
                        else -> 0
                    }
                    if (comparison != 0) {
                        return@Comparator if (direction.isAscending) comparison else -comparison
                    }
                }
                0
            })
        } else {
            // 기본적으로 점수로 내림차순 정렬
            scoredLaptops.sortedByDescending { it.second }
        }

        // 페이지 형태로 변환
        val responseContent = sortedLaptops.map { (laptop, score) ->
            LaptopRecommendationListResponse(
                id = laptop.id!!,
                score = score,
                imgLink = laptop.imageUrl,
                price = laptop.price!!,
                name = laptop.name,
                manufacturer = laptop.name.substringBefore(" "),
                weight = laptop.weight!!
            )
        }

        return PageImpl(responseContent, pageable, filteredLaptopsPage.totalElements)
    }

}