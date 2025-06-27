package com.omok.presentation.ui

import com.omok.application.dto.*
import com.omok.presentation.controller.GameController
import com.omok.presentation.ui.theme.UITheme
import com.omok.presentation.ui.theme.ThemeSelectionDialog
import com.omok.presentation.ui.theme.ThemeManager
import com.omok.presentation.ui.theme.ThemeChangeListener
import com.omok.presentation.ui.theme.GameTheme
import com.omok.presentation.ui.components.*
import com.omok.presentation.ui.dialogs.SettingsDialog
import com.omok.presentation.ui.accessibility.KeyboardShortcuts
import com.omok.presentation.ui.icons.IconLoader
import com.omok.presentation.ui.replay.GameReplayPanel
import java.awt.*
import java.awt.event.ActionEvent
import javax.swing.*
import javax.swing.border.EmptyBorder
import javax.swing.border.CompoundBorder
import javax.swing.border.LineBorder

class GameWindow : JFrame(), ThemeChangeListener {
    private lateinit var gameBoard: GameBoardPanel
    private lateinit var gameController: GameController
    private val statusPanel = StatusPanel()
    private val gameInfoPanel = GameInfoPanel()
    private val gameTimer = GameTimer()
    private val gameReplayPanel = GameReplayPanel()
    
    // 키보드 포커스 위치
    private var keyboardFocusPosition = PositionDto(7, 7)
    
    init {
        title = "렌주룰 오목 - Renju Gomoku"
        iconImage = IconLoader.getIcon(IconLoader.Icon.LOGO, 32, 32)?.image
        
        // 다크 모드 설정 적용
        val settings = com.omok.presentation.ui.settings.UIGameSettings.getInstance()
        val themeMode = if (settings.isDarkMode) UITheme.ThemeMode.DARK else UITheme.ThemeMode.LIGHT
        UITheme.setTheme(themeMode)
        
        initializeUI()
        setupKeyboardShortcuts()
        setupReplayPanel()
        
        // 테마 변경 리스너 등록
        ThemeManager.addThemeChangeListener(this)
    }
    
    fun setController(controller: GameController) {
        this.gameController = controller
        gameBoard.setController(controller)
        gameTimer.setOnTimeUpListener { player ->
            controller.handleTimeUp(player)
        }
    }
    
    val controller: GameController
        get() = gameController
    
    private fun initializeUI() {
        defaultCloseOperation = EXIT_ON_CLOSE
        layout = BorderLayout()
        
        UITheme.applyTheme()
        
        gameBoard = GameBoardPanel()
        
        createMenuBar()
        
        val mainPanel = JPanel(BorderLayout())
        mainPanel.background = UITheme.Colors.BACKGROUND
        
        val centerPanel = JPanel(BorderLayout())
        centerPanel.background = UITheme.Colors.SURFACE
        centerPanel.border = EmptyBorder(10, 10, 10, 10)
        
        // 간소화된 상단 정보 패널
        val topInfoPanel = createCompactInfoPanel()
        centerPanel.add(topInfoPanel, BorderLayout.NORTH)
        
        // Board in center with wrapper for proper centering
        val boardWrapper = JPanel(GridBagLayout())
        boardWrapper.background = UITheme.Colors.SURFACE
        boardWrapper.add(gameBoard)
        centerPanel.add(boardWrapper, BorderLayout.CENTER)
        
        mainPanel.add(centerPanel, BorderLayout.CENTER)
        
        // 하단 영역: 상태 패널만 (버튼들은 제거)
        val bottomContainer = JPanel(BorderLayout())
        bottomContainer.background = UITheme.Colors.BACKGROUND
        
        // 재생 패널 (초기에는 숨김)
        gameReplayPanel.isVisible = false
        bottomContainer.add(gameReplayPanel, BorderLayout.NORTH)
        
        // 상태 패널만 추가
        bottomContainer.add(statusPanel, BorderLayout.SOUTH)
        
        mainPanel.add(bottomContainer, BorderLayout.SOUTH)
        
        add(mainPanel)
        
        // 창 크기는 pack()이 컴포넌트 크기에 따라 자동 계산
        isResizable = false
        
        // 모든 컴포넌트가 추가된 후 pack() 호출
        pack()
        
        // 창을 화면 중앙에 배치
        setLocationRelativeTo(null)
        
        // 최소 크기 설정 (현재 크기로)
        minimumSize = size
    }
    
