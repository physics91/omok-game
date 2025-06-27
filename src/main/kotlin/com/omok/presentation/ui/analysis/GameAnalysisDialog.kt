package com.omok.presentation.ui.analysis

import com.omok.domain.analysis.GameAnalyzer
import com.omok.domain.analysis.GameAnalyzer.*
import com.omok.domain.model.Game
import com.omok.domain.model.Player
import com.omok.presentation.ui.components.ModernButton
import com.omok.presentation.ui.theme.UITheme
import java.awt.*
import javax.swing.*
import javax.swing.border.EmptyBorder
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * 게임 분석 다이얼로그
 */
class GameAnalysisDialog(
    parent: Frame,
    private val game: Game
) : JDialog(parent, "게임 분석", true) {
    
    private val analyzer = GameAnalyzer()
    private lateinit var analysisResult: GameAnalysisResult
    private var currentMoveIndex = 0
    
    // UI 컴포넌트
    private lateinit var evaluationGraph: EvaluationGraphPanel
    private lateinit var moveTable: JTable
    private lateinit var moveDetailPanel: MoveDetailPanel
    private lateinit var summaryPanel: JPanel
    
    init {
        initializeUI()
        analyzeGame()
    }
    
    private fun initializeUI() {
        layout = BorderLayout()
        preferredSize = Dimension(900, 700)
        
        val contentPanel = JPanel(BorderLayout())
        contentPanel.background = UITheme.Colors.BACKGROUND
        contentPanel.border = EmptyBorder(UITheme.Spacing.MD, UITheme.Spacing.MD, UITheme.Spacing.MD, UITheme.Spacing.MD)
        
        // 상단: 평가 그래프
        evaluationGraph = EvaluationGraphPanel()
        evaluationGraph.preferredSize = Dimension(0, 200)
        evaluationGraph.border = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UITheme.Colors.GRAY_300),
            EmptyBorder(UITheme.Spacing.SM, UITheme.Spacing.SM, UITheme.Spacing.SM, UITheme.Spacing.SM)
        )
        contentPanel.add(evaluationGraph, BorderLayout.NORTH)
        
        // 중앙: 수 목록과 상세 정보
        val centerPanel = JPanel(BorderLayout())
        centerPanel.background = UITheme.Colors.BACKGROUND
        
        // 좌측: 수 목록
        val moveListPanel = createMoveListPanel()
        moveListPanel.preferredSize = Dimension(300, 0)
        centerPanel.add(moveListPanel, BorderLayout.WEST)
        
        // 우측: 수 상세 정보
        moveDetailPanel = MoveDetailPanel()
        centerPanel.add(moveDetailPanel, BorderLayout.CENTER)
        
        contentPanel.add(centerPanel, BorderLayout.CENTER)
        
        // 하단: 요약 정보
        summaryPanel = createSummaryPanel()
        summaryPanel.preferredSize = Dimension(0, 120)
        contentPanel.add(summaryPanel, BorderLayout.SOUTH)
        
        add(contentPanel, BorderLayout.CENTER)
        
        // 버튼 패널
        val buttonPanel = createButtonPanel()
        add(buttonPanel, BorderLayout.SOUTH)
        
        pack()
        setLocationRelativeTo(parent)
    }
    
    private fun createMoveListPanel(): JPanel {
        val panel = JPanel(BorderLayout())
        panel.background = UITheme.Colors.SURFACE
        panel.border = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UITheme.Colors.GRAY_300),
            EmptyBorder(UITheme.Spacing.SM, UITheme.Spacing.SM, UITheme.Spacing.SM, UITheme.Spacing.SM)
        )
        
        // 테이블 생성
        val columnNames = arrayOf("수", "플레이어", "품질", "평가")
        val tableModel = DefaultTableModel(columnNames, 0)
        
        moveTable = JTable(tableModel)
        moveTable.font = UITheme.Fonts.BODY_SMALL
        moveTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
        moveTable.background = UITheme.Colors.SURFACE
        
        // 품질 컬럼 렌더러
        moveTable.columnModel.getColumn(2).cellRenderer = QualityCellRenderer()
        
        // 선택 이벤트
        moveTable.selectionModel.addListSelectionListener { e ->
            if (!e.valueIsAdjusting) {
                val selectedRow = moveTable.selectedRow
                if (selectedRow >= 0) {
                    currentMoveIndex = selectedRow
                    updateMoveDetail()
                }
            }
        }
        
        val scrollPane = JScrollPane(moveTable)
        scrollPane.background = UITheme.Colors.SURFACE
        panel.add(scrollPane, BorderLayout.CENTER)
        
        return panel
    }
    
    private fun createSummaryPanel(): JPanel {
        val panel = JPanel(GridLayout(2, 3, UITheme.Spacing.MD, UITheme.Spacing.SM))
        panel.background = UITheme.Colors.SURFACE
        panel.border = BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(UITheme.Colors.GRAY_300),
                "게임 요약",
                javax.swing.border.TitledBorder.LEFT,
                javax.swing.border.TitledBorder.TOP,
                UITheme.Fonts.BUTTON,
                UITheme.Colors.GRAY_700
            ),
            EmptyBorder(UITheme.Spacing.SM, UITheme.Spacing.MD, UITheme.Spacing.SM, UITheme.Spacing.MD)
        )
        
        return panel
    }
    
    private fun createButtonPanel(): JPanel {
        val panel = JPanel(FlowLayout(FlowLayout.RIGHT, UITheme.Spacing.SM, 0))
        panel.background = UITheme.Colors.BACKGROUND
        panel.border = EmptyBorder(0, UITheme.Spacing.MD, UITheme.Spacing.SM, UITheme.Spacing.MD)
        
        val exportButton = ModernButton("분석 내보내기", ModernButton.ButtonStyle.SECONDARY)
        exportButton.addActionListener { exportAnalysis() }
        
        val closeButton = ModernButton("닫기", ModernButton.ButtonStyle.PRIMARY)
        closeButton.addActionListener { dispose() }
        
        panel.add(exportButton)
        panel.add(closeButton)
        
        return panel
    }
    
    private fun analyzeGame() {
        // 진행 다이얼로그 표시
        val progressDialog = JDialog(this, "분석 중...", true)
        progressDialog.layout = BorderLayout()
        progressDialog.isUndecorated = true
        
        val progressPanel = JPanel()
        progressPanel.background = UITheme.Colors.SURFACE
        progressPanel.border = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UITheme.Colors.GRAY_300, 1),
            EmptyBorder(UITheme.Spacing.MD, UITheme.Spacing.LG, UITheme.Spacing.MD, UITheme.Spacing.LG)
        )
        
        val progressLabel = JLabel("게임을 분석하고 있습니다...")
        progressLabel.font = UITheme.Fonts.BODY
        progressPanel.add(progressLabel)
        
        progressDialog.add(progressPanel)
        progressDialog.pack()
        progressDialog.setLocationRelativeTo(this)
        
        // 백그라운드에서 분석 실행
        val worker = object : SwingWorker<GameAnalysisResult, Void>() {
            override fun doInBackground(): GameAnalysisResult {
                return analyzer.analyzeGame(game)
            }
            
            override fun done() {
                progressDialog.dispose()
                try {
                    analysisResult = get()
                    displayAnalysisResult()
                } catch (e: Exception) {
                    JOptionPane.showMessageDialog(
                        this@GameAnalysisDialog,
                        "분석 중 오류가 발생했습니다: ${e.message}",
                        "오류",
                        JOptionPane.ERROR_MESSAGE
                    )
                }
            }
        }
        
        worker.execute()
    }
    
    private fun displayAnalysisResult() {
        // 평가 그래프 업데이트
        evaluationGraph.setData(analysisResult.positionEvaluations)
        
        // 수 목록 업데이트
        val tableModel = moveTable.model as DefaultTableModel
        tableModel.rowCount = 0
        
        analysisResult.moveAnalyses.forEach { analysis ->
            val player = if (analysis.move.player == Player.BLACK) "흑" else "백"
            val evaluation = formatEvaluation(analysis.evaluation)
            
            tableModel.addRow(arrayOf(
                analysis.moveNumber,
                player,
                analysis.quality,
                evaluation
            ))
        }
        
        // 요약 정보 업데이트
        updateSummary()
        
        // 첫 번째 수 선택
        if (moveTable.rowCount > 0) {
            moveTable.setRowSelectionInterval(0, 0)
        }
    }
    
    private fun updateMoveDetail() {
        if (currentMoveIndex < analysisResult.moveAnalyses.size) {
            val analysis = analysisResult.moveAnalyses[currentMoveIndex]
            moveDetailPanel.showMoveAnalysis(analysis)
            
            // 그래프에서 현재 수 강조
            evaluationGraph.highlightMove(currentMoveIndex)
        }
    }
    
    private fun updateSummary() {
        summaryPanel.removeAll()
        
        val summary = analysisResult.summary
        
        // 총 수
        addSummaryItem("총 수", "${summary.totalMoves}수")
        
        // 정확도
        val blackAccuracy = summary.accuracy[Player.BLACK] ?: 0.0
        val whiteAccuracy = summary.accuracy[Player.WHITE] ?: 0.0
        addSummaryItem("흑 정확도", "${"%.1f".format(blackAccuracy)}%")
        addSummaryItem("백 정확도", "${"%.1f".format(whiteAccuracy)}%")
        
        // 실수
        addSummaryItem("흑 실수", "${summary.blackMistakes + summary.blackBlunders}개")
        addSummaryItem("백 실수", "${summary.whiteMistakes + summary.whiteBlunders}개")
        
        // 전환점
        addSummaryItem("전환점", "${analysisResult.turningPoints.size}개")
        
        summaryPanel.revalidate()
        summaryPanel.repaint()
    }
    
    private fun addSummaryItem(label: String, value: String) {
        val itemPanel = JPanel(BorderLayout())
        itemPanel.background = UITheme.Colors.SURFACE
        
        val labelComponent = JLabel(label)
        labelComponent.font = UITheme.Fonts.CAPTION
        labelComponent.foreground = UITheme.Colors.GRAY_600
        
        val valueComponent = JLabel(value)
        valueComponent.font = UITheme.Fonts.BUTTON
        valueComponent.foreground = UITheme.Colors.PRIMARY
        
        itemPanel.add(labelComponent, BorderLayout.WEST)
        itemPanel.add(valueComponent, BorderLayout.EAST)
        
        summaryPanel.add(itemPanel)
    }
    
    private fun formatEvaluation(eval: Int): String {
        return when {
            eval > 10000 -> "+${eval / 1000}k"
            eval > 0 -> "+$eval"
            eval < -10000 -> "${eval / 1000}k"
            else -> "$eval"
        }
    }
    
    private fun exportAnalysis() {
        val fileChooser = JFileChooser()
        fileChooser.dialogTitle = "분석 결과 내보내기"
        fileChooser.selectedFile = java.io.File("game_analysis.txt")
        
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                val content = buildAnalysisReport()
                fileChooser.selectedFile.writeText(content)
                JOptionPane.showMessageDialog(
                    this,
                    "분석 결과가 저장되었습니다.",
                    "저장 완료",
                    JOptionPane.INFORMATION_MESSAGE
                )
            } catch (e: Exception) {
                JOptionPane.showMessageDialog(
                    this,
                    "저장 중 오류가 발생했습니다: ${e.message}",
                    "오류",
                    JOptionPane.ERROR_MESSAGE
                )
            }
        }
    }
    
    private fun buildAnalysisReport(): String {
        return buildString {
            appendLine("=== 오목 게임 분석 결과 ===")
            appendLine()
            
            // 요약
            val summary = analysisResult.summary
            appendLine("총 수: ${summary.totalMoves}")
            appendLine("흑 정확도: ${"%.1f".format(summary.accuracy[Player.BLACK])}%")
            appendLine("백 정확도: ${"%.1f".format(summary.accuracy[Player.WHITE])}%")
            appendLine()
            
            // 실수
            appendLine("흑 실수: ${summary.blackMistakes}개, 대실수: ${summary.blackBlunders}개")
            appendLine("백 실수: ${summary.whiteMistakes}개, 대실수: ${summary.whiteBlunders}개")
            appendLine()
            
            // 전환점
            if (analysisResult.turningPoints.isNotEmpty()) {
                appendLine("=== 전환점 ===")
                analysisResult.turningPoints.forEach { tp ->
                    appendLine("${tp.moveNumber}수: ${tp.description}")
                }
                appendLine()
            }
            
            // 각 수 분석
            appendLine("=== 수별 분석 ===")
            analysisResult.moveAnalyses.forEach { analysis ->
                val player = if (analysis.move.player == Player.BLACK) "흑" else "백"
                val coord = "${'A' + analysis.move.position.col}${15 - analysis.move.position.row}"
                
                append("${analysis.moveNumber}. $player $coord ")
                append("[${analysis.quality.displayName}] ")
                
                if (analysis.explanation.isNotEmpty()) {
                    append("- ${analysis.explanation}")
                }
                
                if (analysis.bestMove != null && analysis.bestMove != analysis.move.position) {
                    val bestCoord = "${'A' + analysis.bestMove.col}${15 - analysis.bestMove.row}"
                    append(" (추천: $bestCoord)")
                }
                
                appendLine()
            }
        }
    }
    
    /**
     * 품질 셀 렌더러
     */
    private class QualityCellRenderer : DefaultTableCellRenderer() {
        override fun getTableCellRendererComponent(
            table: JTable?,
            value: Any?,
            isSelected: Boolean,
            hasFocus: Boolean,
            row: Int,
            column: Int
        ): Component {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
            
            if (value is MoveQuality) {
                text = value.displayName
                foreground = value.color
                font = font.deriveFont(Font.BOLD)
            }
            
            return this
        }
    }
}

