package com.omok.infrastructure

import com.omok.application.service.GameApplicationService
import com.omok.application.usecase.*
import com.omok.domain.event.GameEventBus
import com.omok.domain.model.AIDifficulty
import com.omok.domain.service.AIStrategy
import com.omok.domain.service.GameEngine
import com.omok.domain.service.RuleValidator
import com.omok.infrastructure.ai.MinimaxAI
import com.omok.infrastructure.ai.EnhancedAI
import com.omok.domain.model.GameRule
import com.omok.domain.service.rule.RuleValidatorFactory
import com.omok.domain.achievement.AchievementManager
import com.omok.domain.achievement.AchievementRepository
import com.omok.infrastructure.achievement.FileAchievementRepository
import com.omok.domain.logging.DomainLogger
import com.omok.infrastructure.logging.DomainLoggerImpl
import com.omok.infrastructure.logging.Logger

class DependencyContainer {
    private var _ruleValidator: RuleValidator = RuleValidator()
    private var _currentRule: GameRule = GameRule.STANDARD_RENJU
    
    // 성취도 시스템 초기화
    private val achievementRepository: AchievementRepository = FileAchievementRepository()
    private val _achievementManager: AchievementManager by lazy {
        AchievementManager(eventBus, achievementRepository)
    }
    
    val eventBus: GameEventBus get() = GameEventBus
    val ruleValidator: RuleValidator get() = _ruleValidator
    val achievementManager: AchievementManager get() = _achievementManager
    
    // 로거 인스턴스
    private val domainLogger: DomainLogger = DomainLoggerImpl(Logger)
    
    fun createAIStrategy(difficulty: AIDifficulty): AIStrategy {
        // 중간 이상 난이도는 향상된 AI 사용
        return if (difficulty == AIDifficulty.MEDIUM || difficulty == AIDifficulty.HARD) {
            EnhancedAI(difficulty, _ruleValidator)
        } else {
            MinimaxAI(difficulty, _ruleValidator)
        }
    }
    
    fun createGameEngine(aiStrategy: AIStrategy? = null): GameEngine {
        return GameEngine(_ruleValidator, aiStrategy, domainLogger)
    }
    
    fun setGameRule(rule: GameRule) {
        _currentRule = rule
        _ruleValidator = RuleValidatorFactory.create(rule)
    }
    
    fun createGameApplicationService(aiDifficulty: AIDifficulty = AIDifficulty.MEDIUM): GameApplicationService {
        val aiStrategy = createAIStrategy(aiDifficulty)
        val gameEngine = createGameEngine(aiStrategy)
        
        val startGameUseCase = StartGameUseCase(gameEngine)
        val makeMoveUseCase = MakeMoveUseCase(gameEngine)
        val undoMoveUseCase = UndoMoveUseCase(gameEngine)
        val processAIMoveUseCase = ProcessAIMoveUseCase(gameEngine)
        val processAchievementEventUseCase = ProcessAchievementEventUseCase(achievementManager)
        
        return GameApplicationService(
            startGameUseCase,
            makeMoveUseCase,
            undoMoveUseCase,
            processAIMoveUseCase,
            gameEngine
        )
    }
    
    fun createProcessAchievementEventUseCase(): ProcessAchievementEventUseCase {
        return ProcessAchievementEventUseCase(achievementManager)
    }
}