    private fun createMenuBar() {
        val menuBar = SimplifiedMenuBar(this)
        jMenuBar = menuBar
    }
    
    
    private val difficultyComboBox = JComboBox(AIDifficultyDto.values())

    private fun createCompactInfoPanel(): JPanel {
        val panel = JPanel(BorderLayout())
        panel.background = UITheme.Colors.SURFACE
        panel.border = EmptyBorder(10, 0, 10, 0)
        panel.preferredSize = Dimension(0, 60)

        // 왼쪽: 게임 정보
        val leftPanel = JPanel(FlowLayout(FlowLayout.LEFT, 10, 0))
        leftPanel.isOpaque = false
        leftPanel.add(gameInfoPanel)

        // 오른쪽: AI 난이도, 타이머
        val rightPanel = JPanel(FlowLayout(FlowLayout.RIGHT, 10, 0))
        rightPanel.isOpaque = false

        val difficultyLabel = JLabel("AI 난이도:")
        difficultyLabel.font = UITheme.Fonts.BODY
        rightPanel.add(difficultyLabel)
        rightPanel.add(difficultyComboBox)
        rightPanel.add(gameTimer)

        difficultyComboBox.addActionListener { e ->
            if (e.actionCommand == "comboBoxChanged") {
                val selectedDifficulty = difficultyComboBox.selectedItem as AIDifficultyDto
                val currentGame = gameController.getCurrentGame()
                if (currentGame?.settings?.mode == GameModeDto.PLAYER_VS_AI) {
                    startNewGame(GameModeDto.PLAYER_VS_AI, selectedDifficulty)
                }
            }
        }

        panel.add(leftPanel, BorderLayout.WEST)
        panel.add(rightPanel, BorderLayout.EAST)

        return panel
    }

    fun showNewGameDialog() {
        val dialog = com.omok.presentation.ui.dialogs.GameSelectionDialog(this)
        val (mode, difficulty) = dialog.showDialog()

        if (mode != null) {
            // 기본 설정으로 게임 시작 (고급 설정은 메뉴에서)
            val defaultRule = GameRuleDto.STANDARD_RENJU
            val defaultTimeLimit = TimeLimitDto.NONE

            startNewGame(mode, difficulty, defaultRule, defaultTimeLimit)
        }
    }

    fun startNewGame(
        mode: GameModeDto,
        difficulty: AIDifficultyDto? = null,
        rule: GameRuleDto = GameRuleDto.STANDARD_RENJU,
        timeLimit: TimeLimitDto = TimeLimitDto.NONE
    ) {
        gameController.startNewGame(mode, difficulty, rule, timeLimit)
        gameInfoPanel.resetInfo()
        gameInfoPanel.startTimer()

        // AI 난이도 콤보박스 상태 업데이트
        difficultyComboBox.isEnabled = mode == GameModeDto.PLAYER_VS_AI
        if (mode == GameModeDto.PLAYER_VS_AI) {
            difficultyComboBox.selectedItem = difficulty ?: AIDifficultyDto.MEDIUM
        }
    }
    
    fun showSettingsDialog() {
        val dialog = SettingsDialog(this)
        dialog.isVisible = true
    }
    
    private fun setupKeyboardShortcuts() {
        KeyboardShortcuts.setupShortcuts(rootPane, object : KeyboardShortcuts.ShortcutActions {
            override fun onNewGame() {
                showNewGameDialog()
            }
            
            override fun onUndo() {
                gameController.undoMove()
            }
            
            override fun onHelp() {
                showRulesDialog()
            }
            
            override fun onSettings() {
                showSettingsDialog()
            }
            
            override fun onQuit() {
                System.exit(0)
            }
            
            override fun onNavigate(direction: KeyboardShortcuts.Direction) {
                when (direction) {
                    KeyboardShortcuts.Direction.UP -> {
                        if (keyboardFocusPosition.row > 0) {
                            keyboardFocusPosition = PositionDto(keyboardFocusPosition.row - 1, keyboardFocusPosition.col)
                        }
                    }
                    KeyboardShortcuts.Direction.DOWN -> {
                        if (keyboardFocusPosition.row < 14) {
                            keyboardFocusPosition = PositionDto(keyboardFocusPosition.row + 1, keyboardFocusPosition.col)
                        }
                    }
                    KeyboardShortcuts.Direction.LEFT -> {
                        if (keyboardFocusPosition.col > 0) {
                            keyboardFocusPosition = PositionDto(keyboardFocusPosition.row, keyboardFocusPosition.col - 1)
                        }
                    }
                    KeyboardShortcuts.Direction.RIGHT -> {
                        if (keyboardFocusPosition.col < 14) {
                            keyboardFocusPosition = PositionDto(keyboardFocusPosition.row, keyboardFocusPosition.col + 1)
                        }
                    }
                }
                // gameBoard.setKeyboardFocus(keyboardFocusPosition) // TODO: implement when needed
            }
            
            override fun onPlaceStone() {
                gameController.makeMove(keyboardFocusPosition)
            }
            
            override fun onSave() {
                saveGame()
            }
            
            override fun onLoad() {
                loadGame()
            }
            
            override fun onExportSGF() {
                exportToSGF()
            }
            
            override fun onToggleReplayMode() {
                toggleReplayMode()
            }
            
            override fun onToggleMoveNumbers() {
                toggleMoveNumbers()
            }
            
            override fun onToggleDarkMode() {
                toggleDarkMode()
            }
            
            override fun onToggleSound() {
                toggleSound()
            }
        })
    }
    
