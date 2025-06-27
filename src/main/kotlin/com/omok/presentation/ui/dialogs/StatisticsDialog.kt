package com.omok.presentation.ui.dialogs

import com.omok.infrastructure.statistics.GameStatisticsService
import com.omok.infrastructure.statistics.GameStatistics
import com.omok.infrastructure.statistics.GameRecord
import com.omok.infrastructure.statistics.GameResult
import com.omok.presentation.ui.components.ModernButton
import com.omok.presentation.ui.components.UnifiedDialog
import com.omok.presentation.ui.components.DialogResult
import com.omok.presentation.ui.theme.UITheme
import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.time.format.DateTimeFormatter
import javax.swing.*
import javax.swing.border.EmptyBorder
import javax.swing.border.TitledBorder
import javax.swing.table.DefaultTableModel

/**
 * 게임 통계 다이얼로그
 */
class StatisticsDialog(parent: Frame) : JDialog(parent, "게임 통계", true) {
    
    private val statisticsService = GameStatisticsService.getInstance()
    private val timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    
    init {
        initializeUI()
    }
    
    private fun initializeUI() {
        layout = BorderLayout()
        
        val contentPanel = JPanel(BorderLayout())
        contentPanel.background = UITheme.Colors.BACKGROUND
        contentPanel.border = EmptyBorder(UITheme.Spacing.MD, UITheme.Spacing.MD, UITheme.Spacing.MD, UITheme.Spacing.MD)
        
        // 탭 패널 생성
        val tabbedPane = JTabbedPane()
        tabbedPane.background = UITheme.Colors.SURFACE
        tabbedPane.font = UITheme.Fonts.BODY
        
        // 전체 통계 탭
        tabbedPane.addTab("전체 통계", createOverallStatisticsPanel())
        
        // 최근 통계 탭
        tabbedPane.addTab("최근 10게임", createRecentStatisticsPanel())
        
        // 게임 기록 탭
        tabbedPane.addTab("게임 기록", createGameRecordsPanel())
        
        contentPanel.add(tabbedPane, BorderLayout.CENTER)
        
        // 버튼 패널
        val buttonPanel = createButtonPanel()
        contentPanel.add(buttonPanel, BorderLayout.SOUTH)
        
        add(contentPanel)
        
        setSize(600, 500)
        setLocationRelativeTo(parent)
    }
    
    private fun createOverallStatisticsPanel(): JPanel {
        val panel = JPanel(BorderLayout())
        panel.background = UITheme.Colors.BACKGROUND
        panel.border = EmptyBorder(UITheme.Spacing.MD, UITheme.Spacing.MD, UITheme.Spacing.MD, UITheme.Spacing.MD)
        
        val stats = statisticsService.getStatistics()
        
        if (stats.totalGames == 0) {
            val noDataLabel = JLabel("아직 게임 기록이 없습니다.", SwingConstants.CENTER)
            noDataLabel.font = UITheme.Fonts.HEADING
            noDataLabel.foreground = UITheme.Colors.GRAY_500
            panel.add(noDataLabel, BorderLayout.CENTER)
            return panel
        }
        
        val mainPanel = JPanel()
        mainPanel.layout = BoxLayout(mainPanel, BoxLayout.Y_AXIS)
        mainPanel.background = UITheme.Colors.BACKGROUND
        
        // 기본 통계
        val basicStatsPanel = createStatsSection("기본 통계", listOf(
            "총 게임 수" to "${stats.totalGames}게임",
            "승률" to "${"%.1f".format(stats.winRate)}% (${stats.wins}승)",
            "패율" to "${"%.1f".format(stats.lossRate)}% (${stats.losses}패)",
            "무승부율" to "${"%.1f".format(stats.drawRate)}% (${stats.draws}무)",
            "평균 게임 길이" to "${"%.1f".format(stats.averageGameLength)}수"
        ))
        mainPanel.add(basicStatsPanel)
        
        mainPanel.add(Box.createVerticalStrut(UITheme.Spacing.MD))
        
        // 연승 기록
        val streakStatsPanel = createStatsSection("연승 기록", listOf(
            "현재 연승" to "${stats.currentWinStreak}연승",
            "최고 연승" to "${stats.longestWinStreak}연승",
            "마지막 게임" to (stats.lastPlayed?.format(timeFormatter) ?: "없음")
        ))
        mainPanel.add(streakStatsPanel)
        
        // 모드별/룰별 통계가 있으면 추가
        if (stats.winsByMode.isNotEmpty()) {
            mainPanel.add(Box.createVerticalStrut(UITheme.Spacing.MD))
            val modeStats = stats.winsByMode.map { (mode, wins) ->
                val modeName = when (mode) {
                    com.omok.domain.model.GameMode.PLAYER_VS_PLAYER -> "사람 vs 사람"
                    com.omok.domain.model.GameMode.PLAYER_VS_AI -> "사람 vs AI"
                }
                modeName to "${wins}승"
            }
            val modeStatsPanel = createStatsSection("모드별 승수", modeStats)
            mainPanel.add(modeStatsPanel)
        }
        
        if (stats.winsByRule.isNotEmpty()) {
            mainPanel.add(Box.createVerticalStrut(UITheme.Spacing.MD))
            val ruleStats = stats.winsByRule.map { (rule, wins) ->
                rule.displayName to "${wins}승"
            }
            val ruleStatsPanel = createStatsSection("룰별 승수", ruleStats)
            mainPanel.add(ruleStatsPanel)
        }
        
        val scrollPane = JScrollPane(mainPanel)
        scrollPane.border = null
        scrollPane.verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
        scrollPane.horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        
        panel.add(scrollPane, BorderLayout.CENTER)
        
        return panel
    }
    