/**
 * 평가 그래프 패널
 */
class EvaluationGraphPanel : JPanel() {
    private var evaluations: List<GameAnalyzer.PositionEvaluation> = emptyList()
    private var highlightedMove = -1
    
    init {
        background = UITheme.Colors.SURFACE
    }
    
    fun setData(evaluations: List<GameAnalyzer.PositionEvaluation>) {
        this.evaluations = evaluations
        repaint()
    }
    
    fun highlightMove(moveIndex: Int) {
        highlightedMove = moveIndex
        repaint()
    }
    
    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        
        if (evaluations.isEmpty()) return
        
        val g2 = g as Graphics2D
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        
        val padding = 30
        val width = width - 2 * padding
        val height = height - 2 * padding
        
        // 최대/최소값 찾기
        val maxEval = evaluations.maxOf { abs(it.blackAdvantage) }
        val scale = if (maxEval > 0) height / 2.0 / maxEval else 1.0
        
        // 중앙선 그리기
        g2.color = UITheme.Colors.GRAY_400
        g2.drawLine(padding, height / 2 + padding, width + padding, height / 2 + padding)
        
        // 평가선 그리기
        g2.color = UITheme.Colors.PRIMARY
        g2.stroke = BasicStroke(2f)
        
        for (i in 1 until evaluations.size) {
            val x1 = padding + (i - 1) * width / (evaluations.size - 1)
            val x2 = padding + i * width / (evaluations.size - 1)
            
            val y1 = height / 2 + padding - (evaluations[i - 1].blackAdvantage * scale).toInt()
            val y2 = height / 2 + padding - (evaluations[i].blackAdvantage * scale).toInt()
            
            g2.drawLine(x1, y1, x2, y2)
        }
        
