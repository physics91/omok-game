package com.omok.presentation.ui.puzzle

import com.omok.domain.puzzle.*
import com.omok.infrastructure.puzzle.InMemoryPuzzleRepository
import com.omok.presentation.ui.components.ModernButton
import com.omok.presentation.ui.theme.UITheme
import java.awt.*
import javax.swing.*
import javax.swing.border.EmptyBorder
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel

/**
 * 퍼즐 목록 다이얼로그
 */
class PuzzleListDialog(
    parent: Frame,
    private val onPuzzleSelected: (OmokPuzzle) -> Unit
) : JDialog(parent, "퍼즐 선택", true) {
    
    private val puzzleRepository = InMemoryPuzzleRepository()
    private lateinit var puzzleTable: JTable
    private var selectedCategory: PuzzleCategory? = null
    private var selectedDifficulty: PuzzleDifficulty? = null
    
    init {
        initializeUI()
        loadPuzzles()
    }
    
    private fun initializeUI() {
        layout = BorderLayout()
        preferredSize = Dimension(800, 600)
        
        val contentPanel = JPanel(BorderLayout())
        contentPanel.background = UITheme.Colors.BACKGROUND
        contentPanel.border = EmptyBorder(UITheme.Spacing.MD, UITheme.Spacing.MD, UITheme.Spacing.MD, UITheme.Spacing.MD)
        
        // 상단: 필터 패널
        contentPanel.add(createFilterPanel(), BorderLayout.NORTH)
        
        // 중앙: 퍼즐 목록
        contentPanel.add(createPuzzleListPanel(), BorderLayout.CENTER)
        
        // 하단: 버튼 패널
        contentPanel.add(createButtonPanel(), BorderLayout.SOUTH)
        
        add(contentPanel)
        
        pack()
        setLocationRelativeTo(parent)
    }
    
    private fun createFilterPanel(): JPanel {
        val panel = JPanel(FlowLayout(FlowLayout.LEFT, UITheme.Spacing.MD, UITheme.Spacing.SM))
        panel.background = UITheme.Colors.SURFACE
        panel.border = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UITheme.Colors.GRAY_300),
            EmptyBorder(UITheme.Spacing.SM, UITheme.Spacing.MD, UITheme.Spacing.SM, UITheme.Spacing.MD)
        )
        
        // 카테고리 필터
        panel.add(JLabel("카테고리:"))
        val categoryCombo = JComboBox<String>()
        categoryCombo.addItem("전체")
        PuzzleCategory.values().forEach { categoryCombo.addItem(it.displayName) }
        categoryCombo.addActionListener {
            val selected = categoryCombo.selectedItem as String
            selectedCategory = if (selected == "전체") null else 
                PuzzleCategory.values().find { it.displayName == selected }
            loadPuzzles()
        }
        panel.add(categoryCombo)
        
        panel.add(Box.createHorizontalStrut(UITheme.Spacing.LG))
        
        // 난이도 필터
        panel.add(JLabel("난이도:"))
        val difficultyCombo = JComboBox<String>()
        difficultyCombo.addItem("전체")
        PuzzleDifficulty.values().forEach { difficultyCombo.addItem(it.displayName) }
        difficultyCombo.addActionListener {
            val selected = difficultyCombo.selectedItem as String
            selectedDifficulty = if (selected == "전체") null else 
                PuzzleDifficulty.values().find { it.displayName == selected }
            loadPuzzles()
        }
        panel.add(difficultyCombo)
        
        return panel
    }
    
    private fun createPuzzleListPanel(): JPanel {
        val panel = JPanel(BorderLayout())
        panel.background = UITheme.Colors.SURFACE
        
        // 테이블 생성
        val columnNames = arrayOf("제목", "카테고리", "난이도", "목표", "완료")
        val tableModel = object : DefaultTableModel(columnNames, 0) {
            override fun isCellEditable(row: Int, column: Int) = false
        }
        
        puzzleTable = JTable(tableModel)
        puzzleTable.font = UITheme.Fonts.BODY
        puzzleTable.rowHeight = 40
        puzzleTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
        puzzleTable.background = UITheme.Colors.SURFACE
        
        // 렌더러 설정
        puzzleTable.setDefaultRenderer(Object::class.java, PuzzleTableCellRenderer())
        
        // 컬럼 너비 설정
        puzzleTable.columnModel.getColumn(0).preferredWidth = 300  // 제목
        puzzleTable.columnModel.getColumn(1).preferredWidth = 100  // 카테고리
        puzzleTable.columnModel.getColumn(2).preferredWidth = 100  // 난이도
        puzzleTable.columnModel.getColumn(3).preferredWidth = 150  // 목표
        puzzleTable.columnModel.getColumn(4).preferredWidth = 80   // 완료
        
        // 더블클릭 이벤트
        puzzleTable.addMouseListener(object : java.awt.event.MouseAdapter() {
            override fun mouseClicked(e: java.awt.event.MouseEvent) {
                if (e.clickCount == 2) {
                    selectPuzzle()
                }
            }
        })
        
        val scrollPane = JScrollPane(puzzleTable)
        scrollPane.background = UITheme.Colors.SURFACE
        panel.add(scrollPane, BorderLayout.CENTER)
        
        // 추천 퍼즐 패널
        val recommendPanel = createRecommendPanel()
        panel.add(recommendPanel, BorderLayout.SOUTH)
        
        return panel
    }
    
    private fun createRecommendPanel(): JPanel {
        val panel = JPanel(BorderLayout())
        panel.background = UITheme.Colors.PRIMARY_LIGHT
        panel.preferredSize = Dimension(0, 60)
        panel.border = BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, UITheme.Colors.GRAY_300),
            EmptyBorder(UITheme.Spacing.SM, UITheme.Spacing.MD, UITheme.Spacing.SM, UITheme.Spacing.MD)
        )
        
        val recommendLabel = JLabel("추천 퍼즐: ")
        recommendLabel.font = UITheme.Fonts.BUTTON
        panel.add(recommendLabel, BorderLayout.WEST)
        
        val recommendedPuzzles = puzzleRepository.getRecommendedPuzzles(3)
        if (recommendedPuzzles.isNotEmpty()) {
            val recommendText = recommendedPuzzles.joinToString(", ") { it.title }
            val puzzlesLabel = JLabel(recommendText)
            puzzlesLabel.font = UITheme.Fonts.BODY
            panel.add(puzzlesLabel, BorderLayout.CENTER)
        }
        
        return panel
    }
    
    private fun createButtonPanel(): JPanel {
        val panel = JPanel(FlowLayout(FlowLayout.RIGHT, UITheme.Spacing.SM, 0))
        panel.background = UITheme.Colors.BACKGROUND
        panel.border = EmptyBorder(UITheme.Spacing.SM, 0, 0, 0)
        
        val selectButton = ModernButton("선택", ModernButton.ButtonStyle.PRIMARY)
        selectButton.addActionListener { selectPuzzle() }
        
        val cancelButton = ModernButton("취소", ModernButton.ButtonStyle.SECONDARY)
        cancelButton.addActionListener { dispose() }
        
        panel.add(cancelButton)
        panel.add(selectButton)
        
        return panel
    }
    
    private fun loadPuzzles() {
        val tableModel = puzzleTable.model as DefaultTableModel
        tableModel.rowCount = 0
        
        // 필터 적용하여 퍼즐 로드
        var puzzles = puzzleRepository.getAllPuzzles()
        
        selectedCategory?.let { category ->
            puzzles = puzzles.filter { it.category == category }
        }
        
        selectedDifficulty?.let { difficulty ->
            puzzles = puzzles.filter { it.difficulty == difficulty }
        }
        
        // 완료 기록 조회
        val completionRecords = puzzleRepository.getCompletionRecords()
        val completedPuzzleIds = completionRecords
            .filter { it.success }
            .map { it.puzzleId }
            .toSet()
        
        // 테이블에 추가
        puzzles.forEach { puzzle ->
            val isCompleted = puzzle.id in completedPuzzleIds
            tableModel.addRow(arrayOf(
                puzzle,  // 전체 퍼즐 객체를 저장
                puzzle.category.displayName,
                puzzle.difficulty.displayName,
                puzzle.objective.displayName,
                if (isCompleted) "✓" else ""
            ))
        }
    }
    
    private fun selectPuzzle() {
        val selectedRow = puzzleTable.selectedRow
        if (selectedRow >= 0) {
            val puzzle = puzzleTable.model.getValueAt(selectedRow, 0) as OmokPuzzle
            onPuzzleSelected(puzzle)
            dispose()
        }
    }
    
    /**
     * 테이블 셀 렌더러
     */
    private class PuzzleTableCellRenderer : DefaultTableCellRenderer() {
        override fun getTableCellRendererComponent(
            table: JTable?,
            value: Any?,
            isSelected: Boolean,
            hasFocus: Boolean,
            row: Int,
            column: Int
        ): Component {
            val component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
            
            when (column) {
                0 -> { // 제목 컬럼
                    if (value is OmokPuzzle) {
                        text = value.title
                        toolTipText = value.description
                    }
                }
                2 -> { // 난이도 컬럼
                    val difficulty = value as? String ?: ""
                    foreground = when (difficulty) {
                        "초급" -> Color(34, 197, 94)
                        "중급" -> Color(59, 130, 246)
                        "상급" -> Color(251, 146, 60)
                        "고수" -> Color(239, 68, 68)
                        "달인" -> Color(139, 92, 246)
                        else -> UITheme.Colors.TEXT_PRIMARY
                    }
                    font = font.deriveFont(Font.BOLD)
                }
                4 -> { // 완료 컬럼
                    if (value == "✓") {
                        foreground = UITheme.Colors.SUCCESS
                        font = font.deriveFont(Font.BOLD)
                    }
                }
            }
            
            return component
        }
    }
}