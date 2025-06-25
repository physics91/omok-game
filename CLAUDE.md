# 오목 게임 프로젝트 아키텍처 & 코딩 가이드

## 프로젝트 개요
렌주룰 오목 게임 - Clean Architecture 기반 Kotlin + Swing 데스크톱 게임 애플리케이션

### 기술 스택
- **언어**: Kotlin 1.9.0
- **UI 프레임워크**: Java Swing
- **빌드 도구**: Gradle Kotlin DSL
- **Java 버전**: 17
- **비동기 처리**: Kotlin Coroutines
- **테스트**: JUnit 5, Mockito

## 아키텍처 구조

### Clean Architecture + Hexagonal Architecture 패턴

```
src/main/kotlin/com/omok/
├── domain/                 # 도메인 계층 (핵심 비즈니스 로직)
│   ├── model/             # 도메인 모델
│   │   ├── Game.kt        # 게임 애그리거트 루트
│   │   ├── Board.kt       # 불변 게임 보드
│   │   ├── Player.kt      # 플레이어 열거형
│   │   ├── Move.kt        # 수 데이터 클래스
│   │   └── GameState.kt   # 게임 상태 모델
│   ├── service/           # 도메인 서비스
│   │   ├── GameEngine.kt  # 게임 로직 엔진
│   │   ├── RuleValidator.kt # 렌주룰 검증
│   │   └── AIStrategy.kt  # AI 전략 인터페이스
│   └── event/             # 도메인 이벤트
│       └── GameEvent.kt   # 게임 이벤트 시스템
├── application/           # 애플리케이션 계층
│   ├── usecase/          # 유스케이스 (애플리케이션 서비스)
│   │   ├── StartGameUseCase.kt
│   │   ├── MakeMoveUseCase.kt
│   │   ├── UndoMoveUseCase.kt
│   │   └── ProcessAIMoveUseCase.kt
│   └── service/          # 애플리케이션 서비스
│       └── GameApplicationService.kt
├── infrastructure/        # 인프라 계층
│   ├── ai/               # AI 구현체
│   │   └── MinimaxAI.kt  # 미니맥스 AI 구현
│   └── DependencyContainer.kt # 의존성 주입 컨테이너
└── presentation/         # 프레젠테이션 계층
    ├── ui/               # UI 컴포넌트
    │   ├── GameWindow.kt # 메인 윈도우
    │   └── GameBoardPanel.kt # 게임 보드 패널
    ├── controller/       # 프레젠테이션 컨트롤러
    │   └── GameController.kt
    └── Main.kt           # 애플리케이션 진입점
```

### 레이어별 책임

#### 1. **Domain Layer** (도메인 계층) - 핵심 비즈니스 로직
**책임사항:**
- 게임의 핵심 비즈니스 규칙 구현
- 도메인 모델 정의 (Game, Board, Player, Move)
- 렌주룰 검증 로직
- 도메인 이벤트 정의

**주요 원칙:**
- 외부 계층에 의존하지 않음 (가장 안정적인 계층)
- 불변성과 순수 함수 지향
- 도메인 전문가의 언어를 반영하는 명확한 모델

#### 2. **Application Layer** (애플리케이션 계층) - 유스케이스 조정
**책임사항:**
- 사용자의 요청을 도메인 계층으로 연결
- 비즈니스 플로우 조정
- 트랜잭션 관리
- 도메인 이벤트 발행

**주요 특징:**
- 얇은 계층 (비즈니스 로직은 포함하지 않음)
- 유스케이스별로 명확하게 분리
- 코루틴을 활용한 비동기 처리

#### 3. **Infrastructure Layer** (인프라 계층) - 기술적 구현
**책임사항:**
- 외부 의존성 구현 (AI, 데이터베이스 등)
- 도메인 인터페이스의 구체적 구현
- 의존성 주입 설정

**특징:**
- 도메인 계층이 정의한 인터페이스 구현
- 기술적 관심사 처리
- 교체 가능한 구현체

#### 4. **Presentation Layer** (프레젠테이션 계층) - 사용자 인터페이스
**책임사항:**
- 사용자 인터페이스 제공
- 사용자 입력을 애플리케이션 계층으로 전달
- 도메인 이벤트 구독 및 UI 업데이트

**특징:**
- 이벤트 기반 UI 업데이트
- 도메인 모델을 직접 조작하지 않음
- View와 Controller 분리

## 코딩 규칙 & 컨벤션

### 1. **네이밍 컨벤션**
- **클래스**: PascalCase (`GameController`, `RenjuRule`)
- **함수/변수**: camelCase (`makeMove`, `currentPlayer`)
- **상수**: UPPER_SNAKE_CASE (`BOARD_SIZE`, `WIN_SCORE`)
- **패키지**: 소문자 (`com.omok`)

### 2. **파일 구조**
- 한 파일당 하나의 주요 클래스
- 관련 데이터 클래스/열거형은 같은 파일에 위치 가능
- 파일명은 주요 클래스명과 일치

