package com.omok.presentation.ui.tournament

import com.omok.domain.tournament.*
import com.omok.domain.model.GameMode
import com.omok.infrastructure.tournament.TournamentManager
import com.omok.presentation.ui.components.ModernButton
import com.omok.presentation.ui.theme.UITheme
import com.omok.presentation.ui.GameWindow
import com.omok.application.dto.*
import java.awt.*
import javax.swing.*
import javax.swing.border.EmptyBorder
import javax.swing.border.TitledBorder
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeCellRenderer

/**
 * 토너먼트 진행 제어 다이얼로그
 */
class TournamentControlDialog(
    parent: Frame,
    private val tournament: Tournament
) : JDialog(parent, "${tournament.name} - 토너먼트 진행", false) { // Modal false로 변경
    
    private val tournamentManager = TournamentManager.getInstance()
    private lateinit var bracketsTree: JTree
    private lateinit var matchTable: JTable
    private lateinit var standingsTable: JTable
    private lateinit var statusLabel: JLabel
    private lateinit var startMatchButton: ModernButton
    private lateinit var nextRoundButton: ModernButton
    
    private var refreshTimer: Timer? = null
    
    init {
        initializeUI()
        updateDisplay()
        startAutoRefresh()
    }
    
    private fun initializeUI() {
        layout = BorderLayout()
        preferredSize = Dimension(1000, 700)
        
        val contentPanel = JPanel(BorderLayout())
        contentPanel.background = UITheme.Colors.BACKGROUND
        contentPanel.border = EmptyBorder(UITheme.Spacing.MD, UITheme.Spacing.MD, UITheme.Spacing.MD, UITheme.Spacing.MD)
        
        // 상단: 토너먼트 정보 및 상태
        contentPanel.add(createStatusPanel(), BorderLayout.NORTH)
        
        // 중앙: 탭 패널
        contentPanel.add(createTabPanel(), BorderLayout.CENTER)
        
        // 하단: 제어 버튼
        contentPanel.add(createControlPanel(), BorderLayout.SOUTH)
        
        add(contentPanel)
        
        pack()
        setLocationRelativeTo(parent)
    }
    
    private fun createStatusPanel(): JPanel {
        val panel = JPanel(BorderLayout())
        panel.background = UITheme.Colors.SURFACE
        panel.border = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UITheme.Colors.GRAY_300),
            EmptyBorder(UITheme.Spacing.MD, UITheme.Spacing.MD, UITheme.Spacing.MD, UITheme.Spacing.MD)
        )
        
        // 토너먼트 정보
        val infoPanel = JPanel(GridLayout(2, 3, UITheme.Spacing.MD, UITheme.Spacing.SM))
        infoPanel.background = UITheme.Colors.SURFACE
        
        infoPanel.add(createInfoLabel("토너먼트:", tournament.name))
        infoPanel.add(createInfoLabel("형식:", tournament.format.displayName))
        infoPanel.add(createInfoLabel("참가자:", "${tournament.participants.size}명"))
        
        statusLabel = JLabel("상태: ${getStatusText(tournament.status)}")
        statusLabel.font = UITheme.Fonts.BUTTON
        statusLabel.foreground = getStatusColor(tournament.status)
        infoPanel.add(statusLabel)
        
        infoPanel.add(createInfoLabel("현재 라운드:", getCurrentRoundName()))
        infoPanel.add(createInfoLabel("진행률:", getProgressText()))
        
        panel.add(infoPanel, BorderLayout.CENTER)
        
        return panel
    }
    
    private fun createTabPanel(): JTabbedPane {
        val tabbedPane = JTabbedPane()
        tabbedPane.background = UITheme.Colors.BACKGROUND
        tabbedPane.font = UITheme.Fonts.BODY
        
        // 대진표 탭
        tabbedPane.addTab("대진표", createBracketsPanel())
        
        // 매치 목록 탭
        tabbedPane.addTab("매치 목록", createMatchListPanel())
        
        // 순위표 탭
        tabbedPane.addTab("순위표", createStandingsPanel())
        
        return tabbedPane
    }
    
    private fun createBracketsPanel(): JPanel {
        val panel = JPanel(BorderLayout())
        panel.background = UITheme.Colors.BACKGROUND
        
        // 트리 구조로 대진표 표시
        val rootNode = DefaultMutableTreeNode(tournament.name)
        buildBracketTree(rootNode)
        
        bracketsTree = JTree(rootNode)
        bracketsTree.font = UITheme.Fonts.BODY
        bracketsTree.cellRenderer = BracketTreeCellRenderer()
        
        // 모든 노드 확장
        for (i in 0 until bracketsTree.rowCount) {
            bracketsTree.expandRow(i)
        }
        
        val scrollPane = JScrollPane(bracketsTree)
        scrollPane.background = UITheme.Colors.BACKGROUND
        panel.add(scrollPane, BorderLayout.CENTER)
        
        return panel
    }
    
    private fun createMatchListPanel(): JPanel {
        val panel = JPanel(BorderLayout())
        panel.background = UITheme.Colors.BACKGROUND
        
        // 매치 테이블
        val columnNames = arrayOf("라운드", "매치", "Player 1", "vs", "Player 2", "상태", "결과")
        val tableModel = object : DefaultTableModel(columnNames, 0) {
            override fun isCellEditable(row: Int, column: Int) = false
        }
        
        matchTable = JTable(tableModel)
        matchTable.font = UITheme.Fonts.BODY
        matchTable.rowHeight = 35
        matchTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
        
        // 상태 컬럼 렌더러
        matchTable.columnModel.getColumn(5).cellRenderer = MatchStatusCellRenderer()
        
        // 컬럼 너비 조정
        matchTable.columnModel.getColumn(0).preferredWidth = 60
        matchTable.columnModel.getColumn(1).preferredWidth = 50
        matchTable.columnModel.getColumn(2).preferredWidth = 150
        matchTable.columnModel.getColumn(3).preferredWidth = 30
        matchTable.columnModel.getColumn(4).preferredWidth = 150
        matchTable.columnModel.getColumn(5).preferredWidth = 80
        matchTable.columnModel.getColumn(6).preferredWidth = 100
        
        val scrollPane = JScrollPane(matchTable)
        scrollPane.background = UITheme.Colors.BACKGROUND
        panel.add(scrollPane, BorderLayout.CENTER)
        
        return panel
    }
    
    private fun createStandingsPanel(): JPanel {
        val panel = JPanel(BorderLayout())
        panel.background = UITheme.Colors.BACKGROUND
        
        // 순위표
        val columnNames = arrayOf("순위", "참가자", "승", "패", "무", "포인트")
        val tableModel = object : DefaultTableModel(columnNames, 0) {
            override fun isCellEditable(row: Int, column: Int) = false
        }
        
        standingsTable = JTable(tableModel)
        standingsTable.font = UITheme.Fonts.BODY
        standingsTable.rowHeight = 35
        
        // 순위 컬럼 렌더러
        standingsTable.columnModel.getColumn(0).cellRenderer = RankCellRenderer()
        
        val scrollPane = JScrollPane(standingsTable)
        scrollPane.background = UITheme.Colors.BACKGROUND
        panel.add(scrollPane, BorderLayout.CENTER)
        
        return panel
    }
    
    private fun createControlPanel(): JPanel {
        val panel = JPanel(FlowLayout(FlowLayout.CENTER, UITheme.Spacing.MD, 0))
        panel.background = UITheme.Colors.BACKGROUND
        panel.border = EmptyBorder(UITheme.Spacing.SM, 0, 0, 0)
        
        startMatchButton = ModernButton("매치 시작", ModernButton.ButtonStyle.PRIMARY)
        startMatchButton.addActionListener { startSelectedMatch() }
        
        nextRoundButton = ModernButton("다음 라운드", ModernButton.ButtonStyle.PRIMARY)
        nextRoundButton.addActionListener { proceedToNextRound() }
        nextRoundButton.isEnabled = false
        
        val pauseButton = ModernButton("일시정지", ModernButton.ButtonStyle.SECONDARY)
        pauseButton.addActionListener { pauseTournament() }
        
        val cancelButton = ModernButton("토너먼트 취소", ModernButton.ButtonStyle.SECONDARY)
        cancelButton.addActionListener { cancelTournament() }
        
        val closeButton = ModernButton("닫기", ModernButton.ButtonStyle.GHOST)
        closeButton.addActionListener { dispose() }
        
        panel.add(startMatchButton)
        panel.add(nextRoundButton)
        panel.add(pauseButton)
        panel.add(cancelButton)
        panel.add(closeButton)
        
        return panel
    }
    
    private fun createInfoLabel(label: String, value: String): JPanel {
        val panel = JPanel(FlowLayout(FlowLayout.LEFT, UITheme.Spacing.XS, 0))
        panel.background = UITheme.Colors.SURFACE
        
        val labelComponent = JLabel(label)
        labelComponent.font = UITheme.Fonts.CAPTION
        labelComponent.foreground = UITheme.Colors.GRAY_600
        
        val valueComponent = JLabel(value)
        valueComponent.font = UITheme.Fonts.BUTTON
        valueComponent.foreground = UITheme.Colors.TEXT_PRIMARY
        
        panel.add(labelComponent)
        panel.add(valueComponent)
        
        return panel
    }
    
    private fun buildBracketTree(parentNode: DefaultMutableTreeNode) {
        val currentTournament = tournamentManager.getTournament(tournament.id) ?: return
        
        currentTournament.rounds.forEach { round ->
            val roundNode = DefaultMutableTreeNode("${round.name}")
            
            round.matches.forEach { match ->
                val matchText = buildString {
                    append("${match.player1.name} vs ${match.player2.name}")
                    if (match.isCompleted) {
                        match.result?.let { result ->
                            val winner = if (result.winnerId == match.player1.id) match.player1 else match.player2
                            append(" → 승자: ${winner.name}")
                        }
                    }
                }
                roundNode.add(DefaultMutableTreeNode(matchText))
            }
            
            parentNode.add(roundNode)
        }
    }
    
    private fun updateDisplay() {
        val currentTournament = tournamentManager.getTournament(tournament.id) ?: return
        
        // 상태 업데이트
        statusLabel.text = "상태: ${getStatusText(currentTournament.status)}"
        statusLabel.foreground = getStatusColor(currentTournament.status)
        
        // 대진표 업데이트
        val root = bracketsTree.model.root as DefaultMutableTreeNode
        root.removeAllChildren()
        buildBracketTree(root)
        (bracketsTree.model as javax.swing.tree.DefaultTreeModel).reload()
        
        // 매치 목록 업데이트
        updateMatchList(currentTournament)
        
        // 순위표 업데이트
        updateStandings(currentTournament)
        
        // 버튼 상태 업데이트
        updateButtonStates(currentTournament)
    }
    
    private fun updateMatchList(tournament: Tournament) {
        val tableModel = matchTable.model as DefaultTableModel
        tableModel.rowCount = 0
        
        tournament.rounds.forEach { round ->
            round.matches.forEach { match ->
                val resultText = when {
                    match.result?.isBye == true -> "부전승"
                    match.result != null -> {
                        val winner = if (match.result.winnerId == match.player1.id) match.player1 else match.player2
                        "${winner.name} 승"
                    }
                    else -> "-"
                }
                
                tableModel.addRow(arrayOf(
                    round.name,
                    match.matchNumber,
                    match.player1.name,
                    "vs",
                    match.player2.name,
                    match.status,
                    resultText
                ))
            }
        }
    }
    
    private fun updateStandings(tournament: Tournament) {
        val tableModel = standingsTable.model as DefaultTableModel
        tableModel.rowCount = 0
        
        val standings = tournament.getStandings()
        standings.forEach { standing ->
            tableModel.addRow(arrayOf(
                standing.rank,
                standing.participant.name,
                standing.wins,
                standing.losses,
                standing.draws,
                standing.points
            ))
        }
    }
    
    private fun updateButtonStates(tournament: Tournament) {
        val hasNextMatch = tournamentManager.getNextMatch(tournament.id) != null
        val currentRoundCompleted = tournament.currentRound?.isCompleted ?: false
        
        startMatchButton.isEnabled = hasNextMatch && tournament.status == TournamentStatus.IN_PROGRESS
        nextRoundButton.isEnabled = currentRoundCompleted && !tournament.isCompleted
    }
    
    private fun startSelectedMatch() {
        val currentTournament = tournamentManager.getTournament(tournament.id) ?: return
        val nextMatch = tournamentManager.getNextMatch(tournament.id) ?: return
        
        // 게임 시작을 위한 설정
        val gameWindow = parent as? GameWindow ?: return
        
        // 매치 시작
        tournamentManager.startMatch(tournament.id, nextMatch.id)
        
        // 플레이어 이름 표시를 위한 특별 처리
        JOptionPane.showMessageDialog(
            this,
            """
            매치 시작!
            
            ${nextMatch.player1.name} (흑) vs ${nextMatch.player2.name} (백)
            
            게임이 끝나면 결과를 기록해주세요.
            """.trimIndent(),
            "매치 시작",
            JOptionPane.INFORMATION_MESSAGE
        )
        
        // 게임 시작
        val gameSettings = nextMatch.gameSettings
        gameWindow.startNewGame(
            GameModeDto.PLAYER_VS_PLAYER,
            null,
            com.omok.application.mapper.GameMapper.toDto(gameSettings.gameRule),
            com.omok.application.mapper.GameMapper.toDto(gameSettings.timeLimit)
        )
        
        // 매치 결과 기록을 위한 리스너 추가
        showMatchResultDialog(nextMatch)
    }
    
    private fun showMatchResultDialog(match: TournamentMatch) {
        // 매치 결과 입력 다이얼로그
        val dialog = JDialog(this, "매치 결과 입력", true)
        dialog.layout = BorderLayout()
        dialog.preferredSize = Dimension(400, 200)
        
        val panel = JPanel(GridBagLayout())
        panel.background = UITheme.Colors.BACKGROUND
        panel.border = EmptyBorder(UITheme.Spacing.MD, UITheme.Spacing.MD, UITheme.Spacing.MD, UITheme.Spacing.MD)
        
        val gbc = GridBagConstraints()
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.insets = Insets(UITheme.Spacing.SM, UITheme.Spacing.SM, UITheme.Spacing.SM, UITheme.Spacing.SM)
        
        // 매치 정보
        gbc.gridx = 0
        gbc.gridy = 0
        gbc.gridwidth = 2
        val infoLabel = JLabel("${match.player1.name} vs ${match.player2.name}")
        infoLabel.font = UITheme.Fonts.HEADING
        infoLabel.horizontalAlignment = SwingConstants.CENTER
        panel.add(infoLabel, gbc)
        
        // 결과 선택
        gbc.gridy = 1
        gbc.gridwidth = 1
        panel.add(JLabel("승자:"), gbc)
        
        val winnerCombo = JComboBox(arrayOf(match.player1.name, match.player2.name, "무승부"))
        winnerCombo.font = UITheme.Fonts.BODY
        gbc.gridx = 1
        panel.add(winnerCombo, gbc)
        
        dialog.add(panel, BorderLayout.CENTER)
        
        // 버튼
        val buttonPanel = JPanel(FlowLayout(FlowLayout.RIGHT))
        buttonPanel.background = UITheme.Colors.BACKGROUND
        
        val saveButton = ModernButton("저장", ModernButton.ButtonStyle.PRIMARY)
        saveButton.addActionListener {
            val selectedIndex = winnerCombo.selectedIndex
            val winnerId = when (selectedIndex) {
                0 -> match.player1.id
                1 -> match.player2.id
                else -> match.player1.id // 무승부 처리
            }
            
            tournamentManager.recordMatchResult(
                tournamentId = tournament.id,
                matchId = match.id,
                winnerId = winnerId,
                game = null // 실제 게임 기록은 나중에 연동
            )
            
            updateDisplay()
            dialog.dispose()
        }
        
        val cancelButton = ModernButton("취소", ModernButton.ButtonStyle.SECONDARY)
        cancelButton.addActionListener { dialog.dispose() }
        
        buttonPanel.add(cancelButton)
        buttonPanel.add(saveButton)
        
        dialog.add(buttonPanel, BorderLayout.SOUTH)
        
        dialog.pack()
        dialog.setLocationRelativeTo(this)
        dialog.isVisible = true
    }
    
    private fun proceedToNextRound() {
        // 다음 라운드는 자동으로 진행됨
        updateDisplay()
    }
    
    private fun pauseTournament() {
        tournamentManager.pauseTournament(tournament.id)
        updateDisplay()
    }
    
    private fun cancelTournament() {
        val result = JOptionPane.showConfirmDialog(
            this,
            "토너먼트를 취소하시겠습니까?\n이 작업은 되돌릴 수 없습니다.",
            "토너먼트 취소",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        )
        
        if (result == JOptionPane.YES_OPTION) {
            tournamentManager.cancelTournament(tournament.id)
            dispose()
        }
    }
    
    private fun startAutoRefresh() {
        refreshTimer = Timer(2000) { updateDisplay() }
        refreshTimer?.start()
    }
    
    override fun dispose() {
        refreshTimer?.stop()
        super.dispose()
    }
    
    private fun getStatusText(status: TournamentStatus): String = when (status) {
        TournamentStatus.NOT_STARTED -> "시작 전"
        TournamentStatus.IN_PROGRESS -> "진행 중"
        TournamentStatus.PAUSED -> "일시정지"
        TournamentStatus.COMPLETED -> "완료"
        TournamentStatus.CANCELLED -> "취소됨"
    }
    
    private fun getStatusColor(status: TournamentStatus): Color = when (status) {
        TournamentStatus.NOT_STARTED -> UITheme.Colors.GRAY_600
        TournamentStatus.IN_PROGRESS -> UITheme.Colors.SUCCESS
        TournamentStatus.PAUSED -> UITheme.Colors.WARNING
        TournamentStatus.COMPLETED -> UITheme.Colors.PRIMARY
        TournamentStatus.CANCELLED -> UITheme.Colors.ERROR
    }
    
    private fun getCurrentRoundName(): String {
        val currentTournament = tournamentManager.getTournament(tournament.id) ?: return "-"
        return currentTournament.currentRound?.name ?: "-"
    }
    
    private fun getProgressText(): String {
        val currentTournament = tournamentManager.getTournament(tournament.id) ?: return "0%"
        val totalMatches = currentTournament.rounds.sumOf { it.matches.size }
        val completedMatches = currentTournament.rounds.sumOf { round ->
            round.matches.count { it.isCompleted }
        }
        
        val percentage = if (totalMatches > 0) {
            (completedMatches * 100) / totalMatches
        } else 0
        
        return "$completedMatches/$totalMatches ($percentage%)"
    }
    
    /**
     * 대진표 트리 셀 렌더러
     */
    private class BracketTreeCellRenderer : DefaultTreeCellRenderer() {
        override fun getTreeCellRendererComponent(
            tree: JTree?, value: Any?, sel: Boolean, expanded: Boolean,
            leaf: Boolean, row: Int, hasFocus: Boolean
        ): Component {
            super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus)
            
            font = if (leaf) UITheme.Fonts.BODY else UITheme.Fonts.BUTTON
            
            return this
        }
    }
    
    /**
     * 매치 상태 셀 렌더러
     */
    private class MatchStatusCellRenderer : DefaultTableCellRenderer() {
        override fun getTableCellRendererComponent(
            table: JTable?, value: Any?, isSelected: Boolean,
            hasFocus: Boolean, row: Int, column: Int
        ): Component {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
            
            if (value is MatchStatus) {
                text = when (value) {
                    MatchStatus.NOT_STARTED -> "대기"
                    MatchStatus.IN_PROGRESS -> "진행 중"
                    MatchStatus.COMPLETED -> "완료"
                    MatchStatus.CANCELLED -> "취소"
                }
                
                foreground = when (value) {
                    MatchStatus.NOT_STARTED -> UITheme.Colors.GRAY_600
                    MatchStatus.IN_PROGRESS -> UITheme.Colors.WARNING
                    MatchStatus.COMPLETED -> UITheme.Colors.SUCCESS
                    MatchStatus.CANCELLED -> UITheme.Colors.ERROR
                }
                
                font = font.deriveFont(Font.BOLD)
            }
            
            return this
        }
    }
    
    /**
     * 순위 셀 렌더러
     */
    private class RankCellRenderer : DefaultTableCellRenderer() {
        override fun getTableCellRendererComponent(
            table: JTable?, value: Any?, isSelected: Boolean,
            hasFocus: Boolean, row: Int, column: Int
        ): Component {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
            
            if (value is Int) {
                font = font.deriveFont(Font.BOLD)
                foreground = when (value) {
                    1 -> Color(255, 215, 0)  // Gold
                    2 -> Color(192, 192, 192) // Silver
                    3 -> Color(205, 127, 50)  // Bronze
                    else -> UITheme.Colors.TEXT_PRIMARY
                }
            }
            
            return this
        }
    }
}