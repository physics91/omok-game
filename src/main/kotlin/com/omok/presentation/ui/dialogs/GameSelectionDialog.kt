package com.omok.presentation.ui.dialogs

import com.omok.application.dto.AIDifficultyDto
import com.omok.application.dto.GameModeDto
import com.omok.application.dto.PlayerDto
import com.omok.presentation.ui.components.*
import com.omok.presentation.ui.theme.UITheme
import com.omok.presentation.ui.icons.IconLoader
import java.awt.*
import javax.swing.*
import javax.swing.border.EmptyBorder

class GameSelectionDialog(parent: Frame) : JDialog(parent, "새 게임", true) {

    private var selectedMode: GameModeDto? = null
    private var selectedDifficulty: AIDifficultyDto? = null
    private var selectedPlayerColor: PlayerDto? = null
    private var result: Boolean = false

    private val cardLayout = CardLayout()
    private val mainPanel = JPanel(cardLayout)

    companion object {
        private const val MODE_PANEL = "MODE_PANEL"
        private const val COLOR_PANEL = "COLOR_PANEL"
    }

    init {
        setupDialog()
    }

    private fun setupDialog() {
        layout = BorderLayout()
        preferredSize = Dimension(400, 350)

        val headerPanel = createHeaderPanel()
        add(headerPanel, BorderLayout.NORTH)

        mainPanel.add(createModeSelectionPanel(), MODE_PANEL)
        mainPanel.add(createColorSelectionPanel(), COLOR_PANEL)

        add(mainPanel, BorderLayout.CENTER)

        pack()
        setLocationRelativeTo(parent)
    }

    private fun createHeaderPanel(): JPanel { /* ... existing code ... */ }

    private fun createModeSelectionPanel(): JPanel {
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
        panel.background = UITheme.Colors.BACKGROUND
        panel.border = EmptyBorder(20, 20, 20, 20)

        val pvpCard = createGameModeCard("사람 vs 사람", "친구와 함께 즐기는 오목", IconLoader.getIcon(IconLoader.Icon.PLAYER_VS_PLAYER, 24, 24)) {
            selectedMode = GameModeDto.PLAYER_VS_PLAYER
            selectedDifficulty = null
            selectedPlayerColor = PlayerDto.BLACK // Default to black for PvP
            result = true
            dispose()
        }
        panel.add(pvpCard)
        panel.add(Box.createVerticalStrut(15))

        val easyCard = createGameModeCard("사람 vs AI (쉬움)", "초보자를 위한 쉬운 AI", IconLoader.getIcon(IconLoader.Icon.DIFFICULTY_EASY, 24, 24)) {
            selectedMode = GameModeDto.PLAYER_VS_AI
            selectedDifficulty = AIDifficultyDto.EASY
            cardLayout.show(mainPanel, COLOR_PANEL)
        }
        panel.add(easyCard)
        panel.add(Box.createVerticalStrut(10))

        val mediumCard = createGameModeCard("사람 vs AI (보통)", "적당한 실력의 AI와 대결", IconLoader.getIcon(IconLoader.Icon.DIFFICULTY_MEDIUM, 24, 24)) {
            selectedMode = GameModeDto.PLAYER_VS_AI
            selectedDifficulty = AIDifficultyDto.MEDIUM
            cardLayout.show(mainPanel, COLOR_PANEL)
        }
        panel.add(mediumCard)
        panel.add(Box.createVerticalStrut(10))

        val hardCard = createGameModeCard("사람 vs AI (어려움)", "도전적인 고수 AI와 대결", IconLoader.getIcon(IconLoader.Icon.DIFFICULTY_HARD, 24, 24)) {
            selectedMode = GameModeDto.PLAYER_VS_AI
            selectedDifficulty = AIDifficultyDto.HARD
            cardLayout.show(mainPanel, COLOR_PANEL)
        }
        panel.add(hardCard)

        panel.add(Box.createVerticalStrut(20))
        val cancelButton = ModernButton("취소", ModernButton.ButtonStyle.GHOST)
        cancelButton.addActionListener { dispose() }
        cancelButton.alignmentX = Component.CENTER_ALIGNMENT
        panel.add(cancelButton)

        return panel
    }

    private fun createColorSelectionPanel(): JPanel {
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
        panel.background = UITheme.Colors.BACKGROUND
        panel.border = EmptyBorder(20, 20, 20, 20)

        val titleLabel = JLabel("플레이할 색상을 선택하세요")
        titleLabel.font = UITheme.Fonts.TITLE
        titleLabel.alignmentX = Component.CENTER_ALIGNMENT
        panel.add(titleLabel)
        panel.add(Box.createVerticalStrut(20))

        val buttonPanel = JPanel(GridLayout(1, 2, 20, 0))
        buttonPanel.background = UITheme.Colors.BACKGROUND

        val blackButton = ModernButton("흑돌로 시작", ModernButton.ButtonStyle.PRIMARY)
        blackButton.addActionListener {
            selectedPlayerColor = PlayerDto.BLACK
            result = true
            dispose()
        }
        buttonPanel.add(blackButton)

        val whiteButton = ModernButton("백돌로 시작", ModernButton.ButtonStyle.SECONDARY)
        whiteButton.addActionListener {
            selectedPlayerColor = PlayerDto.WHITE
            result = true
            dispose()
        }
        buttonPanel.add(whiteButton)

        panel.add(buttonPanel)
        panel.add(Box.createVerticalStrut(20))

        val backButton = ModernButton("뒤로가기", ModernButton.ButtonStyle.GHOST)
        backButton.addActionListener { cardLayout.show(mainPanel, MODE_PANEL) }
        backButton.alignmentX = Component.CENTER_ALIGNMENT
        panel.add(backButton)

        return panel
    }

    private fun createGameModeCard(title: String, description: String, icon: Icon?, action: () -> Unit): JPanel { /* ... existing code ... */ }

    fun showDialog(): Pair<GameModeDto?, AIDifficultyDto?> {
        isVisible = true
        return if (result) selectedMode to selectedDifficulty else null to null
    }
}