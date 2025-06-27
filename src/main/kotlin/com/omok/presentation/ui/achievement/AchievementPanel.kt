package com.omok.presentation.ui.achievement

import com.omok.domain.achievement.*
import com.omok.presentation.ui.theme.UITheme
import com.omok.presentation.ui.components.ModernButton
import com.omok.presentation.ui.effects.AdvancedSoundEffects
import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.time.format.DateTimeFormatter
import javax.swing.*
import javax.swing.border.EmptyBorder
import javax.swing.border.LineBorder
import kotlin.math.max
import kotlin.math.min

/**
 * 성취도 패널
 */
class AchievementPanel(
    private val achievementManager: AchievementManager
) : JPanel(), AchievementListener {
    
    private val tabbedPane = JTabbedPane()
    private val overviewPanel = AchievementOverviewPanel()
    private val categoryPanels = mutableMapOf<AchievementCategory, AchievementCategoryPanel>()
    private val statsPanel = PlayerStatsPanel()
    
    private var achievements: List<Achievement> = emptyList()
    private var playerStats: PlayerStats = PlayerStats()
    
    init {
        layout = BorderLayout()
        background = UITheme.Colors.BACKGROUND
        
        achievementManager.addListener(this)
        
        setupUI()
        loadData()
    }
    
    private fun setupUI() {
        // 헤더
        val headerPanel = createHeaderPanel()
        add(headerPanel, BorderLayout.NORTH)
        
        // 탭 패널 설정
        setupTabbedPane()
        add(tabbedPane, BorderLayout.CENTER)
    }
    
    private fun createHeaderPanel(): JPanel {
        val panel = JPanel(BorderLayout())
        panel.background = UITheme.Colors.SURFACE
        panel.border = EmptyBorder(20, 20, 15, 20)
        
        // 제목
        val titleLabel = JLabel("성취도")
        titleLabel.font = UITheme.Fonts.TITLE
        titleLabel.foreground = UITheme.Colors.GRAY_900
        
        // 통계 요약
        val statsLabel = JLabel()
        statsLabel.font = UITheme.Fonts.BODY
        statsLabel.foreground = UITheme.Colors.GRAY_600
        updateStatsLabel(statsLabel)
        
        val textPanel = JPanel()
        textPanel.layout = BoxLayout(textPanel, BoxLayout.Y_AXIS)
        textPanel.isOpaque = false
        textPanel.add(titleLabel)
        textPanel.add(Box.createVerticalStrut(5))
        textPanel.add(statsLabel)
        
        panel.add(textPanel, BorderLayout.WEST)
        
        // 새로고침 버튼
        val refreshButton = ModernButton("새로고침", ModernButton.ButtonStyle.GHOST)
        refreshButton.addActionListener { loadData() }
        panel.add(refreshButton, BorderLayout.EAST)
        
        return panel
    }
    
    private fun updateStatsLabel(label: JLabel) {
        val stats = achievementManager.getPlayerStats()
        label.text = "해제된 성취도: ${stats.achievementsUnlocked}/${stats.totalAchievements} | 총 점수: ${stats.totalPoints}"
    }
    
    private fun setupTabbedPane() {
        // 개요 탭
        tabbedPane.addTab("개요", overviewPanel)
        
        // 카테고리별 탭
        AchievementCategory.values().forEach { category ->
            val panel = AchievementCategoryPanel(category)
            categoryPanels[category] = panel
            tabbedPane.addTab(category.displayName, panel)
        }
        
        // 통계 탭
        tabbedPane.addTab("통계", statsPanel)
    }
    
    private fun loadData() {
        achievements = achievementManager.getAllAchievements()
        playerStats = achievementManager.getPlayerStats()
        
        // 각 패널 업데이트
        overviewPanel.updateData(achievements, achievementManager)
        
        AchievementCategory.values().forEach { category ->
            val categoryAchievements = achievementManager.getAchievementsByCategory(category)
            categoryPanels[category]?.updateData(categoryAchievements, achievementManager)
        }
        
        statsPanel.updateData(playerStats)
        
        // 헤더 통계 업데이트
        val headerPanel = getComponent(0) as JPanel
        val textPanel = headerPanel.getComponent(0) as JPanel
        val statsLabel = textPanel.getComponent(2) as JLabel
        updateStatsLabel(statsLabel)
    }
    
    override fun onAchievementUnlocked(achievement: Achievement, progress: AchievementProgress) {
        // 성취도 해제 알림
        SwingUtilities.invokeLater {
            loadData()
            showAchievementNotification(achievement)
        }
    }
    
    override fun onProgressUpdated(achievement: Achievement, progress: AchievementProgress) {
        // 진행도 업데이트
        SwingUtilities.invokeLater {
            loadData()
        }
    }
    
    private fun showAchievementNotification(achievement: Achievement) {
        val notification = AchievementNotification(achievement)
        notification.show(this)
        
        // 사운드 재생
        try {
            com.omok.presentation.ui.effects.SoundEffects.playWin()
        } catch (e: Exception) {
            // 사운드 재생 실패 시 무시
        }
    }
}

