# UI 추가 조정 옵션

## 오목판이 여전히 작다면:
```kotlin
// GameBoardPanel.kt
const val CELL_SIZE = 42  // 더 크게
const val MARGIN = 35     // 여백 축소
const val STONE_SIZE = 38 // 돌도 크게
```

## 창이 너무 크다면:
```kotlin
// GameWindow.kt
val windowWidth = 700
val windowHeight = 780
```

## 폰트가 작다면:
```kotlin
// UITheme.kt
val TITLE = Font(SYSTEM_FONT, Font.BOLD, 32)
val HEADING = Font(SYSTEM_FONT, Font.BOLD, 24)
val BODY = Font(SYSTEM_FONT, Font.PLAIN, 18)
```

## 버튼이 작다면:
```kotlin
// ModernButton.kt
MEDIUM(Insets(14, 35, 14, 35), 18, UITheme.BorderRadius.MD)
```

## 현재 균형잡힌 설정:
- 오목판: 570px (적절한 크기)
- 창: 620×850px (오목판과 UI 요소를 모두 담기에 충분)
- 폰트: 16pt 기본 (읽기 쉬움)
- 버튼: 충분한 패딩과 크기