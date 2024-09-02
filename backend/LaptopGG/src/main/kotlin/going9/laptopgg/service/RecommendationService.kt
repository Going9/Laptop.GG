package going9.laptopgg.service

import com.linecorp.kotlinjdsl.dsl.jpql.jpql
import com.linecorp.kotlinjdsl.querymodel.Query
import com.linecorp.kotlinjdsl.render.jpql.JpqlRenderContext
import going9.laptopgg.domain.laptop.*
import going9.laptopgg.domain.repository.LaptopRepository
import going9.laptopgg.dto.request.LaptopRecommendationRequest
import going9.laptopgg.dto.request.PurposeDetail
import going9.laptopgg.dto.request.TenKeyPreference
import going9.laptopgg.dto.response.LaptopRecommendationResponse
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class RecommendationService(
    val laptopRepository: LaptopRepository,
    val entityManager: EntityManager,
) {

    @Transactional
    fun recommendLaptop(request: LaptopRecommendationRequest): List<LaptopRecommendationResponse> {
        val recommendedLaptops: List<Laptop?> = laptopRepository.findAll {
            select(
                entity(Laptop::class)
            ).from(
                entity(Laptop::class),
                join(Display::laptop),

            ).whereAnd(
                path(Laptop::price).le(request.budget),
                path(Laptop::weight).le(request.weight),
                path(Display::screenSize).`in`(request.displaySize),
                if (request.isTenKey != TenKeyPreference.DOSE_NOT_MATTER) {
                    path(Laptop::isTenKey).eq(request.isTenKey == TenKeyPreference.NEEDED)
                } else null,

                // 목적에 따른 조건 추가
                when(request.purpose) {
                    PurposeDetail.OFFICE -> path(Laptop::category).eq(LaptopCategory.OFFICE)
                    PurposeDetail.CREATOR -> path(Laptop::category).eq(LaptopCategory.CREATOR)
                    PurposeDetail.LIGHT_OFFICE -> and(
                        path(Laptop::category).eq(LaptopCategory.OFFICE),
                        path(Laptop::weight).le(1.5),
                    )
                    PurposeDetail.LIGHT_GAMING -> path(Laptop::category).eq(LaptopCategory.LIGHT_GAMING)
                    PurposeDetail.MAINSTREAM_GAMING -> path(Laptop::category).eq(LaptopCategory.MAINSTREAM_GAMING)
                    PurposeDetail.HEAVY_GAMING -> path(Laptop::category).eq(LaptopCategory.HEAVY_GAMING)
                    PurposeDetail.OFFICE_LOL -> path(Laptop::category).eq(LaptopCategory.OFFICE_LOL)
                }
            )
        }

        return mutableListOf()
    }
}