    private fun createRecentStatisticsPanel(): JPanel {
        val panel = JPanel(BorderLayout())
        panel.background = UITheme.Colors.BACKGROUND
        panel.border = EmptyBorder(UITheme.Spacing.MD, UITheme.Spacing.MD, UITheme.Spacing.MD, UITheme.Spacing.MD)
        
        val recentStats = statisticsService.getRecentStatistics(10)
        
        if (recentStats.totalGames == 0) {
            val noDataLabel = JLabel("최근 게임 기록이 없습니다.", SwingConstants.CENTER)
            noDataLabel.font = UITheme.Fonts.HEADING
            noDataLabel.foreground = UITheme.Colors.GRAY_500
            panel.add(noDataLabel, BorderLayout.CENTER)
            return panel
        }
        
        val statsPanel = createStatsSection("최근 10게임 통계", listOf(
            "게임 수" to "${recentStats.totalGames}게임",
            "승률" to "${"%.1f".format(recentStats.winRate)}% (${recentStats.wins}승)",
            "패율" to "${"%.1f".format(recentStats.lossRate)}% (${recentStats.losses}패)",
            "무승부율" to "${"%.1f".format(recentStats.drawRate)}% (${recentStats.draws}무)",
            "평균 게임 길이" to "${"%.1f".format(recentStats.averageGameLength)}수"
        ))
        
        panel.add(statsPanel, BorderLayout.NORTH)
        
        return panel
    }
    
    private fun createGameRecordsPanel(): JPanel {
        val panel = JPanel(BorderLayout())
        panel.background = UITheme.Colors.BACKGROUND
        panel.border = EmptyBorder(UITheme.Spacing.MD, UITheme.Spacing.MD, UITheme.Spacing.MD, UITheme.Spacing.MD)
        
        val records = statisticsService.getGameRecords()
        
        if (records.isEmpty()) {
            val noDataLabel = JLabel("게임 기록이 없습니다.", SwingConstants.CENTER)
            noDataLabel.font = UITheme.Fonts.HEADING
            noDataLabel.foreground = UITheme.Colors.GRAY_500
            panel.add(noDataLabel, BorderLayout.CENTER)
            return panel
        }
        
        // 테이블 데이터 준비
        val columnNames = arrayOf("결과", "모드", "룰", "수", "시간", "날짜")
        val data = records.takeLast(50).reversed().map { record ->
            val resultText = when (record.result) {
                GameResult.WIN -> "승리"
                GameResult.LOSS -> "패배"
                GameResult.DRAW -> "무승부"
            }
            
            val modeText = when (record.mode) {
                com.omok.domain.model.GameMode.PLAYER_VS_PLAYER -> "PvP"
                com.omok.domain.model.GameMode.PLAYER_VS_AI -> "PvAI"
            }
            
            val durationText = formatDuration(record.duration)
            
            arrayOf(
                resultText,
                modeText,
                record.rule.displayName,
                "${record.gameLength}수",
                durationText,
                record.timestamp.format(timeFormatter)
            )
        }.toTypedArray()
        
        val tableModel = DefaultTableModel(data, columnNames)
        val table = JTable(tableModel)
        table.font = UITheme.Fonts.BODY_SMALL
        table.background = UITheme.Colors.SURFACE
        table.selectionBackground = UITheme.Colors.PRIMARY_LIGHT
        table.gridColor = UITheme.Colors.GRAY_200
        table.isEnabled = false // 읽기 전용
        
        // 컬럼 너비 조정
        table.columnModel.getColumn(0).preferredWidth = 60  // 결과
        table.columnModel.getColumn(1).preferredWidth = 60  // 모드
        table.columnModel.getColumn(2).preferredWidth = 100 // 룰
        table.columnModel.getColumn(3).preferredWidth = 50  // 수
        table.columnModel.getColumn(4).preferredWidth = 80  // 시간
        table.columnModel.getColumn(5).preferredWidth = 120 // 날짜
        
        val scrollPane = JScrollPane(table)
        scrollPane.background = UITheme.Colors.SURFACE
        
        panel.add(scrollPane, BorderLayout.CENTER)
        
        return panel
    }
    
