package going9.laptopgg.controller

import going9.laptopgg.application.common.PagedResult
import going9.laptopgg.application.comment.AddCommentCommand
import going9.laptopgg.application.comment.CommentResult
import going9.laptopgg.application.comment.UpdateCommentCommand
import going9.laptopgg.application.comment.ManageCommentUseCase
import going9.laptopgg.application.laptop.GetLaptopDetailUseCase
import going9.laptopgg.application.laptop.LaptopDetailResult
import going9.laptopgg.application.recommendation.LaptopRecommendationResult
import going9.laptopgg.application.recommendation.RecommendLaptopsUseCase
import going9.laptopgg.dto.request.CommentRequest
import going9.laptopgg.dto.request.LaptopRecommendationRequest
import going9.laptopgg.dto.response.CommentResponse
import going9.laptopgg.dto.response.LaptopDetailResponse
import going9.laptopgg.dto.response.LaptopRecommendationListResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.ui.ExtendedModelMap

class PageControllerTest {
    private val getLaptopDetailUseCase = Mockito.mock(GetLaptopDetailUseCase::class.java)
    private val recommendLaptopsUseCase = Mockito.mock(RecommendLaptopsUseCase::class.java)
    private val manageCommentUseCase = Mockito.mock(ManageCommentUseCase::class.java)
    private val controller = PageController(
        getLaptopDetailUseCase = getLaptopDetailUseCase,
        recommendLaptopsUseCase = recommendLaptopsUseCase,
        manageCommentUseCase = manageCommentUseCase,
    )

    @Test
    fun `recommendation form keeps expected model attributes`() {
        val model = ExtendedModelMap()

        val viewName = controller.showRecommendationForm(model)

        assertThat(viewName).isEqualTo("recommendation-form")
        assertThat(model["laptopRecommendationRequest"]).isInstanceOf(LaptopRecommendationRequest::class.java)
        assertThat(model["screenSizeList"]).isEqualTo(LaptopRecommendationRequest.ALL_SELECTABLE_SCREEN_SIZES)
        assertThat(model["budgetPresetList"]).isNotNull
        assertThat(model["weightPresetList"]).isNotNull
    }

    @Test
    fun `recommendation result delegates to service and keeps list model attributes`() {
        val request = LaptopRecommendationRequest.fixture()
        val query = request.toQuery()
        val pageQuery = pageQueryFrom(page = 0, size = 10, sort = listOf("price,asc"))
        val recommendedLaptop = LaptopRecommendationResult(
            id = 1L,
            score = 91.2,
            imgLink = "https://example.com/laptop.jpg",
            price = 1_490_000,
            name = "테스트 노트북",
            manufacturer = "테스트",
            weight = 1.2,
            screenSize = 14,
            cpu = "Core Ultra",
            gpu = "Intel Graphics",
            resolutionLabel = "FHD",
            reasons = listOf("문서 작업에 잘 맞아요"),
        )
        Mockito.`when`(recommendLaptopsUseCase.recommend(query, pageQuery))
            .thenReturn(
                PagedResult(
                    content = listOf(recommendedLaptop),
                    page = 0,
                    size = 10,
                    totalElements = 1,
                ),
        )
        val model = ExtendedModelMap()

        val viewName = controller.recommendLaptops(
            laptopRecommendationRequest = request,
            page = 0,
            size = 10,
            sort = listOf("price,asc"),
            model = model,
        )

        assertThat(viewName).isEqualTo("recommendation-list")
        assertThat(model["recommendedLaptops"]).isEqualTo(listOf(LaptopRecommendationListResponse.from(recommendedLaptop)))
        assertThat(model["totalCount"]).isEqualTo(1L)
        assertThat(model["currentSort"]).isEqualTo("price,asc")
    }

    @Test
    fun `laptop detail delegates to services and keeps model attributes`() {
        val laptopDetail = laptopDetailResponse(id = 10L)
        val comments = listOf(CommentResult(id = 1L, author = "iggy", content = "좋아요"))
        Mockito.`when`(getLaptopDetailUseCase.get(10L)).thenReturn(laptopDetail)
        Mockito.`when`(manageCommentUseCase.listByLaptop(10L)).thenReturn(comments)
        val model = ExtendedModelMap()

        val viewName = controller.showLaptopDetail(10L, model)

        assertThat(viewName).isEqualTo("laptop-detail")
        assertThat(model["laptopDetail"]).isEqualTo(LaptopDetailResponse.from(laptopDetail))
        assertThat(model["commentsOfLaptop"]).isEqualTo(comments.map(CommentResponse::from))
        assertThat(model["commentRequest"]).isEqualTo(CommentRequest())
    }

    @Test
    fun `comment create redirects to laptop detail after service call`() {
        val request = CommentRequest(laptopId = 3L, author = "iggy", content = "좋아요", passWord = "pw")

        val viewName = controller.addComment(request)

        assertThat(viewName).isEqualTo("redirect:/laptops/3")
        Mockito.verify(manageCommentUseCase).add(
            AddCommentCommand(
                laptopId = 3L,
                author = "iggy",
                content = "좋아요",
                password = "pw",
            ),
        )
    }

    @Test
    fun `comment edit redirects to laptop detail after service call`() {
        val request = CommentRequest(laptopId = 3L, author = "iggy", content = "수정", passWord = "pw")

        val viewName = controller.editComment(7L, request)

        assertThat(viewName).isEqualTo("redirect:/laptops/3")
        Mockito.verify(manageCommentUseCase).update(
            7L,
            UpdateCommentCommand(password = "pw", content = "수정"),
        )
    }

    private fun laptopDetailResponse(id: Long): LaptopDetailResult {
        return LaptopDetailResult(
            id = id,
            name = "테스트 노트북",
            imageUrl = "https://example.com/laptop.jpg",
            manufacturer = "테스트",
            detailPage = "https://prod.danawa.com/info/?pcode=1&cate=112758",
            price = 1_490_000,
            cpuManufacturer = "인텔",
            cpu = "Core Ultra",
            os = "윈도우11홈",
            screenSize = 14,
            resolution = "1920x1200",
            brightness = 300,
            refreshRate = 60,
            ramSize = 16,
            ramType = "LPDDR5X",
            isRamReplaceable = false,
            graphicsType = "Intel Graphics",
            tgp = 0,
            thunderboltCount = 1,
            usbCCount = 2,
            usbACount = 1,
            sdCard = null,
            isSupportsPdCharging = true,
            batteryCapacity = 60.0,
            storageCapacity = 512,
            storageSlotCount = 1,
            weight = 1.2,
            usage = listOf("사무/인강용"),
        )
    }
}