/**
 * 성취도 개요 패널
 */
class AchievementOverviewPanel : JPanel() {
    
    private val recentPanel = JPanel()
    private val progressPanel = JPanel()
    
    init {
        layout = BorderLayout(10, 10)
        background = UITheme.Colors.BACKGROUND
        border = EmptyBorder(15, 15, 15, 15)
        
        setupUI()
    }
    
    private fun setupUI() {
        // 최근 해제된 성취도
        val recentContainer = createSectionPanel("최근 해제된 성취도", recentPanel)
        add(recentContainer, BorderLayout.NORTH)
        
        // 진행 중인 성취도
        val progressContainer = createSectionPanel("진행 중인 성취도", progressPanel)
        add(progressContainer, BorderLayout.CENTER)
    }
    
    private fun createSectionPanel(title: String, contentPanel: JPanel): JPanel {
        val container = JPanel(BorderLayout())
        container.background = UITheme.Colors.SURFACE
        container.border = LineBorder(UITheme.Colors.GRAY_300, 1)
        
        // 제목
        val titleLabel = JLabel(title)
        titleLabel.font = UITheme.Fonts.BUTTON
        titleLabel.foreground = UITheme.Colors.GRAY_900
        titleLabel.border = EmptyBorder(15, 15, 10, 15)
        container.add(titleLabel, BorderLayout.NORTH)
        
        // 내용 패널
        contentPanel.layout = BoxLayout(contentPanel, BoxLayout.Y_AXIS)
        contentPanel.background = UITheme.Colors.SURFACE
        contentPanel.border = EmptyBorder(0, 15, 15, 15)
        
        val scrollPane = JScrollPane(contentPanel)
        scrollPane.border = null
        scrollPane.verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
        scrollPane.horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        container.add(scrollPane, BorderLayout.CENTER)
        
        return container
    }
    
    fun updateData(achievements: List<Achievement>, manager: AchievementManager) {
        // 최근 해제된 성취도
        recentPanel.removeAll()
        val recentUnlocked = manager.getUnlockedAchievements().take(5)
        
        if (recentUnlocked.isEmpty()) {
            val emptyLabel = JLabel("아직 해제된 성취도가 없습니다")
            emptyLabel.foreground = UITheme.Colors.GRAY_500
            emptyLabel.font = UITheme.Fonts.BODY
            recentPanel.add(emptyLabel)
        } else {
            recentUnlocked.forEach { (achievement, progress) ->
                val card = AchievementCard(achievement, progress, true)
                recentPanel.add(card)
                recentPanel.add(Box.createVerticalStrut(10))
            }
        }
        
        // 진행 중인 성취도
        progressPanel.removeAll()
        val inProgress = manager.getInProgressAchievements().take(10)
        
        if (inProgress.isEmpty()) {
            val emptyLabel = JLabel("진행 중인 성취도가 없습니다")
            emptyLabel.foreground = UITheme.Colors.GRAY_500
            emptyLabel.font = UITheme.Fonts.BODY
            progressPanel.add(emptyLabel)
        } else {
            inProgress.forEach { (achievement, progress) ->
                val card = AchievementCard(achievement, progress, false)
                progressPanel.add(card)
                progressPanel.add(Box.createVerticalStrut(10))
            }
        }
        
        revalidate()
        repaint()
    }
}