    private fun createStatsSection(title: String, stats: List<Pair<String, String>>): JPanel {
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
        panel.background = UITheme.Colors.BACKGROUND
        panel.border = BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(UITheme.Colors.GRAY_300),
                title,
                TitledBorder.LEFT,
                TitledBorder.TOP,
                UITheme.Fonts.BUTTON,
                UITheme.Colors.GRAY_700
            ),
            EmptyBorder(UITheme.Spacing.SM, UITheme.Spacing.MD, UITheme.Spacing.SM, UITheme.Spacing.MD)
        )
        
        for ((label, value) in stats) {
            val statPanel = JPanel(BorderLayout())
            statPanel.background = UITheme.Colors.BACKGROUND
            statPanel.maximumSize = Dimension(Integer.MAX_VALUE, 30)
            
            val labelComponent = JLabel(label)
            labelComponent.font = UITheme.Fonts.BODY
            labelComponent.foreground = UITheme.Colors.GRAY_700
            
            val valueComponent = JLabel(value)
            valueComponent.font = UITheme.Fonts.BUTTON
            valueComponent.foreground = UITheme.Colors.PRIMARY
            valueComponent.horizontalAlignment = SwingConstants.RIGHT
            
            statPanel.add(labelComponent, BorderLayout.WEST)
            statPanel.add(valueComponent, BorderLayout.EAST)
            
            panel.add(statPanel)
            panel.add(Box.createVerticalStrut(UITheme.Spacing.XS))
        }
        
        return panel
    }
    
    private fun createButtonPanel(): JPanel {
        val panel = JPanel(FlowLayout(FlowLayout.RIGHT, UITheme.Spacing.SM, 0))
        panel.background = UITheme.Colors.BACKGROUND
        panel.border = EmptyBorder(UITheme.Spacing.SM, 0, 0, 0)
        
        val resetButton = ModernButton("통계 초기화", ModernButton.ButtonStyle.SECONDARY)
        resetButton.addActionListener {
            val result = UnifiedDialog.showConfirm(
                this,
                "통계 초기화",
                "모든 게임 통계를 초기화하시겠습니까?\n이 작업은 되돌릴 수 없습니다.",
                confirmText = "초기화",
                cancelText = "취소"
            )
            
            if (result == DialogResult.CONFIRMED) {
                statisticsService.resetStatistics()
                dispose()
                // 다이얼로그 재생성
                StatisticsDialog(parent as Frame).isVisible = true
            }
        }
        
        val closeButton = ModernButton("닫기", ModernButton.ButtonStyle.PRIMARY)
        closeButton.addActionListener {
            dispose()
        }
        
        panel.add(resetButton)
        panel.add(closeButton)
        
        return panel
    }
    
    private fun formatDuration(durationMs: Long): String {
        val seconds = durationMs / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        
        return when {
            hours > 0 -> "${hours}h ${minutes % 60}m"
            minutes > 0 -> "${minutes}m ${seconds % 60}s"
            else -> "${seconds}s"
        }
    }
}