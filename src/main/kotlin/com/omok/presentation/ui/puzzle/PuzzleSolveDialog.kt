package com.omok.presentation.ui.puzzle

import com.omok.application.dto.BoardDto
import com.omok.application.dto.PlayerDto
import com.omok.application.dto.PositionDto
import com.omok.application.mapper.GameMapper
import com.omok.application.usecase.puzzle.*
import com.omok.domain.model.Position
import com.omok.domain.puzzle.*
import com.omok.infrastructure.puzzle.InMemoryPuzzleRepository
import com.omok.presentation.ui.GameBoardPanel
import com.omok.presentation.ui.components.ModernButton
import com.omok.presentation.ui.theme.UITheme
import com.omok.presentation.ui.effects.SoundEffects
import java.awt.*
import javax.swing.*
import javax.swing.border.EmptyBorder
import javax.swing.border.TitledBorder

/**
 * 퍼즐 풀기 다이얼로그
 */
class PuzzleSolveDialog(
    parent: Frame,
    private val puzzle: OmokPuzzle
) : JDialog(parent, "퍼즐: ${puzzle.title}", true) {
    
    private val solvePuzzleUseCase = SolvePuzzleUseCase(
        puzzleRepository = InMemoryPuzzleRepository(),
        eventBus = com.omok.domain.event.GameEventBus
    )
    
    private lateinit var boardPanel: GameBoardPanel
    private lateinit var infoPanel: JPanel
    private lateinit var hintButton: ModernButton
    private lateinit var resetButton: ModernButton
    private lateinit var solutionButton: ModernButton
    private lateinit var statusLabel: JLabel
    private lateinit var attemptsLabel: JLabel
    private lateinit var hintsUsedLabel: JLabel
    private lateinit var timerLabel: JLabel
    
    private var timerThread: Thread? = null
    
    init {
        initializeUI()
        startPuzzle()
    }
    
    private fun initializeUI() {
        layout = BorderLayout()
        preferredSize = Dimension(900, 700)
        
        val contentPanel = JPanel(BorderLayout())
        contentPanel.background = UITheme.Colors.BACKGROUND
        contentPanel.border = EmptyBorder(UITheme.Spacing.MD, UITheme.Spacing.MD, UITheme.Spacing.MD, UITheme.Spacing.MD)
        
        // 상단: 퍼즐 정보
        contentPanel.add(createPuzzleInfoPanel(), BorderLayout.NORTH)
        
        // 중앙: 게임 보드와 정보 패널
        val centerPanel = JPanel(BorderLayout())
        centerPanel.background = UITheme.Colors.BACKGROUND
        
        // 보드
        boardPanel = GameBoardPanel()
        boardPanel.preferredSize = Dimension(600, 600)
        boardPanel.addBoardClickListener { position: PositionDto ->
            handleBoardClick(position)
        }
        
        val boardWrapper = JPanel(GridBagLayout())
        boardWrapper.background = UITheme.Colors.SURFACE
        boardWrapper.border = BorderFactory.createLineBorder(UITheme.Colors.GRAY_300)
        boardWrapper.add(boardPanel)
        
        centerPanel.add(boardWrapper, BorderLayout.CENTER)
        
        // 우측: 정보 패널
        infoPanel = createInfoPanel()
        centerPanel.add(infoPanel, BorderLayout.EAST)
        
        contentPanel.add(centerPanel, BorderLayout.CENTER)
        
        // 하단: 버튼 패널
        contentPanel.add(createButtonPanel(), BorderLayout.SOUTH)
        
        add(contentPanel)
        
        pack()
        setLocationRelativeTo(parent)
    }
    
    private fun createPuzzleInfoPanel(): JPanel {
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
        panel.background = UITheme.Colors.SURFACE
        panel.border = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UITheme.Colors.GRAY_300),
            EmptyBorder(UITheme.Spacing.MD, UITheme.Spacing.MD, UITheme.Spacing.MD, UITheme.Spacing.MD)
        )
        
        // 제목
        val titleLabel = JLabel(puzzle.title)
        titleLabel.font = UITheme.Fonts.HEADING
        titleLabel.alignmentX = Component.LEFT_ALIGNMENT
        panel.add(titleLabel)
        
        panel.add(Box.createVerticalStrut(UITheme.Spacing.SM))
        
        // 설명
        val descLabel = JLabel(puzzle.description)
        descLabel.font = UITheme.Fonts.BODY
        descLabel.foreground = UITheme.Colors.GRAY_600
        descLabel.alignmentX = Component.LEFT_ALIGNMENT
        panel.add(descLabel)
        
        panel.add(Box.createVerticalStrut(UITheme.Spacing.SM))
        
        // 메타 정보
        val metaPanel = JPanel(FlowLayout(FlowLayout.LEFT, UITheme.Spacing.LG, 0))
        metaPanel.background = UITheme.Colors.SURFACE
        metaPanel.alignmentX = Component.LEFT_ALIGNMENT
        
        metaPanel.add(createMetaLabel("카테고리", puzzle.category.displayName))
        metaPanel.add(createMetaLabel("난이도", puzzle.difficulty.displayName))
        metaPanel.add(createMetaLabel("목표", puzzle.objective.displayName))
        
        panel.add(metaPanel)
        
        return panel
    }
    
    private fun createMetaLabel(label: String, value: String): JPanel {
        val panel = JPanel(FlowLayout(FlowLayout.LEFT, UITheme.Spacing.XS, 0))
        panel.background = UITheme.Colors.SURFACE
        
        val labelComponent = JLabel("$label:")
        labelComponent.font = UITheme.Fonts.CAPTION
        labelComponent.foreground = UITheme.Colors.GRAY_500
        
        val valueComponent = JLabel(value)
        valueComponent.font = UITheme.Fonts.BUTTON
        valueComponent.foreground = UITheme.Colors.PRIMARY
        
        panel.add(labelComponent)
        panel.add(valueComponent)
        
        return panel
    }
    
    private fun createInfoPanel(): JPanel {
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
        panel.background = UITheme.Colors.BACKGROUND
        panel.preferredSize = Dimension(250, 0)
        panel.border = EmptyBorder(0, UITheme.Spacing.MD, 0, 0)
        
        // 상태 패널
        val statusPanel = JPanel()
        statusPanel.layout = BoxLayout(statusPanel, BoxLayout.Y_AXIS)
        statusPanel.background = UITheme.Colors.SURFACE
        statusPanel.border = BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(UITheme.Colors.GRAY_300),
                "진행 상태",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                UITheme.Fonts.BUTTON,
                UITheme.Colors.GRAY_700
            ),
            EmptyBorder(UITheme.Spacing.SM, UITheme.Spacing.MD, UITheme.Spacing.SM, UITheme.Spacing.MD)
        )
        statusPanel.alignmentX = Component.LEFT_ALIGNMENT
        
        statusLabel = JLabel("퍼즐을 시작하세요")
        statusLabel.font = UITheme.Fonts.BODY
        statusLabel.alignmentX = Component.LEFT_ALIGNMENT
        statusPanel.add(statusLabel)
        
        statusPanel.add(Box.createVerticalStrut(UITheme.Spacing.SM))
        
        attemptsLabel = JLabel("시도: 0회")
        attemptsLabel.font = UITheme.Fonts.CAPTION
        attemptsLabel.alignmentX = Component.LEFT_ALIGNMENT
        statusPanel.add(attemptsLabel)
        
        hintsUsedLabel = JLabel("힌트 사용: 0/${puzzle.hints.size}")
        hintsUsedLabel.font = UITheme.Fonts.CAPTION
        hintsUsedLabel.alignmentX = Component.LEFT_ALIGNMENT
        statusPanel.add(hintsUsedLabel)
        
        timerLabel = JLabel("시간: 00:00")
        timerLabel.font = UITheme.Fonts.CAPTION
        timerLabel.alignmentX = Component.LEFT_ALIGNMENT
        statusPanel.add(timerLabel)
        
        panel.add(statusPanel)
        
        panel.add(Box.createVerticalStrut(UITheme.Spacing.MD))
        
        // 힌트 패널
        val hintPanel = JPanel()
        hintPanel.layout = BoxLayout(hintPanel, BoxLayout.Y_AXIS)
        hintPanel.background = UITheme.Colors.SURFACE
        hintPanel.border = BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(UITheme.Colors.GRAY_300),
                "힌트",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                UITheme.Fonts.BUTTON,
                UITheme.Colors.GRAY_700
            ),
            EmptyBorder(UITheme.Spacing.SM, UITheme.Spacing.MD, UITheme.Spacing.SM, UITheme.Spacing.MD)
        )
        hintPanel.alignmentX = Component.LEFT_ALIGNMENT
        hintPanel.preferredSize = Dimension(250, 150)
        
        val hintArea = JTextArea()
        hintArea.isEditable = false
        hintArea.font = UITheme.Fonts.CAPTION
        hintArea.background = UITheme.Colors.SURFACE
        hintArea.lineWrap = true
        hintArea.wrapStyleWord = true
        
        val hintScroll = JScrollPane(hintArea)
        hintScroll.border = null
        hintPanel.add(hintScroll)
        
        panel.add(hintPanel)
        
        return panel
    }
    
    private fun createButtonPanel(): JPanel {
        val panel = JPanel(FlowLayout(FlowLayout.CENTER, UITheme.Spacing.MD, 0))
        panel.background = UITheme.Colors.BACKGROUND
        panel.border = EmptyBorder(UITheme.Spacing.SM, 0, 0, 0)
        
        hintButton = ModernButton("힌트 (${puzzle.hints.size})", ModernButton.ButtonStyle.SECONDARY)
        hintButton.addActionListener { requestHint() }
        
        resetButton = ModernButton("다시 시작", ModernButton.ButtonStyle.SECONDARY)
        resetButton.addActionListener { resetPuzzle() }
        
        solutionButton = ModernButton("해답 보기", ModernButton.ButtonStyle.SECONDARY)
        solutionButton.addActionListener { showSolution() }
        
        val closeButton = ModernButton("닫기", ModernButton.ButtonStyle.PRIMARY)
        closeButton.addActionListener { 
            timerThread?.interrupt()
            dispose() 
        }
        
        panel.add(hintButton)
        panel.add(resetButton)
        panel.add(solutionButton)
        panel.add(closeButton)
        
        return panel
    }
    
    private fun startPuzzle() {
        val progress = solvePuzzleUseCase.startPuzzle(puzzle.id)
        if (progress != null) {
            updateBoard(progress)
            startTimer()
        }
    }
    
    private fun handleBoardClick(position: PositionDto) {
        val domainPosition = Position(position.row, position.col)
        
        when (val result = solvePuzzleUseCase.makeMove(domainPosition)) {
            is MakeMoveResult.Success -> {
                updateBoard(result.progress)
                handleMoveResult(result)
                
                // 사운드 효과
                when (result.evaluation) {
                    MoveEvaluation.CORRECT -> SoundEffects.playCorrect()
                    MoveEvaluation.GOOD -> SoundEffects.playGood()
                    MoveEvaluation.INCORRECT -> SoundEffects.playError()
                    MoveEvaluation.BLUNDER -> SoundEffects.playError()
                }
            }
            is MakeMoveResult.InvalidMove -> {
                statusLabel.text = result.reason
                statusLabel.foreground = UITheme.Colors.ERROR
                SoundEffects.playError()
            }
            MakeMoveResult.NoPuzzleActive -> {
                statusLabel.text = "퍼즐이 활성화되지 않았습니다."
            }
        }
    }
    
    private fun handleMoveResult(result: MakeMoveResult.Success) {
        // 평가 메시지 표시
        statusLabel.text = result.evaluation.message
        statusLabel.foreground = when (result.evaluation) {
            MoveEvaluation.CORRECT -> UITheme.Colors.SUCCESS
            MoveEvaluation.GOOD -> UITheme.Colors.WARNING
            MoveEvaluation.INCORRECT -> UITheme.Colors.ERROR
            MoveEvaluation.BLUNDER -> Color(220, 38, 38)
        }
        
        // 완료 처리
        if (result.completed) {
            timerThread?.interrupt()
            val time = result.progress.getSolvedTime()
            val timeStr = formatTime(time)
            
            JOptionPane.showMessageDialog(
                this,
                """
                축하합니다! 퍼즐을 해결했습니다.
                
                시도 횟수: ${result.progress.attempts}회
                힌트 사용: ${result.progress.hintsUsed}개
                소요 시간: $timeStr
                """.trimIndent(),
                "퍼즐 완료",
                JOptionPane.INFORMATION_MESSAGE
            )
            
            SoundEffects.playWin()
            dispose()
        }
    }
    
    private fun updateBoard(progress: PuzzleProgress) {
        // Board를 BoardDto로 변환
        val boardArray = Array(15) { row ->
            Array<PlayerDto?>(15) { col ->
                progress.currentBoard.getStone(Position(row, col))?.let {
                    GameMapper.toDto(it)
                }
            }
        }
        val boardDto = BoardDto(boardArray)
        
        boardPanel.updateBoard(boardDto)
        
        // 마지막 수 표시
        if (progress.moveHistory.isNotEmpty()) {
            val lastMove = progress.moveHistory.last()
            boardPanel.setLastMove(GameMapper.toDto(lastMove.position))
        }
        
        // 상태 업데이트
        attemptsLabel.text = "시도: ${progress.attempts}회"
        hintsUsedLabel.text = "힌트 사용: ${progress.hintsUsed}/${puzzle.hints.size}"
        
        // 힌트 버튼 상태
        hintButton.isEnabled = progress.hintsUsed < puzzle.hints.size
        hintButton.text = "힌트 (${puzzle.hints.size - progress.hintsUsed})"
    }
    
    private fun requestHint() {
        when (val result = solvePuzzleUseCase.requestHint()) {
            is HintResult.Success -> {
                val hintArea = findHintTextArea()
                hintArea?.append("${result.hint}\n\n")
                
                solvePuzzleUseCase.getCurrentProgress()?.let { updateBoard(it) }
                
                if (result.hintsRemaining == 0) {
                    hintButton.isEnabled = false
                    hintButton.text = "힌트 없음"
                }
            }
            HintResult.NoMoreHints -> {
                statusLabel.text = "더 이상 힌트가 없습니다."
            }
            HintResult.NoPuzzleActive -> {}
        }
    }
    
    private fun resetPuzzle() {
        val result = JOptionPane.showConfirmDialog(
            this,
            "퍼즐을 다시 시작하시겠습니까?",
            "다시 시작",
            JOptionPane.YES_NO_OPTION
        )
        
        if (result == JOptionPane.YES_OPTION) {
            solvePuzzleUseCase.resetPuzzle()?.let { progress ->
                updateBoard(progress)
                statusLabel.text = "퍼즐을 다시 시작했습니다."
                statusLabel.foreground = UITheme.Colors.TEXT_PRIMARY
                
                // 힌트 영역 초기화
                findHintTextArea()?.text = ""
            }
        }
    }
    
    private fun showSolution() {
        val result = JOptionPane.showConfirmDialog(
            this,
            "해답을 보시겠습니까?\n해답을 보면 퍼즐이 종료됩니다.",
            "해답 보기",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        )
        
        if (result == JOptionPane.YES_OPTION) {
            when (val solutionResult = solvePuzzleUseCase.showSolution()) {
                is SolutionResult.Success -> {
                    // 해답 표시
                    val solutionText = buildString {
                        appendLine("=== 해답 ===")
                        appendLine()
                        appendLine("정답 수순:")
                        solutionResult.mainLine.forEachIndexed { index, pos ->
                            val coord = "${'A' + pos.col}${15 - pos.row}"
                            appendLine("${index + 1}. $coord")
                        }
                        appendLine()
                        appendLine("설명: ${solutionResult.explanation}")
                    }
                    
                    JOptionPane.showMessageDialog(
                        this,
                        solutionText,
                        "퍼즐 해답",
                        JOptionPane.INFORMATION_MESSAGE
                    )
                    
                    timerThread?.interrupt()
                    dispose()
                }
                SolutionResult.NoSolution -> {
                    statusLabel.text = "해답이 없습니다."
                }
                SolutionResult.NoPuzzleActive -> {}
            }
        }
    }
    
    private fun findHintTextArea(): JTextArea? {
        // 힌트 영역의 JTextArea 찾기
        for (component in infoPanel.components) {
            if (component is JPanel) {
                val scrollPane = component.components.firstOrNull { it is JScrollPane } as? JScrollPane
                return scrollPane?.viewport?.view as? JTextArea
            }
        }
        return null
    }
    
    private fun startTimer() {
        timerThread = Thread {
            val startTime = System.currentTimeMillis()
            while (!Thread.currentThread().isInterrupted) {
                try {
                    val elapsed = System.currentTimeMillis() - startTime
                    SwingUtilities.invokeLater {
                        timerLabel.text = "시간: ${formatTime(elapsed)}"
                    }
                    Thread.sleep(1000)
                } catch (e: InterruptedException) {
                    break
                }
            }
        }
        timerThread?.start()
    }
    
    private fun formatTime(millis: Long): String {
        val seconds = millis / 1000
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return "%02d:%02d".format(minutes, remainingSeconds)
    }
    
    override fun dispose() {
        timerThread?.interrupt()
        solvePuzzleUseCase.endPuzzle()
        super.dispose()
    }
}