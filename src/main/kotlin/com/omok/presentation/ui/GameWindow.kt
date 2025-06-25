package com.omok.presentation.ui

import com.omok.application.dto.*
import com.omok.presentation.controller.GameController
import com.omok.presentation.ui.theme.UITheme
import com.omok.presentation.ui.components.*
import com.omok.presentation.ui.dialogs.SettingsDialog
import com.omok.presentation.ui.accessibility.KeyboardShortcuts
import java.awt.*
import java.awt.event.ActionEvent
import javax.swing.*
import javax.swing.border.EmptyBorder

class GameWindow : JFrame() {
    private lateinit var gameBoard: GameBoardPanel
    private lateinit var controller: GameController
    private val gameHeader = GameHeader()
    private val statusPanel = StatusPanel()
    private val gameInfoPanel = GameInfoPanel()
    private lateinit var undoButton: ModernButton
    private lateinit var newGameButton: ModernButton
    private lateinit var settingsButton: ModernButton
    private var keyboardFocusPosition = PositionDto(7, 7)  // Center of board
    
    init {
        title = "렌주룰 오목 - Renju Gomoku"
        initializeUI()
        setupKeyboardShortcuts()
    }
    
    fun setController(controller: GameController) {
        this.controller = controller
        gameBoard.setController(controller)
    }
    
    private fun initializeUI() {
        defaultCloseOperation = EXIT_ON_CLOSE
        layout = BorderLayout()
        
        UITheme.applyTheme()
        
        gameBoard = GameBoardPanel()
        
        createMenuBar()
        
        val mainPanel = JPanel(BorderLayout())
        mainPanel.background = UITheme.Colors.BACKGROUND
        
        // 헤더 추가 (preferredSize는 GameHeader에서 설정)
        mainPanel.add(gameHeader, BorderLayout.NORTH)
        
        val centerPanel = JPanel(BorderLayout())
        centerPanel.background = UITheme.Colors.SURFACE
        centerPanel.border = EmptyBorder(5, 10, 5, 10)
        
        // Info panel at top
        val topInfoPanel = JPanel(BorderLayout())
        topInfoPanel.background = UITheme.Colors.SURFACE
        topInfoPanel.preferredSize = Dimension(0, 80)
        topInfoPanel.border = EmptyBorder(5, 10, 5, 10)
        topInfoPanel.add(gameInfoPanel, BorderLayout.CENTER)
        centerPanel.add(topInfoPanel, BorderLayout.NORTH)
        
        // Board in center with wrapper for proper centering
        val boardWrapper = JPanel(GridBagLayout())
        boardWrapper.background = UITheme.Colors.SURFACE
        boardWrapper.add(gameBoard)
        centerPanel.add(boardWrapper, BorderLayout.CENTER)
        
        mainPanel.add(centerPanel, BorderLayout.CENTER)
        
        val bottomPanel = createBottomPanel()
        mainPanel.add(bottomPanel, BorderLayout.SOUTH)
        
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
        val menuBar = JMenuBar()
        menuBar.background = UITheme.Colors.SURFACE
        menuBar.border = BorderFactory.createMatteBorder(0, 0, 1, 0, UITheme.Colors.GRAY_200)
        
        val gameMenu = JMenu("게임")
        gameMenu.mnemonic = 'G'.code
        
        val newPvPItem = JMenuItem("새 게임 (사람 vs 사람)")
        newPvPItem.accelerator = KeyStroke.getKeyStroke("ctrl N")
        newPvPItem.addActionListener { startNewGame(GameModeDto.PLAYER_VS_PLAYER) }
        
        val newPvEMenu = JMenu("새 게임 (사람 vs AI)")
        
        val easyItem = JMenuItem("쉬움")
        easyItem.addActionListener { startNewGame(GameModeDto.PLAYER_VS_AI, AIDifficultyDto.EASY) }
        
        val mediumItem = JMenuItem("보통")
        mediumItem.addActionListener { startNewGame(GameModeDto.PLAYER_VS_AI, AIDifficultyDto.MEDIUM) }
        
        val hardItem = JMenuItem("어려움")
        hardItem.addActionListener { startNewGame(GameModeDto.PLAYER_VS_AI, AIDifficultyDto.HARD) }
        
        newPvEMenu.add(easyItem)
        newPvEMenu.add(mediumItem)
        newPvEMenu.add(hardItem)
        
        val exitItem = JMenuItem("종료")
        exitItem.accelerator = KeyStroke.getKeyStroke("ctrl Q")
        exitItem.addActionListener { System.exit(0) }
        
        gameMenu.add(newPvPItem)
        gameMenu.add(newPvEMenu)
        gameMenu.addSeparator()
        gameMenu.add(exitItem)
        
        val helpMenu = JMenu("도움말")
        helpMenu.mnemonic = 'H'.code
        
        val rulesItem = JMenuItem("렌주룰 설명")
        rulesItem.addActionListener { showRulesDialog() }
        
        val aboutItem = JMenuItem("정보")
        aboutItem.addActionListener { showAboutDialog() }
        
        helpMenu.add(rulesItem)
        helpMenu.add(aboutItem)
        
        menuBar.add(gameMenu)
        menuBar.add(helpMenu)
        
        jMenuBar = menuBar
    }
    
    
    private fun createBottomPanel(): JPanel {
        val panel = JPanel(BorderLayout())
        panel.background = UITheme.Colors.BACKGROUND
        panel.border = EmptyBorder(UITheme.Spacing.SM, UITheme.Spacing.MD, UITheme.Spacing.SM, UITheme.Spacing.MD)
        panel.preferredSize = Dimension(0, 70)
        
        panel.add(statusPanel, BorderLayout.CENTER)
        
        val buttonPanel = JPanel(FlowLayout(FlowLayout.RIGHT, UITheme.Spacing.SM, 0))
        buttonPanel.background = UITheme.Colors.BACKGROUND
        
        undoButton = ModernButton("무르기", ModernButton.ButtonStyle.SECONDARY)
        undoButton.isEnabled = false
        undoButton.toolTipText = "마지막 수를 취소합니다 (Ctrl+Z)"
        undoButton.addActionListener { controller.undoMove() }
        
        newGameButton = ModernButton("새 게임", ModernButton.ButtonStyle.PRIMARY)
        newGameButton.toolTipText = "새로운 게임을 시작합니다 (Ctrl+N)"
        newGameButton.addActionListener { showNewGameDialog() }
        
        settingsButton = ModernButton("설정", ModernButton.ButtonStyle.GHOST)
        settingsButton.toolTipText = "설정 (Ctrl+,)"
        settingsButton.addActionListener { showSettingsDialog() }
        
        buttonPanel.add(settingsButton)
        buttonPanel.add(undoButton)
        buttonPanel.add(newGameButton)
        
        panel.add(buttonPanel, BorderLayout.EAST)
        
        return panel
    }
    
    
    private fun showNewGameDialog() {
        // 먼저 게임 모드 선택
        val modeOptions = arrayOf("사람 vs 사람", "사람 vs AI (쉬움)", "사람 vs AI (보통)", "사람 vs AI (어려움)")
        val modeChoice = ModernDialog.showOptions(
            this,
            "새 게임",
            "게임 모드를 선택하세요:",
            modeOptions,
            modeOptions[0]
        )
        
        if (modeChoice == null) return
        
        // 게임 규칙 선택
        val ruleOptions = GameRuleDto.values().map { it.displayName }.toTypedArray()
        val ruleChoice = ModernDialog.showOptions(
            this,
            "게임 규칙",
            "게임 규칙을 선택하세요:",
            ruleOptions,
            ruleOptions[0]
        )
        
        if (ruleChoice == null) return
        
        val selectedRule = GameRuleDto.values().first { it.displayName == ruleChoice }
        
        when (modeChoice) {
            "사람 vs 사람" -> startNewGame(GameModeDto.PLAYER_VS_PLAYER, null, selectedRule)
            "사람 vs AI (쉬움)" -> startNewGame(GameModeDto.PLAYER_VS_AI, AIDifficultyDto.EASY, selectedRule)
            "사람 vs AI (보통)" -> startNewGame(GameModeDto.PLAYER_VS_AI, AIDifficultyDto.MEDIUM, selectedRule)
            "사람 vs AI (어려움)" -> startNewGame(GameModeDto.PLAYER_VS_AI, AIDifficultyDto.HARD, selectedRule)
        }
    }
    
