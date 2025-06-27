# 오픈 렌주룰 구현 완료

## 구현된 기능

### 1. 오프닝 규정
- **1수 (흑)**: 반드시 천원(중앙, 7,7)에 착수
- **2수 (백)**: 천원 주위 8곳 중 선택 (3×3 범위에서 중앙 제외)
- **3수 (흑)**: 천원에서 2칸 이내 26점 중 선택 (5×5 범위에서 중앙 제외)

### 2. 스왑 메커니즘
- 3수 이후 백이 흑백 교체 선택 가능
- `GameState.WaitingForSwap` 상태로 전환
- UI에서 스왑 다이얼로그 표시
- 스왑 선택 시 플레이어 역할 교체

### 3. 5수 선택 메커니즘
- 4수 이후 흑이 5수로 둘 위치 2개 제시
- `GameState.WaitingForFifthMove` 상태로 전환
- UI에서 5수 제시 모드 활성화 (2개 위치 클릭)
- 백이 제시된 2개 중 1개 선택
- `GameState.WaitingForFifthMoveSelection` 상태로 전환

### 4. UI/UX 개선사항
- 5수 제시 시 시각적 표시 (번호 표시)
- 5수 선택 시 하이라이트 표시
- 특수 상태에 대한 명확한 안내 메시지
- 스왑 다이얼로그로 직관적인 선택

## 파일 구조

### Domain Layer
- `GameState.kt`: 오픈 렌주룰 특수 상태 추가
  - `WaitingForSwap`
  - `WaitingForFifthMove`
  - `WaitingForFifthMoveSelection`
- `RuleValidatorFactory.kt`: `OpenRenjuValidator` 구현
- `GameEngine.kt`: 스왑 및 5수 처리 메서드 추가

### Application Layer
- `GameApplicationService.kt`: 스왑 및 5수 관련 메서드 추가
  - `processSwap()`
  - `proposeFifthMoves()`
  - `selectFifthMove()`

### Presentation Layer
- `GameController.kt`: 이벤트 핸들러 및 다이얼로그 추가
- `GameWindow.kt`: 5수 관련 UI 메서드 추가
- `GameBoardPanel.kt`: 5수 제시/선택 모드 렌더링

### Events
- `GameEvent.kt`: 새로운 이벤트 타입 추가
  - `SwapDecision`
  - `FifthMovesProposed`
  - `InvalidAction`

## 테스트
- `OpenRenjuValidatorTest.kt`: 모든 오픈 렌주룰 기능 테스트
  - 오프닝 규정 검증
  - 스왑 메커니즘 검증
  - 5수 제시/선택 검증

## 사용 방법
1. 새 게임 시작 시 "오픈 렌주룰" 선택
2. 첫 3수는 정해진 위치에만 착수 가능
3. 3수 후 백은 스왑 여부 선택
4. 4수 후 흑은 5수로 둘 위치 2개 제시
5. 백은 제시된 2개 중 1개 선택
6. 이후 일반 렌주룰로 진행

## 기술적 특징
- Clean Architecture 원칙 준수
- 이벤트 기반 상태 관리
- 도메인 중심 설계
- 확장 가능한 룰 시스템