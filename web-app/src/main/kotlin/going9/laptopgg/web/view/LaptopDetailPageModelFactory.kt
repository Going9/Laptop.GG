package going9.laptopgg.web.view

import going9.laptopgg.application.laptop.LaptopDetailPageResult
import going9.laptopgg.web.dto.request.CommentRequest
import going9.laptopgg.web.dto.response.CommentResponse
import going9.laptopgg.web.dto.response.LaptopDetailResponse
import org.springframework.stereotype.Component

@Component
internal class LaptopDetailPageModelFactory {
    fun create(laptopId: Long, pageResult: LaptopDetailPageResult): LaptopDetailPageModel {
        return LaptopDetailPageModel(
            laptopDetail = LaptopDetailResponse.from(pageResult.laptopDetail),
            comments = pageResult.comments.map(CommentResponse::from),
            commentRequest = CommentRequest(laptopId = laptopId),
        )
    }
}

internal data class LaptopDetailPageModel(
    val laptopDetail: LaptopDetailResponse,
    val comments: List<CommentResponse>,
    val commentRequest: CommentRequest,
)
