# 오목 게임 SVG 아이콘

## 아이콘 목록

### 게임 요소
- **black-stone.svg**: 흑돌 (그라디언트 효과)
- **white-stone.svg**: 백돌 (그라디언트 효과)
- **last-move.svg**: 마지막 수 표시 (애니메이션)
- **forbidden.svg**: 금수 표시
- **win.svg**: 승리 표시 (별 모양)

### UI 컨트롤
- **new-game.svg**: 새 게임
- **undo.svg**: 무르기
- **settings.svg**: 설정
- **help.svg**: 도움말
- **timer.svg**: 타이머

### 게임 모드
- **player-vs-player.svg**: 사람 대 사람
- **player-vs-ai.svg**: 사람 대 AI
- **ai-thinking.svg**: AI 사고 중 (애니메이션)

### 난이도
- **difficulty-easy.svg**: 쉬움 (1/3 바)
- **difficulty-medium.svg**: 보통 (2/3 바)
- **difficulty-hard.svg**: 어려움 (3/3 바)

### 특수 룰
- **swap.svg**: 스왑 (흑백 교체)
- **fifth-move.svg**: 5수 선택

### 기타
- **move-count.svg**: 수 카운트
- **logo.svg**: 앱 로고 (48x48)

## 사용 방법

### Kotlin에서 사용
```kotlin
import com.omok.presentation.ui.icons.IconLoader

// 아이콘 로드
val undoIcon = IconLoader.getIcon(IconLoader.Icon.UNDO)
button.icon = undoIcon

// 크기 지정
val largeIcon = IconLoader.getIcon(IconLoader.Icon.LOGO, 48, 48)
```

### 직접 SVG 사용
```kotlin
// SVG 스트림 가져오기
val svgStream = IconLoader.getIconStream(IconLoader.Icon.BLACK_STONE)
```

## 아이콘 특징

### 색상 팔레트
- Primary: `#6366F1` (인디고)
- Secondary: `#6B7280` (회색)
- Error: `#EF4444` (빨강)
- Success: `#10B981` (초록)
- Warning: `#F59E0B` (주황)
- Black Stone: `#000000` (검정)
- White Stone: `#FFFFFF` (흰색)

### 애니메이션
- **ai-thinking.svg**: 펄싱 애니메이션
- **last-move.svg**: 회전 애니메이션

### 크기
- 기본: 24x24px
- 로고: 48x48px
- 모든 아이콘은 벡터 형식으로 크기 조정 가능

## 확장 가이드

### 새 아이콘 추가
1. `/src/main/resources/icons/`에 SVG 파일 추가
2. `IconLoader.Icon` enum에 항목 추가
3. 필요시 색상은 UITheme 컬러 사용

### SVG 최적화
- viewBox는 "0 0 24 24" 사용 (로고 제외)
- 불필요한 메타데이터 제거
- 경로 최적화로 파일 크기 감소

## 주의사항
- Java Swing은 기본적으로 SVG를 지원하지 않음
- 실제 사용 시 SVG 라이브러리(Apache Batik 등) 사용 권장
- 또는 PNG로 변환하여 사용