    fun showRulesDialog() {
        val rulesText = """
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: '맑은 고딕', 'Malgun Gothic', sans-serif; color: #374151; line-height: 1.6; }
                    h2 { color: #111827; margin-top: 20px; }
                    h3 { color: #374151; margin-top: 15px; }
                    ul { margin: 10px 0; }
                    li { margin: 5px 0; }
                    b { color: #6366F1; }
                </style>
            </head>
            <body>
            <h2>렌주룰 설명</h2>
            <p>렌주룰은 오목의 공식 규칙으로, 흑돌에 대한 제한이 있습니다.</p>
            
            <h3>기본 규칙</h3>
            <ul>
                <li>흑돌이 먼저 시작합니다</li>
                <li>번갈아가며 돌을 놓습니다</li>
                <li>먼저 5개를 연속으로 놓는 사람이 승리합니다</li>
            </ul>
            
            <h3>흑돌 금수 (제한사항)</h3>
            <ul>
                <li><b>삼삼:</b> 열린 3을 동시에 2개 이상 만드는 수</li>
                <li><b>사사:</b> 4를 동시에 2개 이상 만드는 수</li>
                <li><b>장목:</b> 6개 이상을 연속으로 놓는 수</li>
            </ul>
            
            <p>백돌은 이러한 제한이 없으며, 5개 이상을 연속으로 놓으면 승리합니다.</p>
            </body>
            </html>
        """.trimIndent()
        
        UnifiedDialog.showInfo(this, "렌주룰 설명", rulesText)
    }
    
    private fun showRuleSelectionDialog(): GameRuleDto? {
        val dialog = ModernDialog(this, "게임 규칙 선택", true)
        
        val contentPanel = JPanel()
        contentPanel.layout = BoxLayout(contentPanel, BoxLayout.Y_AXIS)
        contentPanel.background = UITheme.Colors.BACKGROUND
        contentPanel.border = EmptyBorder(UITheme.Spacing.MD, UITheme.Spacing.MD, UITheme.Spacing.MD, UITheme.Spacing.MD)
        
        val buttonGroup = ButtonGroup()
        val radioButtons = mutableMapOf<JRadioButton, GameRuleDto>()
        
        for (rule in GameRuleDto.values()) {
            val rulePanel = JPanel()
            rulePanel.layout = BoxLayout(rulePanel, BoxLayout.Y_AXIS)
            rulePanel.background = UITheme.Colors.SURFACE
            rulePanel.border = CompoundBorder(
                LineBorder(UITheme.Colors.GRAY_200, 1, true),
                EmptyBorder(UITheme.Spacing.SM, UITheme.Spacing.MD, UITheme.Spacing.SM, UITheme.Spacing.MD)
            )
            rulePanel.alignmentX = Component.LEFT_ALIGNMENT
            rulePanel.maximumSize = Dimension(600, Integer.MAX_VALUE)
            
            val radioButton = JRadioButton(rule.displayName)
            radioButton.font = UITheme.Fonts.BODY
            radioButton.foreground = UITheme.Colors.GRAY_900
            radioButton.background = UITheme.Colors.SURFACE
            radioButton.isSelected = rule == GameRuleDto.STANDARD_RENJU
            buttonGroup.add(radioButton)
            radioButtons[radioButton] = rule
            
            val descriptionLabel = JLabel("<html><body style='width: 500px'>${rule.description}</body></html>")
            descriptionLabel.font = UITheme.Fonts.CAPTION
            descriptionLabel.foreground = UITheme.Colors.GRAY_600
            descriptionLabel.border = EmptyBorder(UITheme.Spacing.XS, 20, 0, 0)
            
            rulePanel.add(radioButton)
            rulePanel.add(descriptionLabel)
            
            contentPanel.add(rulePanel)
            contentPanel.add(Box.createRigidArea(Dimension(0, UITheme.Spacing.SM)))
        }
        
        val scrollPane = JScrollPane(contentPanel)
        scrollPane.border = null
        scrollPane.verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
        scrollPane.preferredSize = Dimension(650, 400)
        
        dialog.setContent(scrollPane)
        
        var result: GameRuleDto? = null
        
        val confirmButton = ModernButton("확인", ModernButton.ButtonStyle.PRIMARY)
        confirmButton.addActionListener {
            result = radioButtons.entries.firstOrNull { it.key.isSelected }?.value
            dialog.dispose()
        }
        
        val cancelButton = ModernButton("취소", ModernButton.ButtonStyle.GHOST)
        cancelButton.addActionListener {
            dialog.dispose()
        }
        
        dialog.addButton(cancelButton)
        dialog.addButton(confirmButton)
        
        dialog.pack()
        dialog.setLocationRelativeTo(this)
        dialog.isVisible = true
        
        return result
    }
    