### 3. **코드 스타일**
```kotlin
// ✅ 좋은 예시
class GameController(private val gameView: GameView) {
    private val board = Array(BOARD_SIZE) { Array(BOARD_SIZE) { Stone.EMPTY } }
    
    fun makeMove(row: Int, col: Int): Boolean {
        if (gameState != GameState.PLAYING) return false
        // 로직 구현
        return true
    }
}

// ❌ 피해야 할 예시
class gamecontroller {
    var Board: Array<Array<Stone>>? = null
    fun MakeMove(r: Int, c: Int) {
        // 구현
    }
}
```

### 4. **에러 처리**
- 예외 상황에 대한 적절한 처리
- 사용자에게 명확한 메시지 제공
- 방어적 프로그래밍 원칙 적용

```kotlin
// ✅ 좋은 예시
fun makeMove(row: Int, col: Int): Boolean {
    if (gameState != GameState.PLAYING) return false
    if (board[row][col] != Stone.EMPTY) return false
    
    if (!RenjuRule.isValidMove(board, row, col, currentPlayer)) {
        gameView.showMessage("금수입니다! 다른 위치를 선택하세요.")
        return false
    }
    // 정상 처리
}
```

### 5. **상수 관리**
- 매직 넘버 사용 금지
- 의미있는 상수명 사용
- companion object를 통한 상수 그룹화

```kotlin
// ✅ 좋은 예시
companion object {
    const val BOARD_SIZE = 15
    const val WIN_SCORE = 1000000
    const val CELL_SIZE = 40
}
```

## Clean Architecture 핵심 개념

### 1. **의존성 규칙 (Dependency Rule)**
```
Presentation → Application → Domain ← Infrastructure
```
- 내부 계층은 외부 계층에 의존하지 않음
- 의존성은 항상 안정적인 계층을 향함
- 인터페이스를 통한 의존성 역전

### 2. **이벤트 기반 아키텍처**
```kotlin
// 도메인 이벤트 발행
eventBus.publish(GameEvent.MoveMade(game, move))

// UI에서 이벤트 구독
class GameController : GameEventHandler {
    override fun handle(event: GameEvent) {
        when (event) {
            is GameEvent.MoveMade -> updateUI(event.game)
            // ...
        }
    }
}
```

### 3. **불변성과 함수형 접근**
```kotlin
// 불변 도메인 모델
class Board private constructor(private val stones: Array<Array<Player?>>) {
    fun placeStone(move: Move): Board {
        // 새로운 Board 인스턴스 반환
        return Board(newStones)
    }
}

// 순수 함수
fun checkWin(board: Board, position: Position, player: Player): GameState
```

## 확장 가능한 아키텍처 가이드

### 1. **새로운 기능 추가 시**

#### 새로운 게임 규칙 추가:
```kotlin
// domain/service/에 새로운 규칙 검증기 추가
class CustomRuleValidator : RuleValidator {
    override fun isValidMove(board: Board, position: Position, player: Player): Boolean {
        // 커스텀 규칙 구현
    }
}
```

#### 새로운 AI 전략 추가:
```kotlin
// infrastructure/ai/에 새로운 AI 구현
class MonteCarloAI(private val simulationCount: Int) : AIStrategy {
    override fun getBestMove(board: Board, player: Player): Position? {
        // Monte Carlo Tree Search 구현
    }
}
```

#### 새로운 유스케이스 추가:
```kotlin
// application/usecase/에 새로운 유스케이스 추가
class SaveGameUseCase(
    private val gameRepository: GameRepository,
    private val eventBus: GameEventBus
) {
    fun execute(game: Game, filename: String): SaveResult {
        // 게임 저장 로직
    }
}
```

### 2. **의존성 주입 패턴**
```kotlin
// DependencyContainer에서 객체 그래프 구성
class DependencyContainer {
    fun createGameApplicationService(aiDifficulty: AIDifficulty): GameApplicationService {
        val aiStrategy = createAIStrategy(aiDifficulty)
        val gameEngine = createGameEngine(aiStrategy)
        return GameApplicationService(/* dependencies */)
    }
}
```

### 3. **테스트 전략**

#### 단위 테스트 (Unit Tests):
```kotlin
// 도메인 로직 테스트
class GameEngineTest {
    @Test
    fun `should detect win condition correctly`() {
        val board = Board().placeStone(Move(Position(7, 7), Player.BLACK))
        // ...
        val result = ruleValidator.checkWin(board, Position(7, 7), Player.BLACK)
        // assertions
    }
}

// AI 테스트
class MinimaxAITest {
    @Test
    fun `should find winning move`() {
        val ai = MinimaxAI(AIDifficulty.HARD)
        val move = ai.getBestMove(board, Player.WHITE)
        assertNotNull(move)
    }
}
```

#### 통합 테스트 (Integration Tests):
```kotlin
class GameApplicationServiceTest {
    @Test
    fun `should complete full game flow`() {
        val service = dependencyContainer.createGameApplicationService()
        val game = service.startNewGame(GameSettings(GameMode.PLAYER_VS_PLAYER))
        val result = service.makeMove(Position(7, 7))
        assertNotNull(result)
    }
}
```

