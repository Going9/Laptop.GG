package going9.laptopgg.service

import going9.laptopgg.domain.repository.LaptopRepository
import org.springframework.stereotype.Service

@Service
class RecommendationService(
    val laptopRepository: LaptopRepository,
    val scoreCalculatorService: ScoreCalculatorService,
) {

//    @Transactional
//    fun recommendLaptop(request: LaptopRecommendationRequest): List<LaptopRecommendationListResponse> {
//        // Step 1: 조건에 맞는 랩톱 목록 조회
//        val recommendedLaptops = laptopRepository.findAll {
//            select(
//                entity(Laptop::class)
//            ).from(
//                entity(Laptop::class),
//                join(Laptop::displays),
//                join(Laptop::gpus),
//                join(LaptopGpu::gpu),
//            ).whereAnd(
//                path(Laptop::price).le(request.budget),
//                path(Laptop::weight).le(request.weight),
//                path(Display::screenSize).`in`(request.displaySize),
//                if (request.isTenKey != TenKeyPreference.DOSE_NOT_MATTER) {
//                    path(Laptop::isTenKey).eq(request.isTenKey == TenKeyPreference.NEEDED)
//                } else null,
//
//                // 목적에 따른 조건 추가
//                when (request.purpose) {
//                    PurposeDetail.OFFICE -> path(Laptop::category).eq(LaptopCategory.OFFICE)
//                    PurposeDetail.CREATOR -> path(Laptop::category).eq(LaptopCategory.CREATOR)
//                    PurposeDetail.LIGHT_OFFICE -> and(
//                        path(Laptop::category).eq(LaptopCategory.OFFICE),
//                        path(Laptop::weight).le(1.5),
//                    )
//                    PurposeDetail.LIGHT_GAMING -> path(Laptop::category).eq(LaptopCategory.LIGHT_GAMING)
//                    PurposeDetail.MAINSTREAM_GAMING -> path(Laptop::category).eq(LaptopCategory.MAINSTREAM_GAMING)
//                    PurposeDetail.HEAVY_GAMING -> path(Laptop::category).eq(LaptopCategory.HEAVY_GAMING)
//                    PurposeDetail.OFFICE_LOL -> and(
//                        path(Laptop::category).eq(LaptopCategory.OFFICE_LOL),
//                        path(Gpu::name).`in`(
//                            listOf(
//                                "Radeon 890M", "Radeon 880M", "Radeon 780M", "Radeon 760M",
//                                "Radeon 740M", "Radeon 680M", "Radeon 660M", "Radeon 610M", "Iris Xe"
//                            )
//                        )
//                    )
//                }
//            )
//        }
//
//        // Step 2: 점수 계산 후 정렬
//        val scoredLaptops = recommendedLaptops.mapNotNull { laptop ->
//            laptop?.let {
//                val score = scoreCalculatorService.calculateScore(it, request)
//                Pair(it, score)  // 각 랩톱과 점수를 함께 저장
//            }
//        }.sortedByDescending { it.second } // 점수를 기준으로 내림차순 정렬
//
//
//        // Step 3: 정렬된 결과를 LaptopRecommendationListResponse 로 변환
//        return scoredLaptops.map { (laptop, score) ->
//            LaptopRecommendationListResponse(
//                id = laptop.id!!,
//                score = score,
//                imgLink = laptop.imgLink,
//                price = laptop.price,
//                name = laptop.name,
//                manufacturer = laptop.manufacturer,
//                weight = laptop.weight
//            )
//        }
//    }
}


