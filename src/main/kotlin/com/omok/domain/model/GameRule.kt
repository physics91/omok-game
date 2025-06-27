package com.omok.domain.model

/**
 * 오목 게임 규칙 종류
 */
enum class GameRule(
    val displayName: String,
    val description: String
) {
    STANDARD_RENJU(
        "표준 렌주룰",
        "흑돌 금수: 3-3, 4-4, 장목(6목 이상). 백돌은 제한 없음"
    ),
    
    OPEN_RENJU(
        "오픈 렌주룰",
        "1수: 천원, 2수: 8곳, 3수: 26점, 스왑 가능, 5수: 흑이 2개 제시 백이 선택"
    ),
    
    YAMAGUCHI_RULE(
        "야마구치룰",
        "흑이 첫 3수를 놓고, 백이 흑백 선택권을 가짐. 이후 표준 렌주룰 적용"
    ),
    
    SWAP_RULE(
        "스왑룰",
        "흑이 첫 수를 두면 백이 색을 바꿀 수 있음. 이후 표준 렌주룰 적용"
    ),
    
    SWAP2_RULE(
        "스왑2룰",
        "흑이 첫 3수를 두고, 백이 색 선택 또는 백4수 두기 선택. 국제 대회 표준"
    ),
    
    SOOSYRV_RULE(
        "수시르브룰",
        "흑이 8가지 정해진 오프닝 중 하나를 선택하여 시작"
    ),
    
    TARAGUCHI_RULE(
        "타라구치룰",
        "흑이 10개의 정해진 오프닝 중 하나로 시작. 백이 4수를 둔 후 5수 위치 제안"
    ),
    
    FREESTYLE(
        "자유룰",
        "아무 제한 없음. 먼저 5개를 만드는 사람이 승리"
    ),
    
    CARO_RULE(
        "카로룰",
        "6목 이상도 승리로 인정. 3-3, 4-4는 금수가 아님"
    );
    
    /**
     * 이 규칙에서 흑돌에 금수가 적용되는지 여부
     */
    fun hasBlackRestrictions(): Boolean {
        return this != FREESTYLE && this != CARO_RULE
    }
    
    /**
     * 이 규칙에서 6목 이상이 승리인지 여부
     */
    fun allowsOverline(): Boolean {
        return this == FREESTYLE || this == CARO_RULE
    }
    
    /**
     * 오프닝 규정이 있는지 여부
     */
    fun hasOpeningRestrictions(): Boolean {
        return when (this) {
            OPEN_RENJU, YAMAGUCHI_RULE, SWAP_RULE, SWAP2_RULE, 
            SOOSYRV_RULE, TARAGUCHI_RULE -> true
            else -> false
        }
    }
}