    private fun startNewGame(mode: GameModeDto, difficulty: AIDifficultyDto? = null, rule: GameRuleDto = GameRuleDto.STANDARD_RENJU) {
        controller.startNewGame(mode, difficulty, rule)
        gameInfoPanel.resetInfo()
        gameInfoPanel.startTimer()
        
        // Update header
        val modeText = if (mode == GameModeDto.PLAYER_VS_PLAYER) "사람 vs 사람" else "사람 vs AI"
        val diffText = when (difficulty) {
            AIDifficultyDto.EASY -> "쉬움"
            AIDifficultyDto.MEDIUM -> "보통"
            AIDifficultyDto.HARD -> "어려움"
            else -> ""
        }
        gameHeader.setGameMode(modeText, diffText)
    }
    
    private fun showSettingsDialog() {
        val dialog = SettingsDialog(this)
        dialog.isVisible = true
    }
    
    private fun setupKeyboardShortcuts() {
        KeyboardShortcuts.setupShortcuts(rootPane, object : KeyboardShortcuts.ShortcutActions {
            override fun onNewGame() {
                showNewGameDialog()
            }
            
            override fun onUndo() {
                if (undoButton.isEnabled) {
                    controller.undoMove()
                }
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
                controller.makeMove(keyboardFocusPosition)
            }
        })
    }
    
    private fun showRulesDialog() {
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
        
        ModernDialog.showInfo(this, "렌주룰 설명", rulesText)
    }
    
    private fun showAboutDialog() {
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
        
        ModernDialog.showInfo(this, "정보", aboutText)
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
        undoButton.isEnabled = enabled
    }
    
    fun showMessage(message: String) {
        ModernDialog.showWarning(this, "알림", message)
    }
    
    fun showWinAnimation(winningLine: List<PositionDto>) {
        gameBoard.showWinAnimation(winningLine)
        gameInfoPanel.stopTimer()
    }
}