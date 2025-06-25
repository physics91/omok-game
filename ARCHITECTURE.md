# 오목 게임 프로젝트 아키텍처 문서

## 목차
1. [아키텍처 개요](#아키텍처-개요)
2. [시스템 아키텍처](#시스템-아키텍처)
3. [계층별 상세 설계](#계층별-상세-설계)
4. [데이터 플로우](#데이터-플로우)
5. [주요 설계 결정사항](#주요-설계-결정사항)
6. [확장 전략](#확장-전략)
7. [성능 고려사항](#성능-고려사항)
8. [보안 및 안정성](#보안-및-안정성)

## 아키텍처 개요

### 프로젝트 목표
렌주룰 기반 오목 게임을 Clean Architecture와 Hexagonal Architecture 패턴을 적용하여 확장 가능하고 유지보수가 용이한 데스크톱 애플리케이션으로 구현

### 아키텍처 원칙
- **관심사의 분리 (Separation of Concerns)**: 각 계층이 명확한 책임을 가짐
- **의존성 역전 (Dependency Inversion)**: 추상화에 의존하여 유연성 확보
- **단일 책임 원칙 (Single Responsibility)**: 각 모듈은 하나의 책임만 가짐
- **개방-폐쇄 원칙 (Open-Closed)**: 확장에는 열려있고 수정에는 닫혀있음
- **인터페이스 분리 (Interface Segregation)**: 클라이언트별 특화된 인터페이스 제공

### 핵심 패턴
- **Clean Architecture**: 비즈니스 로직과 기술적 세부사항의 분리
- **Hexagonal Architecture**: 포트와 어댑터를 통한 외부 시스템 연동
- **Event-Driven Architecture**: 이벤트 기반 통신으로 느슨한 결합
- **Domain-Driven Design**: 도메인 중심의 모델링

## 시스템 아키텍처

### 계층 구조도

```
┌─────────────────────────────────────────────────────────────┐
│                    Presentation Layer                        │
│  ┌─────────────┐  ┌──────────────┐  ┌──────────────────┐  │
│  │   GameUI    │  │  Controller  │  │  Event Handlers  │  │
│  └─────────────┘  └──────────────┘  └──────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                    Application Layer                         │
│  ┌──────────────┐  ┌──────────────┐  ┌─────────────────┐  │
│  │  Use Cases   │  │  App Service │  │  Event Publisher│  │
│  └──────────────┘  └──────────────┘  └─────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                      Domain Layer                            │
│  ┌─────────────┐  ┌──────────────┐  ┌─────────────────┐   │
│  │   Entities  │  │ Domain Service│  │   Value Objects │   │
│  │  (Game,Board)│  │(GameEngine)  │  │ (Move,Position) │   │
│  └─────────────┘  └──────────────┘  └─────────────────┘   │
│  ┌──────────────────────────────────────────────────────┐  │
│  │              Domain Events & Interfaces               │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                               ▲
┌─────────────────────────────────────────────────────────────┐
│                  Infrastructure Layer                        │
│  ┌──────────────┐  ┌──────────────┐  ┌─────────────────┐  │
│  │  AI Engines  │  │ Persistence  │  │   Event Bus     │  │
│  └──────────────┘  └──────────────┘  └─────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

### 패키지 구조

```
com.omok/
├── domain/                    # 핵심 비즈니스 로직
│   ├── model/
│   │   ├── game/
│   │   │   ├── Game.kt       # 게임 애그리거트 루트
│   │   │   ├── GameId.kt     # 게임 식별자 VO
│   │   │   └── GameState.kt  # 게임 상태 열거형
│   │   ├── board/
│   │   │   ├── Board.kt      # 불변 보드 엔티티
│   │   │   ├── Position.kt   # 위치 값 객체
│   │   │   └── Stone.kt      # 돌 열거형
│   │   ├── player/
│   │   │   ├── Player.kt     # 플레이어 열거형
│   │   │   └── PlayerType.kt # 플레이어 타입
│   │   └── move/
│   │       ├── Move.kt       # 수 값 객체
│   │       └── MoveHistory.kt # 수 히스토리
│   ├── service/
│   │   ├── GameEngine.kt     # 게임 로직 도메인 서비스
│   │   ├── RuleValidator.kt  # 규칙 검증 인터페이스
│   │   ├── WinConditionChecker.kt # 승리 조건 체크
│   │   └── AIStrategy.kt     # AI 전략 인터페이스
│   ├── event/
│   │   ├── DomainEvent.kt    # 도메인 이벤트 기본
│   │   ├── GameEvent.kt      # 게임 이벤트
│   │   └── EventPublisher.kt # 이벤트 발행 인터페이스
│   └── exception/
│       ├── DomainException.kt # 도메인 예외
│       └── GameException.kt   # 게임 관련 예외
│
├── application/               # 애플리케이션 비즈니스 규칙
│   ├── usecase/
│   │   ├── game/
│   │   │   ├── StartGameUseCase.kt
│   │   │   ├── MakeMoveUseCase.kt
│   │   │   ├── UndoMoveUseCase.kt
│   │   │   └── ResignGameUseCase.kt
│   │   ├── ai/
│   │   │   └── ProcessAIMoveUseCase.kt
│   │   └── replay/
│   │       ├── SaveGameUseCase.kt
│   │       └── LoadGameUseCase.kt
│   ├── service/
│   │   ├── GameApplicationService.kt
│   │   └── GameStateManager.kt
│   └── port/                 # 헥사고날 아키텍처 포트
│       ├── in/
│       │   └── GameUseCase.kt
│       └── out/
│           ├── GameRepository.kt
│           └── EventPublisher.kt
│
├── infrastructure/           # 프레임워크와 드라이버
│   ├── ai/
│   │   ├── MinimaxAI.kt     # Minimax 알고리즘 구현
│   │   ├── AlphaBetaAI.kt   # Alpha-Beta 가지치기
│   │   └── MonteCarloAI.kt  # MCTS 구현
│   ├── persistence/
│   │   ├── InMemoryGameRepository.kt
│   │   └── FileGameRepository.kt
│   ├── event/
│   │   ├── SimpleEventBus.kt # 이벤트 버스 구현
│   │   └── AsyncEventBus.kt  # 비동기 이벤트 버스
│   ├── rule/
│   │   ├── RenjuRuleValidator.kt # 렌주룰 구현
│   │   └── StandardRuleValidator.kt
│   └── config/
│       └── DependencyContainer.kt # DI 컨테이너
│
└── presentation/            # 사용자 인터페이스
    ├── ui/
    │   ├── window/
    │   │   └── GameWindow.kt # 메인 윈도우
    │   ├── panel/
    │   │   ├── GameBoardPanel.kt # 게임 보드
    │   │   ├── ControlPanel.kt   # 컨트롤 패널
    │   │   └── StatusPanel.kt    # 상태 표시
    │   └── dialog/
    │       ├── SettingsDialog.kt
    │       └── GameOverDialog.kt
    ├── controller/
    │   ├── GameController.kt  # MVC 컨트롤러
    │   └── UIEventHandler.kt  # UI 이벤트 처리
    ├── model/
    │   └── GameViewModel.kt   # 뷰 모델
    └── Main.kt               # 진입점
```

## 계층별 상세 설계

### 1. Domain Layer (도메인 계층)

#### 핵심 엔티티

```kotlin
// Game.kt - 애그리거트 루트
class Game private constructor(
    val id: GameId,
    private var board: Board,
    private var currentPlayer: Player,
    private var state: GameState,
    private val moveHistory: MoveHistory,
    private val settings: GameSettings
) {
    companion object {
        fun create(settings: GameSettings): Game {
            return Game(
                id = GameId.generate(),
                board = Board.empty(),
                currentPlayer = Player.BLACK,
                state = GameState.PLAYING,
                moveHistory = MoveHistory.empty(),
                settings = settings
            )
        }
    }
    
    fun makeMove(position: Position): MoveResult {
        // 불변성을 유지하면서 상태 변경
        val newBoard = board.placeStone(position, currentPlayer)
        val move = Move(position, currentPlayer, Clock.now())
        
        return MoveResult(
            game = copy(
                board = newBoard,
                moveHistory = moveHistory.add(move),
                currentPlayer = currentPlayer.opposite()
            ),
            move = move,
            events = listOf(MoveMadeEvent(id, move))
        )
    }
}

// Board.kt - 불변 보드
class Board private constructor(
    private val stones: Array<Array<Player?>>
) {
    fun placeStone(position: Position, player: Player): Board {
        require(isValidPosition(position)) { "Invalid position" }
        require(isEmpty(position)) { "Position already occupied" }
        
        val newStones = stones.deepCopy()
        newStones[position.row][position.col] = player
        return Board(newStones)
    }
    
    fun getStone(position: Position): Player? = 
        stones[position.row][position.col]
}
```

#### 도메인 서비스

```kotlin
// GameEngine.kt - 게임 로직 엔진
class GameEngine(
    private val ruleValidator: RuleValidator,
    private val winChecker: WinConditionChecker
) {
    fun processMove(game: Game, position: Position): ProcessResult {
        // 규칙 검증
        val validation = ruleValidator.validate(game, position)
        if (!validation.isValid) {
            return ProcessResult.Invalid(validation.reason)
        }
        
        // 수 처리
        val moveResult = game.makeMove(position)
        
        // 승리 조건 체크
        val winCheck = winChecker.check(moveResult.game)
        
        return ProcessResult.Success(
            game = moveResult.game,
            gameOver = winCheck.isGameOver,
            winner = winCheck.winner
        )
    }
}

// RuleValidator.kt - 규칙 검증 인터페이스
interface RuleValidator {
    fun validate(game: Game, position: Position): ValidationResult
}

// AIStrategy.kt - AI 전략 인터페이스
interface AIStrategy {
    suspend fun calculateBestMove(
        board: Board, 
        player: Player,
        timeLimit: Duration
    ): Position?
}
```

#### 도메인 이벤트

```kotlin
// DomainEvent.kt
sealed class DomainEvent {
    abstract val aggregateId: GameId
    abstract val occurredAt: Instant
}

// GameEvent.kt
sealed class GameEvent : DomainEvent() {
    data class GameStarted(
        override val aggregateId: GameId,
        val settings: GameSettings,
        override val occurredAt: Instant = Clock.now()
    ) : GameEvent()
    
    data class MoveMade(
        override val aggregateId: GameId,
        val move: Move,
        override val occurredAt: Instant = Clock.now()
    ) : GameEvent()
    
    data class GameEnded(
        override val aggregateId: GameId,
        val winner: Player?,
        val reason: EndReason,
        override val occurredAt: Instant = Clock.now()
    ) : GameEvent()
}
```

### 2. Application Layer (애플리케이션 계층)

#### 유스케이스

```kotlin
// MakeMoveUseCase.kt
class MakeMoveUseCase(
    private val gameRepository: GameRepository,
    private val gameEngine: GameEngine,
    private val eventPublisher: EventPublisher
) {
    suspend fun execute(gameId: GameId, position: Position): MakeMoveResult {
        return coroutineScope {
            // 게임 조회
            val game = gameRepository.findById(gameId)
                ?: throw GameNotFoundException(gameId)
            
            // 수 처리
            val result = gameEngine.processMove(game, position)
            
            when (result) {
                is ProcessResult.Success -> {
                    // 게임 저장
                    gameRepository.save(result.game)
                    
                    // 이벤트 발행
                    result.game.domainEvents.forEach { event ->
                        eventPublisher.publish(event)
                    }
                    
                    MakeMoveResult.Success(result.game)
                }
                is ProcessResult.Invalid -> {
                    MakeMoveResult.Invalid(result.reason)
                }
            }
        }
    }
}

// ProcessAIMoveUseCase.kt
class ProcessAIMoveUseCase(
    private val gameRepository: GameRepository,
    private val aiStrategy: AIStrategy,
    private val makeMoveUseCase: MakeMoveUseCase
) {
    suspend fun execute(
        gameId: GameId,
        timeLimit: Duration = 5.seconds
    ): ProcessAIResult = withContext(Dispatchers.Default) {
        val game = gameRepository.findById(gameId)
            ?: throw GameNotFoundException(gameId)
        
        // AI 계산
        val position = aiStrategy.calculateBestMove(
            game.board,
            game.currentPlayer,
            timeLimit
        ) ?: return@withContext ProcessAIResult.NoMoveAvailable
        
        // 수 실행
        val moveResult = makeMoveUseCase.execute(gameId, position)
        
        ProcessAIResult.MoveMade(position, moveResult)
    }
}
```

#### 애플리케이션 서비스

```kotlin
// GameApplicationService.kt
class GameApplicationService(
    private val startGameUseCase: StartGameUseCase,
    private val makeMoveUseCase: MakeMoveUseCase,
    private val processAIMoveUseCase: ProcessAIMoveUseCase,
    private val undoMoveUseCase: UndoMoveUseCase
) {
    suspend fun startNewGame(settings: GameSettings): Game {
        return startGameUseCase.execute(settings)
    }
    
    suspend fun makeMove(gameId: GameId, position: Position): MakeMoveResult {
        return makeMoveUseCase.execute(gameId, position)
    }
    
    suspend fun processAIMove(gameId: GameId): ProcessAIResult {
        return processAIMoveUseCase.execute(gameId)
    }
    
    suspend fun undoLastMove(gameId: GameId): UndoResult {
        return undoMoveUseCase.execute(gameId)
    }
}
```

### 3. Infrastructure Layer (인프라 계층)

#### AI 구현

```kotlin
// MinimaxAI.kt
class MinimaxAI(
    private val maxDepth: Int,
    private val evaluator: BoardEvaluator
) : AIStrategy {
    
    override suspend fun calculateBestMove(
        board: Board,
        player: Player,
        timeLimit: Duration
    ): Position? = coroutineScope {
        val startTime = System.currentTimeMillis()
        
        val moves = board.getValidMoves()
        if (moves.isEmpty()) return@coroutineScope null
        
        val movesWithScores = moves.map { position ->
            async {
                val newBoard = board.placeStone(position, player)
                val score = minimax(
                    newBoard, 
                    maxDepth - 1, 
                    Int.MIN_VALUE,
                    Int.MAX_VALUE,
                    false, 
                    player.opposite(),
                    startTime,
                    timeLimit
                )
                position to score
            }
        }.awaitAll()
        
        movesWithScores.maxByOrNull { it.second }?.first
    }
    
    private fun minimax(
        board: Board,
        depth: Int,
        alpha: Int,
        beta: Int,
        isMaximizing: Boolean,
        currentPlayer: Player,
        startTime: Long,
        timeLimit: Duration
    ): Int {
        // 시간 제한 체크
        if (System.currentTimeMillis() - startTime > timeLimit.inWholeMilliseconds) {
            return evaluator.evaluate(board, currentPlayer)
        }
        
        // 종료 조건
        if (depth == 0 || board.isGameOver()) {
            return evaluator.evaluate(board, currentPlayer)
        }
        
        // Minimax with Alpha-Beta Pruning
        var localAlpha = alpha
        var localBeta = beta
        
        if (isMaximizing) {
            var maxEval = Int.MIN_VALUE
            for (move in board.getValidMoves()) {
                val newBoard = board.placeStone(move, currentPlayer)
                val eval = minimax(
                    newBoard, depth - 1, localAlpha, localBeta, 
                    false, currentPlayer.opposite(), startTime, timeLimit
                )
                maxEval = max(maxEval, eval)
                localAlpha = max(localAlpha, eval)
                if (localBeta <= localAlpha) break // Beta cutoff
            }
            return maxEval
        } else {
            var minEval = Int.MAX_VALUE
            for (move in board.getValidMoves()) {
                val newBoard = board.placeStone(move, currentPlayer)
                val eval = minimax(
                    newBoard, depth - 1, localAlpha, localBeta,
                    true, currentPlayer.opposite(), startTime, timeLimit
                )
                minEval = min(minEval, eval)
                localBeta = min(localBeta, eval)
                if (localBeta <= localAlpha) break // Alpha cutoff
            }
            return minEval
        }
    }
}
```

#### 이벤트 버스

```kotlin
// AsyncEventBus.kt
class AsyncEventBus(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) : EventPublisher, EventSubscriber {
    
    private val handlers = mutableMapOf<KClass<*>, MutableList<EventHandler<*>>>()
    private val eventChannel = Channel<DomainEvent>(Channel.UNLIMITED)
    
    init {
        scope.launch {
            for (event in eventChannel) {
                dispatchEvent(event)
            }
        }
    }
    
    override suspend fun publish(event: DomainEvent) {
        eventChannel.send(event)
    }
    
    override fun <T : DomainEvent> subscribe(
        eventType: KClass<T>,
        handler: EventHandler<T>
    ) {
        handlers.getOrPut(eventType) { mutableListOf() }.add(handler)
    }
    
    private suspend fun dispatchEvent(event: DomainEvent) {
        handlers[event::class]?.forEach { handler ->
            scope.launch {
                try {
                    @Suppress("UNCHECKED_CAST")
                    (handler as EventHandler<DomainEvent>).handle(event)
                } catch (e: Exception) {
                    // 에러 로깅
                }
            }
        }
    }
}
```

#### 의존성 주입 컨테이너

```kotlin
// DependencyContainer.kt
class DependencyContainer {
    // 싱글톤 인스턴스
    private val eventBus = AsyncEventBus()
    private val gameRepository = InMemoryGameRepository()
    
    // AI 전략 팩토리
    fun createAIStrategy(difficulty: AIDifficulty): AIStrategy {
        return when (difficulty) {
            AIDifficulty.EASY -> MinimaxAI(
                maxDepth = 2,
                evaluator = SimpleEvaluator()
            )
            AIDifficulty.MEDIUM -> MinimaxAI(
                maxDepth = 4,
                evaluator = AdvancedEvaluator()
            )
            AIDifficulty.HARD -> AlphaBetaAI(
                maxDepth = 6,
                evaluator = AdvancedEvaluator(),
                transpositionTable = TranspositionTable()
            )
        }
    }
    
    // 게임 엔진 생성
    fun createGameEngine(): GameEngine {
        return GameEngine(
            ruleValidator = RenjuRuleValidator(),
            winChecker = StandardWinChecker()
        )
    }
    
    // 애플리케이션 서비스 생성
    fun createGameApplicationService(
        aiDifficulty: AIDifficulty? = null
    ): GameApplicationService {
        val gameEngine = createGameEngine()
        val aiStrategy = aiDifficulty?.let { createAIStrategy(it) }
        
        return GameApplicationService(
            startGameUseCase = StartGameUseCase(
                gameRepository, eventBus
            ),
            makeMoveUseCase = MakeMoveUseCase(
                gameRepository, gameEngine, eventBus
            ),
            processAIMoveUseCase = aiStrategy?.let {
                ProcessAIMoveUseCase(
                    gameRepository, it,
                    MakeMoveUseCase(gameRepository, gameEngine, eventBus)
                )
            },
            undoMoveUseCase = UndoMoveUseCase(
                gameRepository, eventBus
            )
        )
    }
}
```

### 4. Presentation Layer (프레젠테이션 계층)

#### MVC 패턴 구현

```kotlin
// GameController.kt
class GameController(
    private val applicationService: GameApplicationService,
    private val eventBus: EventSubscriber,
    private val scope: CoroutineScope
) {
    private lateinit var view: GameView
    private lateinit var model: GameViewModel
    
    init {
        subscribeToEvents()
    }
    
    fun initialize(view: GameView, settings: GameSettings) {
        this.view = view
        
        scope.launch {
            val game = applicationService.startNewGame(settings)
            model = GameViewModel(game)
            view.updateDisplay(model)
        }
    }
    
    fun handleCellClick(row: Int, col: Int) {
        scope.launch {
            val position = Position(row, col)
            val result = applicationService.makeMove(
                model.gameId, 
                position
            )
            
            when (result) {
                is MakeMoveResult.Success -> {
                    model.update(result.game)
                    view.updateDisplay(model)
                    
                    // AI 차례인 경우 AI 수 처리
                    if (shouldProcessAI()) {
                        processAIMove()
                    }
                }
                is MakeMoveResult.Invalid -> {
                    view.showMessage(result.reason)
                }
            }
        }
    }
    
    private fun subscribeToEvents() {
        eventBus.subscribe(MoveMadeEvent::class) { event ->
            withContext(Dispatchers.Main) {
                view.highlightLastMove(event.move.position)
                view.playSound(SoundEffect.MOVE)
            }
        }
        
        eventBus.subscribe(GameEndedEvent::class) { event ->
            withContext(Dispatchers.Main) {
                view.showGameOverDialog(event.winner, event.reason)
            }
        }
    }
}

// GameViewModel.kt - MVVM 패턴 지원
class GameViewModel(
    private var game: Game
) {
    val gameId: GameId get() = game.id
    val board: BoardViewModel = BoardViewModel(game.board)
    val currentPlayer: Player get() = game.currentPlayer
    val gameState: GameState get() = game.state
    val moveHistory: List<MoveViewModel> = game.moveHistory.toViewModels()
    
    fun update(newGame: Game) {
        game = newGame
        board.update(game.board)
        notifyObservers()
    }
}
```

#### UI 컴포넌트

```kotlin
// GameBoardPanel.kt
class GameBoardPanel(
    private val controller: GameController
) : JPanel() {
    private val cellSize = 40
    private val boardSize = 15
    private var lastMove: Position? = null
    private var boardImage: BufferedImage? = null
    
    init {
        preferredSize = Dimension(
            cellSize * boardSize,
            cellSize * boardSize
        )
        background = Color(220, 179, 92) // 나무색
        
        addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                handleClick(e.x, e.y)
            }
        })
    }
    
    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val g2d = g as Graphics2D
        
        // 안티앨리어싱 설정
        g2d.setRenderingHint(
            RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON
        )
        
        drawBoard(g2d)
        drawStones(g2d)
        highlightLastMove(g2d)
    }
    
    private fun drawBoard(g: Graphics2D) {
        g.color = Color.BLACK
        g.stroke = BasicStroke(1f)
        
        // 격자 그리기
        for (i in 0 until boardSize) {
            val coord = i * cellSize + cellSize / 2
            // 가로선
            g.drawLine(cellSize / 2, coord, 
                      width - cellSize / 2, coord)
            // 세로선
            g.drawLine(coord, cellSize / 2,
                      coord, height - cellSize / 2)
        }
        
        // 화점 그리기
        val dotPositions = listOf(3, 7, 11)
        g.fillOval(...)
    }
}
```

## 데이터 플로우

### 1. 사용자 입력 처리 플로우

```
User Click → UI Event → Controller → Use Case → Domain Service → Domain Model
    ↓                                                                    ↓
UI Update ← View Model ← Event Handler ← Event Bus ← Domain Event ←────┘
```

### 2. AI 처리 플로우

```
Game State → AI Use Case → AI Strategy → Board Evaluation
                ↓                              ↓
            Position ←─── Best Move ←─── Minimax Tree
                ↓
         Make Move Use Case → Domain → Event → UI Update
```

### 3. 이벤트 기반 통신

```kotlin
// 이벤트 발행
MoveMadeEvent(gameId, move) → EventBus
                                  ↓
                          ┌───────┴────────┐
                          ↓                ↓
                    UI Handler      Analytics Handler
                          ↓                ↓
                   Update View      Log Move
```

## 주요 설계 결정사항

### 1. 불변성 (Immutability)

**결정**: 도메인 모델을 불변 객체로 설계

**이유**:
- 스레드 안전성 보장
- 상태 변경 추적 용이
- 디버깅 및 테스트 편의성
- 함수형 프로그래밍 패러다임 적용

**구현**:
```kotlin
// 불변 Board 구현
class Board private constructor(
    private val stones: Array<Array<Player?>>
) {
    fun placeStone(position: Position, player: Player): Board {
        val newStones = stones.deepCopy()
        newStones[position.row][position.col] = player
        return Board(newStones) // 새로운 인스턴스 반환
    }
}
```

### 2. 이벤트 소싱 (Event Sourcing) 준비

**결정**: 도메인 이벤트 시스템 구축

**이유**:
- 게임 히스토리 완벽 추적
- 리플레이 기능 구현 가능
- 감사(Audit) 로그 자동 생성
- CQRS 패턴 적용 가능

**구현**:
```kotlin
class Game {
    private val uncommittedEvents = mutableListOf<DomainEvent>()
    
    fun makeMove(position: Position): MoveResult {
        // 비즈니스 로직
        val event = MoveMadeEvent(id, Move(position, currentPlayer))
        uncommittedEvents.add(event)
        return MoveResult(...)
    }
    
    fun markEventsAsCommitted() {
        uncommittedEvents.clear()
    }
}
```

### 3. 코루틴 기반 비동기 처리

**결정**: Kotlin Coroutines 전면 도입

**이유**:
- UI 반응성 향상
- AI 계산 중 UI 블로킹 방지
- 구조화된 동시성
- 취소 가능한 작업

**구현**:
```kotlin
class ProcessAIMoveUseCase {
    suspend fun execute(gameId: GameId): ProcessAIResult = 
        withContext(Dispatchers.Default) {
            // CPU 집약적 AI 계산
            val position = aiStrategy.calculateBestMove(...)
            
            withContext(Dispatchers.Main) {
                // UI 업데이트
                makeMoveUseCase.execute(gameId, position)
            }
        }
}
```

### 4. 전략 패턴 (Strategy Pattern)

**결정**: AI와 규칙 검증을 인터페이스로 추상화

**이유**:
- 다양한 AI 난이도 지원
- 규칙 변경 용이
- 테스트 더블 사용 가능
- 개방-폐쇄 원칙 준수

**구현**:
```kotlin
interface AIStrategy {
    suspend fun calculateBestMove(
        board: Board,
        player: Player,
        timeLimit: Duration
    ): Position?
}

// 다양한 구현체
class MinimaxAI : AIStrategy { ... }
class MonteCarloAI : AIStrategy { ... }
class NeuralNetworkAI : AIStrategy { ... }
```

## 확장 전략

### 1. 온라인 멀티플레이어 지원

```kotlin
// 새로운 인프라 계층 추가
interface GameNetworkService {
    suspend fun joinGame(roomId: String): NetworkGame
    suspend fun sendMove(move: Move)
    fun subscribeToOpponentMoves(): Flow<Move>
}

// 새로운 유스케이스
class JoinOnlineGameUseCase(
    private val networkService: GameNetworkService,
    private val gameRepository: GameRepository
) {
    suspend fun execute(roomId: String): JoinResult {
        val networkGame = networkService.joinGame(roomId)
        val game = Game.fromNetwork(networkGame)
        gameRepository.save(game)
        return JoinResult.Success(game)
    }
}
```

### 2. AI 학습 시스템

```kotlin
// 머신러닝 기반 AI 추가
class NeuralNetworkAI(
    private val model: OmokNeuralNetwork
) : AIStrategy {
    override suspend fun calculateBestMove(
        board: Board,
        player: Player,
        timeLimit: Duration
    ): Position? {
        val prediction = model.predict(board.toTensor())
        return prediction.toBestPosition()
    }
}

// 학습 데이터 수집
class GameDataCollector : EventHandler<GameEndedEvent> {
    override suspend fun handle(event: GameEndedEvent) {
        val gameData = extractGameData(event.gameId)
        trainingDataRepository.save(gameData)
    }
}
```

### 3. 플러그인 아키텍처

```kotlin
// 플러그인 인터페이스
interface GamePlugin {
    val id: String
    val name: String
    fun initialize(context: PluginContext)
    fun onGameStart(game: Game)
    fun onMoveMade(move: Move)
}

// 플러그인 매니저
class PluginManager {
    private val plugins = mutableListOf<GamePlugin>()
    
    fun loadPlugin(plugin: GamePlugin) {
        plugins.add(plugin)
        plugin.initialize(createContext())
    }
    
    fun notifyGameStart(game: Game) {
        plugins.forEach { it.onGameStart(game) }
    }
}
```

### 4. 관전 모드

```kotlin
// 관전자 패턴
class SpectatorMode(
    private val gameId: GameId,
    private val eventBus: EventSubscriber
) {
    private val spectators = mutableListOf<SpectatorView>()
    
    init {
        eventBus.subscribe(MoveMadeEvent::class) { event ->
            if (event.aggregateId == gameId) {
                notifySpectators(event.move)
            }
        }
    }
    
    fun addSpectator(view: SpectatorView) {
        spectators.add(view)
        view.initialize(getCurrentGameState())
    }
}
```

## 성능 고려사항

### 1. AI 성능 최적화

#### 트랜스포지션 테이블
```kotlin
class TranspositionTable(
    private val maxSize: Int = 1_000_000
) {
    private val table = LRUCache<BoardHash, EvaluationEntry>(maxSize)
    
    fun store(board: Board, depth: Int, score: Int, flag: NodeType) {
        val hash = board.computeHash()
        table.put(hash, EvaluationEntry(depth, score, flag))
    }
    
    fun lookup(board: Board, depth: Int): EvaluationEntry? {
        val entry = table.get(board.computeHash())
        return if (entry != null && entry.depth >= depth) entry else null
    }
}
```

#### 병렬 처리
```kotlin
class ParallelMinimaxAI : AIStrategy {
    override suspend fun calculateBestMove(
        board: Board,
        player: Player,
        timeLimit: Duration
    ): Position? = coroutineScope {
        val moves = board.getValidMoves()
        
        // 병렬로 각 수 평가
        val evaluations = moves.map { position ->
            async(Dispatchers.Default) {
                val newBoard = board.placeStone(position, player)
                position to evaluate(newBoard, player)
            }
        }.awaitAll()
        
        evaluations.maxByOrNull { it.second }?.first
    }
}
```

### 2. 메모리 최적화

#### 보드 표현 최적화
```kotlin
// 비트보드 사용
class BitBoard(
    private val blackStones: Long,
    private val whiteStones: Long
) {
    fun placeStone(position: Position, player: Player): BitBoard {
        val bit = 1L shl (position.row * 15 + position.col)
        return when (player) {
            Player.BLACK -> BitBoard(blackStones or bit, whiteStones)
            Player.WHITE -> BitBoard(blackStones, whiteStones or bit)
        }
    }
}
```

#### 객체 풀링
```kotlin
object PositionPool {
    private val pool = Array(15) { row ->
        Array(15) { col ->
            Position(row, col)
        }
    }
    
    fun get(row: Int, col: Int): Position = pool[row][col]
}
```

### 3. UI 반응성

#### 더블 버퍼링
```kotlin
class GameBoardPanel : JPanel() {
    private var backBuffer: BufferedImage? = null
    
    override fun paintComponent(g: Graphics) {
        // 백버퍼에 그리기
        val buffer = ensureBackBuffer()
        val bg = buffer.graphics as Graphics2D
        
        drawBoard(bg)
        drawStones(bg)
        
        // 화면에 한 번에 그리기
        g.drawImage(buffer, 0, 0, null)
        bg.dispose()
    }
}
```

## 보안 및 안정성

### 1. 입력 검증

```kotlin
class PositionValidator {
    fun validate(row: Int, col: Int): ValidationResult {
        if (row !in 0 until BOARD_SIZE || col !in 0 until BOARD_SIZE) {
            return ValidationResult.Invalid("Position out of bounds")
        }
        return ValidationResult.Valid
    }
}
```

### 2. 동시성 안전성

```kotlin
class ThreadSafeGameRepository : GameRepository {
    private val games = ConcurrentHashMap<GameId, Game>()
    private val locks = ConcurrentHashMap<GameId, ReentrantReadWriteLock>()
    
    override suspend fun save(game: Game) {
        val lock = locks.computeIfAbsent(game.id) { ReentrantReadWriteLock() }
        lock.writeLock().withLock {
            games[game.id] = game
        }
    }
}
```

### 3. 에러 처리

```kotlin
class ResilientEventBus : EventPublisher {
    override suspend fun publish(event: DomainEvent) {
        try {
            internalPublish(event)
        } catch (e: Exception) {
            logger.error("Failed to publish event", e)
            // Dead Letter Queue로 전송
            deadLetterQueue.send(event)
        }
    }
}
```

## 모니터링 및 로깅

### 1. 구조화된 로깅

```kotlin
class GameLoggingInterceptor : UseCase.Interceptor {
    override suspend fun <T> intercept(
        useCase: UseCase<T>,
        input: Any
    ): T {
        val context = LogContext(
            useCase = useCase::class.simpleName,
            input = input,
            timestamp = Instant.now()
        )
        
        logger.info("Executing use case", context)
        
        return try {
            val result = useCase.execute(input)
            logger.info("Use case completed", context.copy(result = result))
            result
        } catch (e: Exception) {
            logger.error("Use case failed", context.copy(error = e))
            throw e
        }
    }
}
```

### 2. 성능 메트릭

```kotlin
class PerformanceMonitor {
    private val metrics = MeterRegistry()
    
    fun recordAICalculationTime(duration: Duration, difficulty: AIDifficulty) {
        metrics.timer("ai.calculation.time")
            .tag("difficulty", difficulty.name)
            .record(duration)
    }
    
    fun recordMoveProcessingTime(duration: Duration) {
        metrics.timer("move.processing.time").record(duration)
    }
}
```

## 결론

이 아키텍처는 다음과 같은 이점을 제공합니다:

1. **확장성**: 새로운 기능을 기존 코드 수정 없이 추가 가능
2. **유지보수성**: 명확한 계층 분리로 변경 영향도 최소화
3. **테스트 용이성**: 각 계층을 독립적으로 테스트 가능
4. **성능**: 비동기 처리와 최적화 기법으로 높은 성능 달성
5. **안정성**: 불변성과 타입 안전성으로 런타임 오류 최소화

이 아키텍처를 기반으로 지속적으로 발전 가능한 오목 게임을 구축할 수 있습니다.