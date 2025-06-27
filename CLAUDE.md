# 오목 게임 프로젝트 아키텍처 & 코딩 가이드

## 프로젝트 개요
렌주룰 오목 게임 - Clean Architecture 기반 Kotlin + Swing 데스크톱 게임 애플리케이션

**📋 현재 상태**: UI 간소화 및 모듈화 완료 (2024.12)
- 불필요한 UI 컴포넌트 제거로 깔끔한 인터페이스 구현
- 통합 다이얼로그 시스템으로 일관된 사용자 경험 제공
- Clean Architecture 원칙 준수를 위한 모듈화 구조 개선

### 기술 스택
- **언어**: Kotlin 1.9.0
- **UI 프레임워크**: Java Swing + 커스텀 컴포넌트
- **빌드 도구**: Gradle Kotlin DSL
- **Java 버전**: 17
- **비동기 처리**: Kotlin Coroutines
- **테스트**: JUnit 5, Mockito
- **아키텍처**: Clean Architecture + Hexagonal Architecture
- **UI 패턴**: 통합 다이얼로그 시스템, 컴포넌트 기반 설계

## 아키텍처 구조

### Clean Architecture + Hexagonal Architecture 패턴

```
src/main/kotlin/com/omok/
├── domain/                     # 도메인 계층 (핵심 비즈니스 로직)
│   ├── model/                 # 도메인 모델
│   │   ├── Game.kt            # 게임 애그리거트 루트
│   │   ├── Board.kt           # 불변 게임 보드
│   │   ├── Player.kt          # 플레이어 열거형
│   │   ├── Move.kt            # 수 데이터 클래스
│   │   └── GameState.kt       # 게임 상태 모델
│   ├── service/               # 도메인 서비스
│   │   ├── GameEngine.kt      # 게임 로직 엔진 (DI 적용)
│   │   ├── RuleValidator.kt   # 렌주룰 검증
│   │   └── AIStrategy.kt      # AI 전략 인터페이스
│   ├── logging/               # 🆕 도메인 로깅 추상화
│   │   └── DomainLogger.kt    # 로깅 인터페이스 (의존성 역전)
│   ├── achievement/           # 성취도 시스템
│   └── event/                 # 도메인 이벤트
│       └── GameEvent.kt       # 게임 이벤트 시스템
├── application/               # 애플리케이션 계층
│   ├── usecase/              # 유스케이스 (애플리케이션 서비스)
│   │   ├── StartGameUseCase.kt
│   │   ├── MakeMoveUseCase.kt
│   │   ├── UndoMoveUseCase.kt
│   │   └── ProcessAIMoveUseCase.kt
│   ├── ui/                   # 🆕 UI 추상화 인터페이스
│   │   └── DialogService.kt   # 다이얼로그 서비스 인터페이스
│   └── service/              # 애플리케이션 서비스
│       └── GameApplicationService.kt
├── infrastructure/            # 인프라 계층
│   ├── ai/                   # AI 구현체
│   │   ├── MinimaxAI.kt      # 미니맥스 AI 구현
│   │   └── EnhancedAI.kt     # 고급 AI 구현
│   ├── logging/              # 🆕 로깅 구현체
│   │   ├── Logger.kt         # 파일/콘솔 로거
│   │   └── DomainLoggerImpl.kt # 도메인 로거 구현체
│   ├── ui/                   # 🆕 UI 서비스 구현체
│   │   └── SwingDialogService.kt # Swing 다이얼로그 구현
│   └── DependencyContainer.kt # 의존성 주입 컨테이너 (DI 강화)
└── presentation/             # 프레젠테이션 계층
    ├── ui/                   # UI 컴포넌트 (간소화됨)
    │   ├── GameWindow.kt     # 🔄 간소화된 메인 윈도우
    │   ├── GameBoardPanel.kt # 게임 보드 패널
    │   ├── components/       # 🆕 통합 UI 컴포넌트
    │   │   ├── UnifiedDialog.kt      # 통합 다이얼로그 시스템
    │   │   ├── SimplifiedMenuBar.kt  # 간소화된 메뉴바
    │   │   └── ModernButton.kt       # 모던 버튼 컴포넌트
    │   └── dialogs/          # 특화된 다이얼로그들
    │       ├── GameSelectionDialog.kt # 🔄 간소화된 게임 선택
    │       └── SettingsDialog.kt     # 설정 다이얼로그
    ├── controller/           # 프레젠테이션 컨트롤러
    │   └── GameController.kt
    └── Main.kt              # 애플리케이션 진입점
```

**🔄 = 간소화됨, 🆕 = 새로 추가됨**

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
- 간소화된 사용자 인터페이스 제공
- 사용자 입력을 애플리케이션 계층으로 전달
- 도메인 이벤트 구독 및 UI 업데이트
- 통합 다이얼로그 시스템을 통한 일관된 사용자 경험

