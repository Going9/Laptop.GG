package going9.laptopgg.dto.request

class RecommendRequest(
    val budge: Int,
    val weight: Int,
    val isTenKey: Boolean,
    val displaySize: Double,
    val isForeignLaptop: Boolean,
    val isDGpu: Boolean?,  // 외장 필요한면 true, 필요없으면 false, 모르면 null
    val purpose: PurposeDetail,
) {
}

sealed class PurposeDetail {
    data class Gaming(val gamingPurpose: GamingPurpose) : PurposeDetail()
    data class Work(val workPurpose: WorkPurpose) : PurposeDetail()
    data class VideoEditing(val videoEditingPurpose: VideoEditingPurpose) : PurposeDetail()
}

enum class GamingPurpose {
    OCCASIONAL_LOL,  // 가끔 롤
    LOW_SPEC,        // 저사양: 던메롤
    ONLINE_GAME,     // 중사양 온라인: 로아
    AAA_GAME         // 스팀 겜
}

enum class WorkPurpose {
    GENERAL_OFFICE,  // 일반  사무
    CODING_WEP_APP,  // 웹, 앱 개발
    CODING_AI        // AI 관련 개발
}

enum class VideoEditingPurpose {
    FHD_EDITING,     // FHD 편집
    UHD_EDITING      // 4k 편집
}