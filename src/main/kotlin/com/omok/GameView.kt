package com.omok

import java.awt.*
import java.awt.event.ActionEvent
import javax.swing.*
import javax.swing.border.EmptyBorder

class GameView : JFrame("렌주룰 오목") {
    private lateinit var gameBoard: GameBoard
    private lateinit var gameController: GameController
    private val statusLabel = JLabel("게임을 시작하세요", SwingConstants.CENTER)
    private lateinit var undoButton: JButton
    private lateinit var newGameButton: JButton
    
    init {
        initializeUI()
    }
    
    private fun initializeUI() {
        defaultCloseOperation = EXIT_ON_CLOSE
        layout = BorderLayout()
        
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
        
        gameController = GameController(this)
        gameBoard = GameBoard(gameController)
        
        createMenuBar()
        
        val mainPanel = JPanel(BorderLayout())
        mainPanel.background = Color(240, 240, 240)
        
        val topPanel = createTopPanel()
        mainPanel.add(topPanel, BorderLayout.NORTH)
        
        val boardContainer = JPanel(GridBagLayout())
        boardContainer.background = Color(240, 240, 240)
        boardContainer.border = EmptyBorder(20, 20, 20, 20)
        boardContainer.add(gameBoard)
        mainPanel.add(boardContainer, BorderLayout.CENTER)
        
        val bottomPanel = createBottomPanel()
        mainPanel.add(bottomPanel, BorderLayout.SOUTH)
        
        add(mainPanel)
        
        pack()
        setLocationRelativeTo(null)
        isResizable = false
    }
    
    private fun createMenuBar() {
        val menuBar = JMenuBar()
        
        val gameMenu = JMenu("게임")
        gameMenu.mnemonic = 'G'.toInt()
        
        val newPvPItem = JMenuItem("새 게임 (사람 vs 사람)")
        newPvPItem.accelerator = KeyStroke.getKeyStroke("ctrl N")
        newPvPItem.addActionListener { startNewGame(GameController.GameMode.PVP) }
        
        val newPvEMenu = JMenu("새 게임 (사람 vs AI)")
        
        val easyItem = JMenuItem("쉬움")
        easyItem.addActionListener { startNewGame(GameController.GameMode.PVE, AI.Difficulty.EASY) }
        
        val mediumItem = JMenuItem("보통")
        mediumItem.addActionListener { startNewGame(GameController.GameMode.PVE, AI.Difficulty.MEDIUM) }
        
        val hardItem = JMenuItem("어려움")
        hardItem.addActionListener { startNewGame(GameController.GameMode.PVE, AI.Difficulty.HARD) }
        
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
        helpMenu.mnemonic = 'H'.toInt()
        
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
    
    private fun createTopPanel(): JPanel {
        val panel = JPanel(BorderLayout())
        panel.background = Color(250, 250, 250)
        panel.border = BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, Color(200, 200, 200)),
            EmptyBorder(15, 20, 15, 20)
        )
        
        val titleLabel = JLabel("렌주룰 오목", SwingConstants.CENTER)
        titleLabel.font = Font("맑은 고딕", Font.BOLD, 24)
        titleLabel.foreground = Color(50, 50, 50)
        
        panel.add(titleLabel, BorderLayout.CENTER)
        