/**
 * 카테고리별 성취도 패널
 */
class AchievementCategoryPanel(
    private val category: AchievementCategory
) : JPanel() {
    
    private val contentPanel = JPanel()
    
    init {
        layout = BorderLayout()
        background = UITheme.Colors.BACKGROUND
        border = EmptyBorder(15, 15, 15, 15)
        
        setupUI()
    }
    
    private fun setupUI() {
        // 카테고리 설명
        val headerPanel = JPanel(BorderLayout())
        headerPanel.background = UITheme.Colors.SURFACE
        headerPanel.border = EmptyBorder(15, 15, 15, 15)
        
        val titleLabel = JLabel(category.displayName)
        titleLabel.font = UITheme.Fonts.BUTTON
        titleLabel.foreground = category.color
        
        headerPanel.add(titleLabel, BorderLayout.WEST)
        add(headerPanel, BorderLayout.NORTH)
        
        // 성취도 목록
        contentPanel.layout = BoxLayout(contentPanel, BoxLayout.Y_AXIS)
        contentPanel.background = UITheme.Colors.BACKGROUND
        
        val scrollPane = JScrollPane(contentPanel)
        scrollPane.border = null
        scrollPane.verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
        add(scrollPane, BorderLayout.CENTER)
    }
    
    fun updateData(achievements: List<Achievement>, manager: AchievementManager) {
        contentPanel.removeAll()
        
        if (achievements.isEmpty()) {
            val emptyLabel = JLabel("이 카테고리에는 성취도가 없습니다")
            emptyLabel.foreground = UITheme.Colors.GRAY_500
            emptyLabel.font = UITheme.Fonts.BODY
            contentPanel.add(emptyLabel)
        } else {
            achievements.forEach { achievement ->
                val progress = manager.getProgress(achievement.id) ?: 
                    AchievementProgress(achievement.id)
                val card = AchievementCard(achievement, progress, false)
                contentPanel.add(card)
                contentPanel.add(Box.createVerticalStrut(10))
            }
        }
        
        revalidate()
        repaint()
    }
}

/**
 * 플레이어 통계 패널
 */
class PlayerStatsPanel : JPanel() {
    
    private val statsLabels = mutableMapOf<String, JLabel>()
    
    init {
        layout = BorderLayout()
        background = UITheme.Colors.BACKGROUND
        border = EmptyBorder(15, 15, 15, 15)
        
        setupUI()
    }
    
    private fun setupUI() {
        val mainPanel = JPanel(GridBagLayout())
        mainPanel.background = UITheme.Colors.BACKGROUND
        
        val gbc = GridBagConstraints()
        gbc.insets = Insets(5, 5, 5, 5)
        gbc.fill = GridBagConstraints.BOTH
        
        // 게임 통계 섹션
        gbc.gridx = 0
        gbc.gridy = 0
        gbc.weightx = 1.0
        gbc.weighty = 0.5
        mainPanel.add(createGameStatsSection(), gbc)
        
        // 성취도 통계 섹션
        gbc.gridx = 1
        gbc.gridy = 0
        mainPanel.add(createAchievementStatsSection(), gbc)
        
        // 활동 통계 섹션
        gbc.gridx = 0
        gbc.gridy = 1
        gbc.gridwidth = 2
        gbc.weighty = 0.5
        mainPanel.add(createActivityStatsSection(), gbc)
        
        add(mainPanel, BorderLayout.CENTER)
    }
    