    fun showAboutDialog() {
        val aboutText = """
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: '맑은 고딕', 'Malgun Gothic', sans-serif; color: #374151; text-align: center; }
                    h2 { color: #6366F1; margin: 20px 0 10px 0; }
                    p { margin: 10px 0; }
                    .version { font-size: 18px; color: #6B7280; }
                    .tech { background-color: #F3F4F6; padding: 15px; border-radius: 8px; margin: 20px 0; }
                </style>
            </head>
            <body>
            <h2>렌주룰 오목</h2>
            <p class='version'>버전 2.0</p>
            <p>Clean Architecture로 재구성된 렌주룰 오목 게임입니다.</p>
            <div class='tech'>
                <p><b>기술 스택</b></p>
                <p>Kotlin 1.9.0 + Swing</p>
                <p>AI: 미니맥스 알고리즘</p>
            </div>
            </body>
            </html>
        """.trimIndent()
        
        UnifiedDialog.showInfo(this, "정보", aboutText)
    }
    
    fun showStatisticsDialog() {
        val dialog = com.omok.presentation.ui.dialogs.StatisticsDialog(this)
        dialog.isVisible = true
    }
    
    fun showAchievementDialog() {
        val dependencyContainer = com.omok.infrastructure.DependencyContainer()
        val achievementManager = dependencyContainer.achievementManager
        val dialog = com.omok.presentation.ui.achievement.AchievementDialog(this, achievementManager)
        dialog.isVisible = true
    }
    
    fun updateBoard(board: BoardDto) {
        gameBoard.updateBoard(board)
        
        // Update move count
        var moveCount = 0
        for (row in 0 until 15) {
            for (col in 0 until 15) {
                if (board.getStone(PositionDto(row, col)) != null) {
                    moveCount++
                }
            }
        }
        gameInfoPanel.updateMoveCount(moveCount)
    }
    
    fun updateBoard(gameDto: GameDto) {
        gameBoard.updateBoard(gameDto.board, gameDto.moveHistory)
        
        // Update move count
        gameInfoPanel.updateMoveCount(gameDto.moveHistory.size)
    }
    
    fun updateTimer(gameDto: GameDto) {
        gameTimer.updateTimeState(gameDto.timeState, gameDto.currentPlayer)
    }
    
    fun updateStatus(status: String) {
        val isPlaying = status.contains("차례") || status.contains("AI")
        statusPanel.updateStatus(status, isPlaying)
        
        if (status.contains("흑돌")) {
            statusPanel.setCurrentTurn(true)
        } else if (status.contains("백돌")) {
            statusPanel.setCurrentTurn(false)
        }
    }
    
    fun setLastMove(position: PositionDto) {
        gameBoard.setLastMove(position)
    }
    
    fun clearLastMove() {
        gameBoard.clearLastMove()
    }
    
    fun setUndoEnabled(enabled: Boolean) {
        (jMenuBar as? SimplifiedMenuBar)?.setUndoEnabled(enabled)
    }
    
    fun showMessage(message: String) {
        UnifiedDialog.showInfo(this, "알림", message)
    }
    
    fun showWinAnimation(winningLine: List<PositionDto>) {
        gameBoard.showWinAnimation(winningLine)
        gameInfoPanel.stopTimer()
    }
    
    fun clearWinAnimation() {
        gameBoard.clearWinAnimation()
    }
    
