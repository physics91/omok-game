# 오목 게임 (Gomoku)

Kotlin으로 구현한 콘솔 기반 오목 게임입니다.

## 게임 방법

- 15x15 크기의 바둑판에서 플레이합니다
- 흑돌(●)과 백돌(○)이 번갈아가며 둡니다
- 가로, 세로, 대각선 중 한 방향으로 5개의 돌을 연속으로 놓으면 승리합니다
- 좌표는 행과 열을 공백으로 구분하여 입력합니다 (예: 8 8)

## 실행 방법

### 방법 1: Kotlin 컴파일러 사용
```bash
kotlinc Gomoku.kt -include-runtime -d gomoku.jar
java -jar gomoku.jar
```

### 방법 2: Gradle 사용
```bash
./gradlew run
```

### 방법 3: JAR 파일 생성 후 실행
```bash
./gradlew jar
java -jar build/libs/gomoku-1.0-SNAPSHOT.jar
```

## Windows에서 실행

### 사전 요구사항
- Java 11 이상 설치 필요
- 다운로드: https://adoptium.net/

### 실행 방법
1. 명령 프롬프트(cmd) 또는 PowerShell 열기
2. 프로젝트 폴더로 이동
3. 다음 명령어 실행:
   ```cmd
   gradlew.bat jar
   java -jar build\libs\gomoku-1.0-SNAPSHOT.jar
   ```

또는 JAR 파일을 더블 클릭하여 실행할 수 있습니다.