    private fun createGameStatsSection(): JPanel {
        val panel = createStatsSection("게임 통계")
        
        addStatRow(panel, "플레이한 게임", "gamesPlayed", "0")
        addStatRow(panel, "승리한 게임", "gamesWon", "0")
        addStatRow(panel, "승률", "winRate", "0%")
        addStatRow(panel, "현재 연승", "currentWinStreak", "0")
        addStatRow(panel, "최고 연승", "bestWinStreak", "0")
        addStatRow(panel, "총 플레이 시간", "totalPlayTime", "0분")
        
        return panel
    }
    
    private fun createAchievementStatsSection(): JPanel {
        val panel = createStatsSection("성취도 통계")
        
        addStatRow(panel, "해제된 성취도", "achievementsUnlocked", "0/0")
        addStatRow(panel, "총 점수", "totalPoints", "0")
        addStatRow(panel, "완료율", "completionRate", "0%")
        
        return panel
    }
    
    private fun createActivityStatsSection(): JPanel {
        val panel = createStatsSection("활동 통계")
        
        addStatRow(panel, "해결한 퍼즐", "puzzlesSolved", "0")
        addStatRow(panel, "토너먼트 우승", "tournamentsWon", "0")
        addStatRow(panel, "분석한 게임", "gamesAnalyzed", "0")
        addStatRow(panel, "사용한 테마", "themesUsed", "0")
        addStatRow(panel, "발견한 명수", "brilliantMoves", "0")
        
        return panel
    }
    
    private fun createStatsSection(title: String): JPanel {
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
        panel.background = UITheme.Colors.SURFACE
        panel.border = LineBorder(UITheme.Colors.GRAY_300, 1)
        
        // 제목
        val titleLabel = JLabel(title)
        titleLabel.font = UITheme.Fonts.BUTTON
        titleLabel.foreground = UITheme.Colors.GRAY_900
        titleLabel.border = EmptyBorder(15, 15, 10, 15)
        titleLabel.alignmentX = Component.LEFT_ALIGNMENT
        panel.add(titleLabel)
        
        return panel
    }
    
    private fun addStatRow(panel: JPanel, label: String, key: String, defaultValue: String) {
        val rowPanel = JPanel(BorderLayout())
        rowPanel.background = UITheme.Colors.SURFACE
        rowPanel.border = EmptyBorder(5, 15, 5, 15)
        rowPanel.maximumSize = Dimension(Int.MAX_VALUE, 30)
        
        val labelComponent = JLabel(label)
        labelComponent.font = UITheme.Fonts.BODY
        labelComponent.foreground = UITheme.Colors.GRAY_700
        
        val valueComponent = JLabel(defaultValue)
        valueComponent.font = UITheme.Fonts.BUTTON
        valueComponent.foreground = UITheme.Colors.GRAY_900
        valueComponent.horizontalAlignment = SwingConstants.RIGHT
        
        rowPanel.add(labelComponent, BorderLayout.WEST)
        rowPanel.add(valueComponent, BorderLayout.EAST)
        
        statsLabels[key] = valueComponent
        panel.add(rowPanel)
    }
    
    fun updateData(stats: PlayerStats) {
        statsLabels["gamesPlayed"]?.text = stats.gamesPlayed.toString()
        statsLabels["gamesWon"]?.text = stats.gamesWon.toString()
        statsLabels["winRate"]?.text = String.format("%.1f%%", stats.winRate)
        statsLabels["currentWinStreak"]?.text = stats.currentWinStreak.toString()
        statsLabels["bestWinStreak"]?.text = stats.bestWinStreak.toString()
        statsLabels["totalPlayTime"]?.text = formatPlayTime(stats.totalPlayTimeMinutes)
        
        statsLabels["achievementsUnlocked"]?.text = "${stats.achievementsUnlocked}/${stats.totalAchievements}"
        statsLabels["totalPoints"]?.text = stats.totalPoints.toString()
        val completionRate = if (stats.totalAchievements > 0) {
            (stats.achievementsUnlocked.toFloat() / stats.totalAchievements * 100)
        } else 0f
        statsLabels["completionRate"]?.text = String.format("%.1f%%", completionRate)
        
        statsLabels["puzzlesSolved"]?.text = stats.puzzlesSolved.toString()
        statsLabels["tournamentsWon"]?.text = stats.tournamentsWon.toString()
        statsLabels["gamesAnalyzed"]?.text = stats.gamesAnalyzed.toString()
        statsLabels["themesUsed"]?.text = stats.themesUsed.toString()
        statsLabels["brilliantMoves"]?.text = stats.brilliantMovesFound.toString()
    }
    
