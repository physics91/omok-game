package com.omok.application.dto

/**
 * AI 평가 정보 DTO
 */
data class AIEvaluationDto(
    val position: PositionDto,
    val score: Int,
    val depth: Int,
    val isCandidate: Boolean = true,
    val reason: String? = null
)

/**
 * AI 사고 과정 정보 DTO
 */
data class AIThinkingInfoDto(
    val evaluations: List<AIEvaluationDto>,
    val currentBestMove: PositionDto?,
    val thinkingProgress: Float,
    val nodesEvaluated: Int,
    val currentDepth: Int
)