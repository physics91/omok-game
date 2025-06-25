package com.omok.infrastructure

import com.omok.application.service.GameApplicationService
import com.omok.application.usecase.*
import com.omok.domain.event.GameEventBus
import com.omok.domain.model.AIDifficulty
import com.omok.domain.service.AIStrategy
import com.omok.domain.service.GameEngine
import com.omok.domain.service.RuleValidator
import com.omok.infrastructure.ai.MinimaxAI
import com.omok.infrastructure.ai.AdvancedAI
import com.omok.domain.model.GameRule
import com.omok.domain.service.rule.RuleValidatorFactory

class DependencyContainer {
    private val _eventBus = GameEventBus()
    private var _ruleValidator: RuleValidator = RuleValidator()
    private var _currentRule: GameRule = GameRule.STANDARD_RENJU
    
    val eventBus: GameEventBus get() = _eventBus
    val ruleValidator: RuleValidator get() = _ruleValidator
    
    fun createAIStrategy(difficulty: AIDifficulty): AIStrategy {
        // 어려움 난이도는 고급 AI 사용
        return if (difficulty == AIDifficulty.HARD) {
            AdvancedAI(difficulty, _ruleValidator)
        } else {
            MinimaxAI(difficulty, _ruleValidator)
        }
    }
    
    fun createGameEngine(aiStrategy: AIStrategy? = null): GameEngine {
        return GameEngine(_ruleValidator, aiStrategy)
    }
    
    fun setGameRule(rule: GameRule) {
        _currentRule = rule
        _ruleValidator = RuleValidatorFactory.create(rule)
    }
    
    fun createGameApplicationService(aiDifficulty: AIDifficulty = AIDifficulty.MEDIUM): GameApplicationService {
        val aiStrategy = createAIStrategy(aiDifficulty)
        val gameEngine = createGameEngine(aiStrategy)
        
        val startGameUseCase = StartGameUseCase(gameEngine, _eventBus)
        val makeMoveUseCase = MakeMoveUseCase(gameEngine, _eventBus)
        val undoMoveUseCase = UndoMoveUseCase(gameEngine, _eventBus)
        val processAIMoveUseCase = ProcessAIMoveUseCase(gameEngine, _eventBus)
        
        return GameApplicationService(
            startGameUseCase,
            makeMoveUseCase,
            undoMoveUseCase,
            processAIMoveUseCase,
            gameEngine
        )
    }
}