### 4. **성능 최적화 전략**

#### 비동기 처리:
```kotlin
// AI 계산을 코루틴으로 처리
class ProcessAIMoveUseCase {
    suspend fun execute(game: Game): Game? = withContext(Dispatchers.Default) {
        // AI 계산
        val result = gameEngine.processAIMove(game)
        withContext(Dispatchers.Main) {
            // UI 업데이트
            eventBus.publish(GameEvent.MoveMade(game, move))
        }
        result.game
    }
}
```

#### 메모리 최적화:
- 불변 객체 사용으로 메모리 안정성 확보
- Board 복사 시 필요한 부분만 새로 생성
- 이벤트 핸들러 적절한 해제

## 코드 품질 도구 설정

### 현재 build.gradle.kts 설정:
```kotlin
plugins {
    kotlin("jvm") version "1.9.0"
    application
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.7.3")
    
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.2")
    testImplementation("org.mockito.kotlin:mockito-kotlin:4.1.0")
}

tasks.test {
    useJUnitPlatform()
}

application {
    mainClass.set("com.omok.presentation.MainKt")
}
```

### 추가 권장 도구:
```kotlin
// 코드 품질 도구 추가
plugins {
    id("org.jlleitschuh.gradle.ktlint") version "11.0.0"
    id("io.gitlab.arturbosch.detekt") version "1.23.1"
}

// 정적 분석 설정
detekt {
    buildUponDefaultConfig = true
    config = files("$projectDir/config/detekt/detekt.yml")
}
```

## 개발 워크플로우

### 1. **Clean Architecture 기반 개발 절차**
1. **도메인 모델링**: 비즈니스 요구사항을 도메인 모델로 표현
2. **유스케이스 정의**: 사용자 시나리오를 유스케이스로 구현
3. **인터페이스 설계**: 계층 간 의존성을 인터페이스로 추상화
4. **외부 구현**: Infrastructure 계층에서 구체적 구현
5. **테스트 작성**: 각 계층별 단위 테스트 및 통합 테스트
6. **UI 연결**: Presentation 계층에서 이벤트 기반 UI 구현

### 2. **개발 시 고려사항**

#### 도메인 중심 설계:
- 비즈니스 로직은 Domain 계층에만 위치
- 도메인 객체는 불변성 유지
- 도메인 이벤트를 통한 사이드 이펙트 처리

#### 의존성 관리:
- 의존성 역전 원칙 준수
- DependencyContainer를 통한 객체 생성
- 인터페이스를 통한 느슨한 결합

#### 테스트 용이성:
- 순수 함수로 비즈니스 로직 구현
- Mock 객체를 활용한 격리 테스트
- 의존성 주입으로 테스트 더블 사용

### 3. **커밋 메시지 컨벤션**
```
feat(domain): 새로운 게임 규칙 추가
fix(ui): 보드 렌더링 버그 수정
refactor(app): 유스케이스 구조 개선
test(domain): RuleValidator 테스트 추가
docs: 아키텍처 가이드 업데이트
```

## 권장 IDE 설정

### IntelliJ IDEA 플러그인:
- Kotlin
- Gradle
- Ktlint (코드 포매팅)

### 코드 포매팅 설정:
- 들여쓰기: 공백 4개
- 줄 길이: 120자
- import 정리 자동화

## 아키텍처 이점 및 장점

### 1. **유지보수성**
- 계층 분리로 변경 영향도 최소화
- 단일 책임 원칙으로 명확한 역할 분담
- 의존성 역전으로 유연한 구조

### 2. **테스트 용이성**
- 도메인 로직의 순수 함수화
- 의존성 주입으로 Mock 테스트 가능
- 계층별 독립적 테스트

### 3. **확장성**
- 새로운 기능 추가 시 기존 코드 수정 최소화
- 플러그인 아키텍처로 AI 전략 교체 가능
- 이벤트 시스템으로 느슨한 결합

### 4. **성능 최적화**
- 불변 객체로 메모리 안정성
- 코루틴 기반 비동기 처리
- 이벤트 기반 UI 업데이트로 반응성 향상

## 결론

이 Clean Architecture 기반 오목 게임은 다음 원칙들을 실현합니다:

1. **관심사의 분리**: 각 계층이 명확한 책임을 가짐
2. **의존성 역전**: 안정적인 추상화에 의존
3. **이벤트 기반**: 느슨한 결합과 높은 응집성
4. **불변성**: 안전하고 예측 가능한 상태 관리
5. **테스트 친화적**: 모든 계층의 독립적 테스트 가능

이 아키텍처를 기반으로 확장 가능하고 유지보수 가능한 소프트웨어를 개발하세요.

---

**참고 자료:**
- Clean Architecture (Robert C. Martin)
- Hexagonal Architecture (Alistair Cockburn)  
- Domain Driven Design (Eric Evans)
- Kotlin Coroutines 공식 문서