    private fun formatPlayTime(minutes: Long): String {
        return when {
            minutes < 60 -> "${minutes}분"
            minutes < 1440 -> "${minutes / 60}시간 ${minutes % 60}분"
            else -> "${minutes / 1440}일 ${(minutes % 1440) / 60}시간"
        }
    }
}

/**
 * 성취도 카드
 */
class AchievementCard(
    private val achievement: Achievement,
    private val progress: AchievementProgress,
    private val compact: Boolean = false
) : JPanel() {
    
    init {
        layout = BorderLayout()
        background = UITheme.Colors.SURFACE
        border = LineBorder(
            if (progress.isUnlocked) achievement.category.color else UITheme.Colors.GRAY_300,
            if (progress.isUnlocked) 2 else 1
        )
        preferredSize = Dimension(0, if (compact) 60 else 80)
        maximumSize = Dimension(Int.MAX_VALUE, if (compact) 60 else 80)
        
        setupUI()
    }
    
    private fun setupUI() {
        // 아이콘 영역
        val iconPanel = JPanel()
        iconPanel.background = achievement.category.color
        iconPanel.preferredSize = Dimension(if (compact) 50 else 60, 0)
        iconPanel.layout = GridBagLayout()
        
        val iconLabel = JLabel("★")
        iconLabel.font = Font("Arial", Font.BOLD, if (compact) 20 else 24)
        iconLabel.foreground = Color.WHITE
        iconPanel.add(iconLabel)
        
        // 잠긴 성취도 표시
        if (!progress.isUnlocked) {
            iconLabel.text = "🔒"
            iconPanel.background = UITheme.Colors.GRAY_400
        }
        
        add(iconPanel, BorderLayout.WEST)
        
        // 정보 영역
        val infoPanel = JPanel()
        infoPanel.layout = BoxLayout(infoPanel, BoxLayout.Y_AXIS)
        infoPanel.background = UITheme.Colors.SURFACE
        infoPanel.border = EmptyBorder(10, 15, 10, 15)
        
        // 제목과 점수
        val titlePanel = JPanel(BorderLayout())
        titlePanel.isOpaque = false
        
        val nameLabel = JLabel(achievement.name)
        nameLabel.font = if (compact) UITheme.Fonts.BODY else UITheme.Fonts.BUTTON
        nameLabel.foreground = if (progress.isUnlocked) UITheme.Colors.GRAY_900 else UITheme.Colors.GRAY_600
        titlePanel.add(nameLabel, BorderLayout.WEST)
        
        val pointsLabel = JLabel("${achievement.points}점")
        pointsLabel.font = UITheme.Fonts.BODY_SMALL
        pointsLabel.foreground = achievement.category.color
        titlePanel.add(pointsLabel, BorderLayout.EAST)
        
        infoPanel.add(titlePanel)
        
        if (!compact) {
            infoPanel.add(Box.createVerticalStrut(5))
            
            // 설명
            val descLabel = JLabel(achievement.description)
            descLabel.font = UITheme.Fonts.BODY_SMALL
            descLabel.foreground = UITheme.Colors.GRAY_600
            infoPanel.add(descLabel)
        }
        
        // 진행도 표시
        if (!progress.isUnlocked && progress.targetValue > 1) {
            infoPanel.add(Box.createVerticalStrut(5))
            val progressBar = createProgressBar()
            infoPanel.add(progressBar)
        }
        
        // 해제 시간 표시
        if (progress.isUnlocked && progress.unlockedAt != null) {
            infoPanel.add(Box.createVerticalStrut(5))
            val timeLabel = JLabel("해제: ${progress.unlockedAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))}")
            timeLabel.font = UITheme.Fonts.BODY_SMALL
            timeLabel.foreground = UITheme.Colors.GRAY_500
            infoPanel.add(timeLabel)
        }
        
        add(infoPanel, BorderLayout.CENTER)
    }
    
    private fun createProgressBar(): JPanel {
        val progressPanel = JPanel(BorderLayout())
        progressPanel.isOpaque = false
        progressPanel.maximumSize = Dimension(Int.MAX_VALUE, 20)
        
        val progressBar = JProgressBar(0, progress.targetValue)
        progressBar.value = progress.currentValue
        progressBar.isStringPainted = true
        progressBar.string = "${progress.currentValue}/${progress.targetValue}"
        progressBar.foreground = achievement.category.color
        progressBar.font = UITheme.Fonts.BODY_SMALL
        
        progressPanel.add(progressBar, BorderLayout.CENTER)
        
        val percentLabel = JLabel(String.format("%.1f%%", progress.progressPercentage))
        percentLabel.font = UITheme.Fonts.BODY_SMALL
        percentLabel.foreground = UITheme.Colors.GRAY_600
        percentLabel.border = EmptyBorder(0, 10, 0, 0)
        progressPanel.add(percentLabel, BorderLayout.EAST)
        
        return progressPanel
    }
}