    /**
     * 오픈 렌주룰 - 5수 제시 모드 활성화
     */
    fun enableFifthMoveProposalMode(onComplete: (List<PositionDto>) -> Unit) {
        gameBoard.enableFifthMoveProposalMode(onComplete)
    }
    
    /**
     * 오픈 렌주룰 - 5수 선택 모드 활성화
     */
    fun enableFifthMoveSelectionMode(proposedMoves: List<PositionDto>, onSelect: (PositionDto) -> Unit) {
        gameBoard.enableFifthMoveSelectionMode(proposedMoves, onSelect)
    }
    
    /**
     * 오픈 렌주룰 - 제시된 5수 위치 강조
     */
    fun highlightFifthMoveOptions(positions: List<PositionDto>) {
        gameBoard.highlightFifthMoveOptions(positions)
    }
    
    /**
     * AI 사고 과정 표시
     */
    fun showAIThinking(thinkingInfo: AIThinkingInfoDto) {
        gameBoard.showAIThinking(thinkingInfo)
    }
    
    /**
     * 마우스 좌표 업데이트
     */
    fun updateMouseCoordinate(position: PositionDto?) {
        val coordinate = position?.let {
            val col = ('A' + it.col).toString()
            val row = (15 - it.row).toString()
            "$col$row"
        }
        statusPanel.updateCoordinate(coordinate)
    }
    
    fun saveGame() {
        val currentGame = gameController.getCurrentGame()
        if (currentGame == null) {
            showMessage("저장할 게임이 없습니다.")
            return
        }
        
        val fileChooser = JFileChooser()
        fileChooser.dialogTitle = "게임 저장"
        fileChooser.currentDirectory = java.io.File("saves")
        fileChooser.fileFilter = object : javax.swing.filechooser.FileFilter() {
            override fun accept(f: java.io.File): Boolean {
                return f.isDirectory || f.name.endsWith(".omok")
            }
            override fun getDescription(): String = "오목 게임 파일 (*.omok)"
        }
        
        val result = fileChooser.showSaveDialog(this)
        if (result == JFileChooser.APPROVE_OPTION) {
            var fileName = fileChooser.selectedFile.name
            if (fileName.endsWith(".omok")) {
                fileName = fileName.removeSuffix(".omok")
            }
            
            if (gameController.saveGame(fileName)) {
                showMessage("게임이 저장되었습니다.")
            } else {
                showMessage("게임 저장에 실패했습니다.")
            }
        }
    }
    
    fun loadGame() {
        val fileChooser = JFileChooser()
        fileChooser.dialogTitle = "게임 불러오기"
        fileChooser.currentDirectory = java.io.File("saves")
        fileChooser.fileFilter = object : javax.swing.filechooser.FileFilter() {
            override fun accept(f: java.io.File): Boolean {
                return f.isDirectory || f.name.endsWith(".omok")
            }
            override fun getDescription(): String = "오목 게임 파일 (*.omok)"
        }
        
        val result = fileChooser.showOpenDialog(this)
        if (result == JFileChooser.APPROVE_OPTION) {
            val fileName = fileChooser.selectedFile.name
            
            val loadedGame = gameController.loadGame(fileName)
            if (loadedGame != null) {
                showMessage("게임이 불러와졌습니다.")
                gameInfoPanel.resetInfo()
                gameInfoPanel.startTimer()
                
                // Game information is now managed by the status panel
            } else {
                showMessage("게임 불러오기에 실패했습니다.")
            }
        }
    }
    
    private fun loadLatestAutoSave() {
        val loadedGame = gameController.loadLatestAutoSave()
        if (loadedGame != null) {
            showMessage("최근 자동 저장 게임이 불러와졌습니다.")
            gameInfoPanel.resetInfo()
            gameInfoPanel.startTimer()
            
            // Game information is now managed by the status panel
        } else {
            showMessage("최근 자동 저장 게임이 없습니다.")
        }
    }
    
    private fun toggleMoveNumbers() {
        val currentSettings = com.omok.presentation.ui.settings.UIGameSettings.getInstance()
        val newSettings = currentSettings.copy(showMoveNumbers = !currentSettings.showMoveNumbers)
        com.omok.presentation.ui.settings.UIGameSettings.updateSettings(newSettings)
        
        // 보드 다시 그리기
        gameBoard.repaint()
        
        val statusText = if (newSettings.showMoveNumbers) "수순 번호 표시" else "수순 번호 숨김"
        showMessage(statusText)
    }
    