        // 점 그리기
        for (i in evaluations.indices) {
            val x = padding + i * width / max(1, evaluations.size - 1)
            val y = height / 2 + padding - (evaluations[i].blackAdvantage * scale).toInt()
            
            g2.color = if (i == highlightedMove) UITheme.Colors.WARNING else UITheme.Colors.PRIMARY
            g2.fillOval(x - 3, y - 3, 6, 6)
        }
        
        // 라벨
        g2.color = UITheme.Colors.GRAY_700
        g2.font = UITheme.Fonts.CAPTION
        g2.drawString("흑 유리", 5, 20)
        g2.drawString("백 유리", 5, height - 5)
    }
}

/**
 * 수 상세 정보 패널
 */
class MoveDetailPanel : JPanel() {
    private val detailArea = JTextArea()
    
    init {
        layout = BorderLayout()
        background = UITheme.Colors.SURFACE
        border = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UITheme.Colors.GRAY_300),
            EmptyBorder(UITheme.Spacing.MD, UITheme.Spacing.MD, UITheme.Spacing.MD, UITheme.Spacing.MD)
        )
        
        detailArea.isEditable = false
        detailArea.font = UITheme.Fonts.BODY
        detailArea.background = UITheme.Colors.SURFACE
        detailArea.lineWrap = true
        detailArea.wrapStyleWord = true
        
        val scrollPane = JScrollPane(detailArea)
        scrollPane.border = null
        add(scrollPane, BorderLayout.CENTER)
    }
    
    fun showMoveAnalysis(analysis: GameAnalyzer.MoveAnalysis) {
        val text = buildString {
            val player = if (analysis.move.player == Player.BLACK) "흑" else "백"
            val coord = "${'A' + analysis.move.position.col}${15 - analysis.move.position.row}"
            
            appendLine("${analysis.moveNumber}수: $player $coord")
            appendLine()
            appendLine("품질: ${analysis.quality.displayName}")
            appendLine("평가: ${analysis.evaluation}")
            
            if (analysis.bestMove != null && analysis.bestMove != analysis.move.position) {
                val bestCoord = "${'A' + analysis.bestMove.col}${15 - analysis.bestMove.row}"
                appendLine("추천수: $bestCoord")
            }
            
            if (analysis.explanation.isNotEmpty()) {
                appendLine()
                appendLine("설명: ${analysis.explanation}")
            }
            
            if (analysis.threats.isNotEmpty()) {
                appendLine()
                appendLine("위협 요소:")
                analysis.threats.forEach { threat ->
                    appendLine("- ${threat.type}: 심각도 ${threat.severity}")
                }
            }
            
            if (analysis.opportunities.isNotEmpty()) {
                appendLine()
                appendLine("기회: ${analysis.opportunities.size}개의 좋은 후속수")
            }
        }
        
        detailArea.text = text
        detailArea.caretPosition = 0
    }
}