**🆕 주요 개선사항:**
- **UI 간소화**: 불필요한 컴포넌트 제거로 깔끔한 인터페이스 구현
  - GameHeader, 개별 버튼 패널 제거
  - SimplifiedMenuBar로 기능 재구성
- **통합 다이얼로그**: UnifiedDialog 시스템으로 모든 팝업 표준화
  - 일관된 디자인과 키보드 단축키 지원
  - 페이드 인 애니메이션 효과 추가
- **컴포넌트 모듈화**: 재사용 가능한 UI 컴포넌트 분리

**특징:**
- 메뉴 기반 네비게이션으로 UI 복잡도 감소
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

## 🆕 UI 시스템 & 모듈화 개선사항 (2024.12 업데이트)

### 1. **통합 다이얼로그 시스템 (UnifiedDialog)**

#### 개념
모든 팝업 다이얼로그를 표준화하여 일관된 사용자 경험 제공

#### 주요 기능
```kotlin
// 정보 다이얼로그
UnifiedDialog.showInfo(parent, "제목", "메시지")

// 확인 다이얼로그
val result = UnifiedDialog.showConfirm(parent, "확인", "정말 삭제하시겠습니까?")
if (result == DialogResult.CONFIRMED) {
    // 확인된 경우 처리
}

// 선택 다이얼로그
val (result, selected) = UnifiedDialog.showSelection(
    parent, "선택", "옵션을 선택하세요", 
    arrayOf("옵션1", "옵션2", "옵션3")
)

// 입력 다이얼로그
val (result, input) = UnifiedDialog.showInput(parent, "입력", "이름을 입력하세요")
```

#### 장점
- **일관성**: 모든 다이얼로그가 동일한 디자인과 동작
- **접근성**: ESC/Enter 키보드 단축키 지원
- **UX**: 페이드 인 애니메이션으로 부드러운 전환
- **확장성**: 새로운 다이얼로그 타입 쉽게 추가 가능

### 2. **간소화된 UI 구조**

#### 이전 구조 (복잡함)
```
GameWindow
├── GameHeader (제거됨)
├── GameBoard
├── ButtonPanel (제거됨)
│   ├── UndoButton
│   ├── NewGameButton
│   └── SettingsButton
└── StatusPanel
```

#### 🔄 현재 구조 (간소화됨)
```
GameWindow
├── SimplifiedMenuBar (새로 추가)
│   ├── 게임 메뉴 (새 게임, 무르기, 저장/불러오기)
│   ├── 도구 메뉴 (설정, 테마, 고급 기능)
│   └── 도움말 메뉴 (통계, 성취도, 규칙)
├── GameBoard (핵심 유지)
├── GameInfoPanel (핵심 유지)
├── GameTimer (핵심 유지)
├── GameReplayPanel (핵심 유지)
└── StatusPanel (핵심 유지)
```

#### 장점
- **간결성**: 불필요한 버튼과 패널 제거
- **접근성**: 메뉴 기반 네비게이션으로 키보드 친화적
- **공간 효율성**: 더 넓은 게임 영역 확보
- **논리적 그룹화**: 관련 기능들이 메뉴별로 체계적 구성

### 3. **모듈화 및 의존성 역전 개선**

#### 🆕 도메인 로깅 추상화
```kotlin
// 도메인 계층 - 인터페이스 정의
interface DomainLogger {
    fun debug(message: String)
    fun info(message: String)
    fun warn(message: String, throwable: Throwable? = null)
    fun error(message: String, throwable: Throwable? = null)
}

// 인프라 계층 - 구현체
class DomainLoggerImpl(private val logger: Logger) : DomainLogger {
    override fun info(message: String) {
        logger.info("Domain", message)
    }
}

// 도메인 서비스 - 의존성 주입
class GameEngine(
    private val ruleValidator: RuleValidator,
    private val aiStrategy: AIStrategy?,
    private val logger: DomainLogger = NoOpLogger  // 기본값으로 NoOp 제공
)
```

#### 🆕 UI 서비스 추상화
```kotlin
// 애플리케이션 계층 - 인터페이스 정의
interface DialogService {
    fun showInfo(title: String, message: String): DialogResult
    fun showConfirm(title: String, message: String): DialogResult
}

// 인프라 계층 - Swing 구현체
class SwingDialogService(private val parentWindow: Window?) : DialogService {
    override fun showInfo(title: String, message: String): DialogResult {
        return mapResult(UnifiedDialog.showInfo(parentWindow, title, message))
    }
}
```

#### 아키텍처 무결성 확보
- ✅ **의존성 규칙 준수**: 도메인 → 인프라 의존성 제거
- ✅ **인터페이스 분리**: 각 계층의 관심사별 인터페이스 분리
- ✅ **의존성 주입**: DependencyContainer를 통한 체계적 DI

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

