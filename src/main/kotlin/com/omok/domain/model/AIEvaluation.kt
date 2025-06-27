package com.omok.domain.model

/**
 * AI 평가 정보
 */
data class AIEvaluation(
    val position: Position,
    val score: Int,
    val depth: Int,
    val isCandidate: Boolean = true,
    val reason: String? = null // 예: "공격", "방어", "필승수" 등
)

/**
 * AI 사고 과정 정보
 */
data class AIThinkingInfo(
    val evaluations: List<AIEvaluation>,
    val currentBestMove: Position?,
    val thinkingProgress: Float, // 0.0 ~ 1.0
    val nodesEvaluated: Int,
    val currentDepth: Int
)