/**
 * 성취도 해제 알림
 */
class AchievementNotification(private val achievement: Achievement) {
    
    fun show(parent: Component) {
        val notification = JWindow()
        notification.background = Color(0, 0, 0, 0)
        
        val panel = JPanel(BorderLayout())
        panel.background = achievement.category.color
        panel.border = LineBorder(Color.WHITE, 2)
        panel.preferredSize = Dimension(350, 100)
        
        // 아이콘
        val iconLabel = JLabel("🏆")
        iconLabel.font = Font("Arial", Font.BOLD, 32)
        iconLabel.horizontalAlignment = SwingConstants.CENTER
        iconLabel.preferredSize = Dimension(60, 0)
        iconLabel.foreground = Color.WHITE
        panel.add(iconLabel, BorderLayout.WEST)
        
        // 텍스트
        val textPanel = JPanel()
        textPanel.layout = BoxLayout(textPanel, BoxLayout.Y_AXIS)
        textPanel.background = achievement.category.color
        textPanel.border = EmptyBorder(15, 15, 15, 15)
        
        val titleLabel = JLabel("성취도 해제!")
        titleLabel.font = UITheme.Fonts.BUTTON
        titleLabel.foreground = Color.WHITE
        titleLabel.alignmentX = Component.LEFT_ALIGNMENT
        
        val nameLabel = JLabel(achievement.name)
        nameLabel.font = UITheme.Fonts.BUTTON
        nameLabel.foreground = Color.WHITE
        nameLabel.alignmentX = Component.LEFT_ALIGNMENT
        
        val pointsLabel = JLabel("${achievement.points}점 획득")
        pointsLabel.font = UITheme.Fonts.BODY_SMALL
        pointsLabel.foreground = Color.WHITE
        pointsLabel.alignmentX = Component.LEFT_ALIGNMENT
        
        textPanel.add(titleLabel)
        textPanel.add(Box.createVerticalStrut(5))
        textPanel.add(nameLabel)
        textPanel.add(Box.createVerticalStrut(5))
        textPanel.add(pointsLabel)
        
        panel.add(textPanel, BorderLayout.CENTER)
        
        notification.add(panel)
        notification.pack()
        
        // 위치 설정 (우상단)
        val parentBounds = SwingUtilities.getWindowAncestor(parent)?.bounds 
            ?: Rectangle(0, 0, 800, 600)
        notification.setLocation(
            parentBounds.x + parentBounds.width - notification.width - 20,
            parentBounds.y + 20
        )
        
        notification.isVisible = true
        
        // 3초 후 자동으로 사라짐
        Timer(3000) { 
            notification.dispose() 
        }.apply {
            isRepeats = false
            start()
        }
    }
}