package going9.laptopgg.dto.request

data class RamRequest(
    var capacity: Int = 0,
    var slot: Int? = null,  // RAM 슬롯 수, 업그레이드 가능 시 슬롯 수 입력 불가능하면 null
    var clockSpeed: Int = 0,  // 램 클럭 스피드
    var ddrType: Int = 0, // drr4 or 5 or lpddr
) {
}