    private fun toggleDarkMode() {
        val currentSettings = com.omok.presentation.ui.settings.UIGameSettings.getInstance()
        val newSettings = currentSettings.copy(isDarkMode = !currentSettings.isDarkMode)
        com.omok.presentation.ui.settings.UIGameSettings.updateSettings(newSettings)
        
        // 테마 업데이트
        val themeMode = if (newSettings.isDarkMode) UITheme.ThemeMode.DARK else UITheme.ThemeMode.LIGHT
        UITheme.setTheme(themeMode)
        
        // UI 전체 업데이트
        refreshUI()
        
        val statusText = if (newSettings.isDarkMode) "다크 모드 적용" else "라이트 모드 적용"
        showMessage(statusText)
    }
    
    private fun updateDarkModeButton(button: ModernButton) {
        val isDark = com.omok.presentation.ui.settings.UIGameSettings.getInstance().isDarkMode
        button.text = if (isDark) "라이트" else "다크"
    }
    
    private fun refreshUI() {
        // 전체 UI 컴포넌트 색상 업데이트
        updateComponentColors(this)
        repaint()
        revalidate()
    }
    
    private fun updateComponentColors(component: java.awt.Component) {
        when (component) {
            is javax.swing.JPanel -> {
                component.background = UITheme.Colors.BACKGROUND
                for (child in component.components) {
                    updateComponentColors(child)
                }
            }
            is javax.swing.JLabel -> {
                component.foreground = UITheme.Colors.TEXT_PRIMARY
            }
            is javax.swing.JButton -> {
                component.background = UITheme.Colors.SURFACE
                component.foreground = UITheme.Colors.TEXT_PRIMARY
            }
        }
        
        if (component is java.awt.Container) {
            for (child in component.components) {
                updateComponentColors(child)
            }
        }
    }
    
    private fun toggleSound() {
        val currentSettings = com.omok.presentation.ui.settings.UIGameSettings.getInstance()
        val newSettings = currentSettings.copy(soundEnabled = !currentSettings.soundEnabled)
        com.omok.presentation.ui.settings.UIGameSettings.updateSettings(newSettings)
        
        com.omok.presentation.ui.effects.SoundEffects.setSoundEnabled(newSettings.soundEnabled)
        
        val statusText = if (newSettings.soundEnabled) "사운드 효과 켜짐" else "사운드 효과 꺼짐"
        showMessage(statusText)
        
        // 버튼 클릭 사운드
        if (newSettings.soundEnabled) {
            com.omok.presentation.ui.effects.SoundEffects.playButtonClick()
        }
    }
    
    private fun updateSoundButton(button: ModernButton) {
        val isEnabled = com.omok.presentation.ui.settings.UIGameSettings.getInstance().soundEnabled
        button.text = if (isEnabled) "사운드 ON" else "사운드 OFF"
    }
    
    fun exportToSGF() {
        val currentGame = gameController.getCurrentGame()
        if (currentGame == null) {
            showMessage("내보낼 게임이 없습니다.")
            return
        }
        
        // Domain 모델로 변환
        val domainGame = com.omok.application.mapper.GameMapper.toDomain(currentGame)
        
        val fileChooser = JFileChooser()
        fileChooser.dialogTitle = "SGF 파일로 내보내기"
        fileChooser.currentDirectory = java.io.File("exports")
        
        // 기본 파일명 설정
        val exporter = com.omok.infrastructure.export.SGFExporter()
        val defaultFileName = exporter.generateDefaultFileName(domainGame)
        fileChooser.selectedFile = java.io.File(defaultFileName)
        
        fileChooser.fileFilter = object : javax.swing.filechooser.FileFilter() {
            override fun accept(f: java.io.File): Boolean {
                return f.isDirectory || f.name.lowercase().endsWith(".sgf")
            }
            override fun getDescription(): String = "SGF 파일 (*.sgf)"
        }
        
        val result = fileChooser.showSaveDialog(this)
        if (result == JFileChooser.APPROVE_OPTION) {
            var file = fileChooser.selectedFile
            
            // .sgf 확장자 자동 추가
            if (!file.name.lowercase().endsWith(".sgf")) {
                file = java.io.File(file.parent, file.name + ".sgf")
            }
            
            try {
                // exports 디렉토리 생성
                file.parentFile?.mkdirs()
                
                // SGF 내용 생성
                val sgfContent = exporter.exportToSGF(domainGame)
                
                // 파일 저장
                file.writeText(sgfContent, Charsets.UTF_8)
                
                val message = buildString {
                    append("게임이 SGF 형식으로 내보내기 되었습니다.\\n")
                    append("파일: ${file.absolutePath}\\n")
                    append("총 ${currentGame.moveHistory.size}수")
                }
                showMessage(message)
                
                // 내보내기 사운드
                com.omok.presentation.ui.effects.SoundEffects.playButtonClick()
                
            } catch (e: Exception) {
                showMessage("SGF 파일 내보내기에 실패했습니다: ${e.message}")
            }
        }
    }
    
