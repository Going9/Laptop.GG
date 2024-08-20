package going9.laptopgg.dto.request

class RamRequest(
    val capacity: Int,
    val slot: Int? = null,  // RAM 슬롯 수, 업그레이드 가능 시 슬롯 수 입력 불가능하면 null
    val clockSpeed: Int,  // 램 클럭 스피드
    val ddrType: Int, // drr4 or 5 or lpddr
) {
}