#### 🆕 새로운 다이얼로그 추가:
```kotlin
// presentation/ui/dialogs/에 새로운 다이얼로그 추가
class CustomDialog(parent: Frame) : JDialog(parent, "제목", true) {
    
    fun showDialog(): DialogResult {
        // UnifiedDialog 패턴 활용
        val content = createContent()
        val config = DialogConfig(
            title = "커스텀 다이얼로그",
            content = content,
            buttons = listOf(
                ButtonConfig("확인", ModernButton.ButtonStyle.PRIMARY, DialogResult.CONFIRMED),
                ButtonConfig("취소", ModernButton.ButtonStyle.GHOST, DialogResult.CANCELLED)
            ),
            headerContent = DialogHeader("제목", icon = customIcon)
        )
        
        return UnifiedDialog.showCustom(parent, config).apply { 
            isVisible = true 
        }.getResult()
    }
}
```

#### 🆕 메뉴 기능 추가:
```kotlin
// SimplifiedMenuBar에 새로운 메뉴 아이템 추가
private fun createToolsMenu(): JMenu {
    val menu = JMenu("도구")
    
    // 기존 메뉴 아이템들...
    
    // 새로운 기능 추가
    val newFeatureItem = JMenuItem("새로운 기능")
    newFeatureItem.icon = IconLoader.getIcon(IconLoader.Icon.CUSTOM, 16, 16)
    newFeatureItem.accelerator = KeyStroke.getKeyStroke("ctrl F")
    newFeatureItem.addActionListener { gameWindow.showNewFeatureDialog() }
    
    menu.add(newFeatureItem)
    return menu
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

## 업데이트 히스토리

### 2024.12 - UI 단순화 및 모듈화 강화

#### 주요 변경사항:
1. **불필요한 UI 컴포넌트 제거**
   - GameHeader, undoButton, newGameButton, settingsButton 제거
   - 복잡한 버튼 패널 구조 단순화
   - 메뉴 기반 인터페이스로 통합

2. **팝업 UI/UX 통합 및 강화**
   - UnifiedDialog 시스템 도입으로 모든 다이얼로그 표준화
   - 페이드인 애니메이션 효과 추가
   - 일관된 디자인 및 사용자 경험 제공

3. **Clean Architecture 모듈화 개선**
   - DomainLogger 인터페이스로 의존성 역전 구현
   - DialogService 추상화로 UI 계층 분리
   - 아키텍처 위반 요소 제거 및 계층 간 의존성 명확화

#### 기술적 세부사항:

**UnifiedDialog 시스템:**
```kotlin
// 모든 다이얼로그를 통합하는 단일 인터페이스
UnifiedDialog.showInfo(parent, "게임 시작", "새 게임이 시작됩니다")
UnifiedDialog.showConfirm(parent, "확인", "정말 종료하시겠습니까?")
UnifiedDialog.showSelection(parent, "난이도 선택", "AI 난이도를 선택하세요", options)
```

**의존성 역전 구현:**
```kotlin
// Domain Layer Interface
interface DomainLogger {
    fun info(message: String)
    fun debug(message: String)
    fun warn(message: String, throwable: Throwable? = null)
    fun error(message: String, throwable: Throwable? = null)
}

// Infrastructure Layer Implementation
class DomainLoggerImpl(private val logger: Logger) : DomainLogger
```

**UI 서비스 추상화:**
```kotlin
// Application Layer Interface
interface DialogService {
    fun showInfo(title: String, message: String): DialogResult
    fun showConfirm(title: String, message: String): DialogResult
}

// Infrastructure Layer Implementation
class SwingDialogService : DialogService
```

#### 아키텍처 개선 효과:
- **의존성 위반 제거**: Domain → Infrastructure 의존성 완전 제거
- **UI 일관성 향상**: 모든 다이얼로그가 동일한 디자인 패턴 사용
- **개발 효율성 증대**: 새로운 다이얼로그 추가 시 UnifiedDialog 활용
- **테스트 용이성**: DialogService 인터페이스를 통한 Mock 테스트 가능
- **코드 품질 향상**: 중복 코드 제거 및 단일 책임 원칙 준수

#### 새로운 개발 워크플로우:
1. **다이얼로그 추가 시**: UnifiedDialog의 표준 메서드 사용
2. **도메인 로깅**: DomainLogger 인터페이스를 통한 계층 분리 로깅
3. **UI 테스트**: DialogService Mock을 활용한 단위 테스트
4. **메뉴 기반 네비게이션**: SimplifiedMenuBar를 통한 기능 접근

이러한 개선을 통해 더욱 견고하고 유지보수하기 쉬운 Clean Architecture가 완성되었습니다.