    private fun setupReplayPanel() {
        // 재생 패널 콜백 설정
        gameReplayPanel.setOnMoveChangedListener { moveIndex ->
            updateBoardForReplay(moveIndex)
        }
        
        gameReplayPanel.setOnReplayModeChangedListener { isReplayMode ->
            if (!isReplayMode) {
                // 재생 모드 종료 시 현재 게임 상태로 복원
                val currentGame = gameController.getCurrentGame()
                if (currentGame != null) {
                    updateBoard(currentGame)
                    updateTimer(currentGame)
                }
            }
        }
    }
    
    fun toggleReplayMode() {
        val currentGame = gameController.getCurrentGame()
        if (currentGame == null) {
            showMessage("재생할 게임이 없습니다.")
            return
        }
        
        if (gameReplayPanel.isInReplayMode()) {
            // 재생 모드 종료
            gameReplayPanel.setReplayMode(false)
            pack() // 크기 재조정
        } else {
            // 재생 모드 시작
            if (currentGame.moveHistory.isEmpty()) {
                showMessage("재생할 수순이 없습니다.")
                return
            }
            
            gameReplayPanel.setGameHistory(currentGame.moveHistory)
            gameReplayPanel.setReplayMode(true)
            pack() // 크기 재조정
        }
    }
    
    private fun updateBoardForReplay(moveIndex: Int) {
        val currentGame = gameController.getCurrentGame() ?: return
        
        // 지정된 수까지의 보드 상태 재구성
        val replayBoard = Array(15) { Array<PlayerDto?>(15) { null } }
        
        // moveIndex까지의 수들을 보드에 배치
        for (i in 0..moveIndex) {
            if (i < currentGame.moveHistory.size) {
                val move = currentGame.moveHistory[i]
                replayBoard[move.position.row][move.position.col] = move.player
            }
        }
        
        val replayBoardDto = BoardDto(replayBoard)
        
        // 보드 업데이트
        gameBoard.updateBoard(replayBoardDto)
        
        // 현재 수 표시
        if (moveIndex >= 0 && moveIndex < currentGame.moveHistory.size) {
            val currentMove = currentGame.moveHistory[moveIndex]
            gameBoard.setLastMove(currentMove.position)
        } else {
            gameBoard.clearLastMove()
        }
        
        // 상태 업데이트
        if (moveIndex < 0) {
            updateStatus("게임 시작 - 흑돌 차례입니다.")
        } else if (moveIndex < currentGame.moveHistory.size) {
            val move = currentGame.moveHistory[moveIndex]
            val coordinate = "${'A' + move.position.col}${15 - move.position.row}"
            val playerName = when (move.player) {
                PlayerDto.BLACK -> "흑돌"
                PlayerDto.WHITE -> "백돌"
            }
            updateStatus("${move.moveNumber}수: $coordinate ($playerName)")
        }
    }
    
    private fun toggleAISpeed() {
        val currentSettings = com.omok.presentation.ui.settings.UIGameSettings.getInstance()
        val currentSpeed = currentSettings.aiThinkingTime
        
        // 다음 속도로 순환
        val speeds = com.omok.presentation.ui.settings.AIThinkingTime.values()
        val currentIndex = speeds.indexOf(currentSpeed)
        val nextIndex = (currentIndex + 1) % speeds.size
        val nextSpeed = speeds[nextIndex]
        
        val newSettings = currentSettings.copy(aiThinkingTime = nextSpeed)
        com.omok.presentation.ui.settings.UIGameSettings.updateSettings(newSettings)
        
        // 컨트롤러에 AI 사고시간 업데이트 전달
        gameController.updateAIThinkingTime(nextSpeed.milliseconds)
        
        showMessage("AI 사고시간: ${nextSpeed.displayName} (${nextSpeed.description})")
        
        // 사운드 피드백
        com.omok.presentation.ui.effects.SoundEffects.playButtonClick()
    }
    