        return panel
    }
    
    private fun createBottomPanel(): JPanel {
        val panel = JPanel(BorderLayout())
        panel.background = Color(250, 250, 250)
        panel.border = BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, Color(200, 200, 200)),
            EmptyBorder(15, 20, 15, 20)
        )
        
        statusLabel.font = Font("맑은 고딕", Font.PLAIN, 16)
        statusLabel.foreground = Color(70, 70, 70)
        panel.add(statusLabel, BorderLayout.CENTER)
        
        val buttonPanel = JPanel(FlowLayout(FlowLayout.RIGHT, 10, 0))
        buttonPanel.background = Color(250, 250, 250)
        
        undoButton = createStyledButton("무르기", Color(108, 117, 125))
        undoButton.isEnabled = false
        undoButton.addActionListener { gameController.undoMove() }
        
        newGameButton = createStyledButton("새 게임", Color(40, 167, 69))
        newGameButton.addActionListener { showNewGameDialog() }
        
        buttonPanel.add(undoButton)
        buttonPanel.add(newGameButton)
        
        panel.add(buttonPanel, BorderLayout.EAST)
        
        return panel
    }
    
    private fun createStyledButton(text: String, color: Color): JButton {
        val button = JButton(text)
        button.font = Font("맑은 고딕", Font.BOLD, 14)
        button.foreground = Color.WHITE
        button.background = color
        button.isFocusPainted = false
        button.border = BorderFactory.createEmptyBorder(8, 20, 8, 20)
        button.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        
        button.addMouseListener(object : java.awt.event.MouseAdapter() {
            override fun mouseEntered(e: java.awt.event.MouseEvent) {
                button.background = color.darker()
            }
            
            override fun mouseExited(e: java.awt.event.MouseEvent) {
                button.background = color
            }
        })
        
        return button
    }
    
    private fun showNewGameDialog() {
        val options = arrayOf("사람 vs 사람", "사람 vs AI (쉬움)", "사람 vs AI (보통)", "사람 vs AI (어려움)")
        val choice = JOptionPane.showOptionDialog(
            this,
            "게임 모드를 선택하세요:",
            "새 게임",
            JOptionPane.DEFAULT_OPTION,
            JOptionPane.QUESTION_MESSAGE,
            null,
            options,
            options[0]
        )
        
        when (choice) {
            0 -> startNewGame(GameController.GameMode.PVP)
            1 -> startNewGame(GameController.GameMode.PVE, AI.Difficulty.EASY)
            2 -> startNewGame(GameController.GameMode.PVE, AI.Difficulty.MEDIUM)
            3 -> startNewGame(GameController.GameMode.PVE, AI.Difficulty.HARD)
        }
    }
    
    private fun startNewGame(mode: GameController.GameMode, difficulty: AI.Difficulty? = null) {
        gameBoard.reset()
        gameController.startNewGame(mode, difficulty)
    }
    
    private fun showRulesDialog() {
        val rulesText = """
            <html>
            <body style='width: 400px; padding: 10px;'>
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
        
        JOptionPane.showMessageDialog(this, rulesText, "렌주룰 설명", JOptionPane.INFORMATION_MESSAGE)
    }
    
    private fun showAboutDialog() {
        val aboutText = """
            <html>
            <body style='width: 300px; padding: 10px; text-align: center;'>
            <h2>렌주룰 오목</h2>
            <p>버전 1.0</p>
            <p>Kotlin과 Swing으로 제작된 렌주룰 오목 게임입니다.</p>
            <br>
            <p>AI는 미니맥스 알고리즘을 사용하여 구현되었습니다.</p>
            </body>
            </html>
        """.trimIndent()
        
        JOptionPane.showMessageDialog(this, aboutText, "정보", JOptionPane.INFORMATION_MESSAGE)
    }
    
    fun updateBoard(board: Array<Array<Stone>>) {
        gameBoard.updateBoard(board)
    }
    
    fun updateStatus(status: String) {
        statusLabel.text = status
    }
    
    fun setLastMove(row: Int, col: Int) {
        gameBoard.setLastMove(row, col)
    }
    
    fun setUndoEnabled(enabled: Boolean) {
        undoButton.isEnabled = enabled
    }
    
    fun showMessage(message: String) {
        JOptionPane.showMessageDialog(this, message, "알림", JOptionPane.WARNING_MESSAGE)
    }
    
    fun showWinAnimation(row: Int, col: Int) {
        Timer(100) { e ->
            gameBoard.repaint()
            (e.source as Timer).stop()
        }.start()
    }
}