    private fun updateAISpeedButton(button: ModernButton) {
        val currentSpeed = com.omok.presentation.ui.settings.UIGameSettings.getInstance().aiThinkingTime
        button.text = currentSpeed.displayName
    }
    
    fun analyzeGame() {
        val currentGame = gameController.getCurrentGame()
        if (currentGame == null) {
            showMessage("분석할 게임이 없습니다.")
            return
        }
        
        if (currentGame.moveHistory.isEmpty()) {
            showMessage("분석할 수가 없습니다.")
            return
        }
        
        // Domain 모델로 변환
        val domainGame = com.omok.application.mapper.GameMapper.toDomain(currentGame)
        
        // 분석 다이얼로그 표시
        val dialog = com.omok.presentation.ui.analysis.GameAnalysisDialog(this, domainGame)
        dialog.isVisible = true
    }
    
    fun showPuzzleDialog() {
        // 퍼즐 목록 다이얼로그 표시
        val puzzleListDialog = com.omok.presentation.ui.puzzle.PuzzleListDialog(this) { puzzle ->
            // 선택된 퍼즐로 풀기 다이얼로그 열기
            val solveDialog = com.omok.presentation.ui.puzzle.PuzzleSolveDialog(this, puzzle)
            solveDialog.isVisible = true
        }
        puzzleListDialog.isVisible = true
    }
    
    fun showTournamentDialog() {
        val options = arrayOf("새 토너먼트", "진행 중인 토너먼트")
        val (result, selected) = UnifiedDialog.showSelection(
            this,
            "토너먼트",
            "토너먼트 옵션을 선택하세요:",
            options,
            options[0]
        )
        
        if (result != DialogResult.CONFIRMED || selected == null) return
        
        val choice = options.indexOf(selected)
        
        when (choice) {
            0 -> {
                // 새 토너먼트 생성
                val creationDialog = com.omok.presentation.ui.tournament.TournamentCreationDialog(this)
                creationDialog.isVisible = true
                
                creationDialog.getCreatedTournament()?.let { tournament ->
                    // 토너먼트 시작
                    val manager = com.omok.infrastructure.tournament.TournamentManager.getInstance()
                    manager.startTournament(tournament.id)?.let { startedTournament ->
                        // 토너먼트 제어 다이얼로그 표시
                        val controlDialog = com.omok.presentation.ui.tournament.TournamentControlDialog(this, startedTournament)
                        controlDialog.isVisible = true
                    }
                }
            }
            1 -> {
                // 진행 중인 토너먼트 선택
                val manager = com.omok.infrastructure.tournament.TournamentManager.getInstance()
                val activeTournament = manager.getActiveTournament()
                
                if (activeTournament != null) {
                    val controlDialog = com.omok.presentation.ui.tournament.TournamentControlDialog(this, activeTournament)
                    controlDialog.isVisible = true
                } else {
                    // 토너먼트 목록에서 선택
                    val tournaments = manager.getAllTournaments()
                        .filter { it.status != com.omok.domain.tournament.TournamentStatus.COMPLETED }
                    
                    if (tournaments.isEmpty()) {
                        UnifiedDialog.showInfo(
                            this,
                            "토너먼트",
                            "진행 중인 토너먼트가 없습니다."
                        )
                    } else {
                        val tournamentNames = tournaments.map { it.name }.toTypedArray()
                        val (selectionResult, selected) = UnifiedDialog.showSelection(
                            this,
                            "토너먼트 선택",
                            "토너먼트를 선택하세요:",
                            tournamentNames,
                            tournamentNames[0]
                        )
                        
                        if (selectionResult == DialogResult.CONFIRMED && selected != null) {
                            tournaments.find { it.name == selected }?.let { tournament ->
                                val controlDialog = com.omok.presentation.ui.tournament.TournamentControlDialog(this, tournament)
                                controlDialog.isVisible = true
                            }
                        }
                    }
                }
            }
        }
    }
    
    /**
     * 테마 선택 다이얼로그 표시
     */
    fun showThemeSelectionDialog() {
        val dialog = ThemeSelectionDialog(this)
        dialog.isVisible = true
    }
    
    /**
     * 테마 변경 리스너 구현
     */
    override fun onThemeChanged(oldTheme: GameTheme, newTheme: GameTheme) {
        // UI 요소들의 색상 업데이트
        background = newTheme.colorScheme.background
        
        // 메뉴바 업데이트
        jMenuBar?.background = newTheme.colorScheme.surface
        
        // 전체 UI 새로고침
        SwingUtilities.invokeLater {
            repaint()
            revalidate()
        }
    }
}