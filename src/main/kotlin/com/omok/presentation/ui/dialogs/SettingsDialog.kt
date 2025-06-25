package com.omok.presentation.ui.dialogs

import com.omok.presentation.ui.components.ModernButton
import com.omok.presentation.ui.components.ModernDialog
import com.omok.presentation.ui.theme.UITheme
import com.omok.presentation.ui.effects.SoundEffects
import java.awt.*
import javax.swing.*
import javax.swing.border.EmptyBorder
import javax.swing.border.TitledBorder

class SettingsDialog(parent: Frame) : JDialog(parent, "설정", true) {
    private val soundCheckBox = JCheckBox("효과음 사용", SoundEffects.isSoundEnabled())
    private val animationCheckBox = JCheckBox("애니메이션 사용", true)
    private val colorBlindCheckBox = JCheckBox("색맹 모드", false)
    private val highContrastCheckBox = JCheckBox("고대비 모드", false)
    private val showCoordinatesCheckBox = JCheckBox("좌표 항상 표시", true)
    private val showTimerCheckBox = JCheckBox("타이머 표시", true)
    
    init {
        layout = BorderLayout()
        
        val contentPanel = JPanel()
        contentPanel.layout = BoxLayout(contentPanel, BoxLayout.Y_AXIS)
        contentPanel.background = UITheme.Colors.BACKGROUND
        
        // Game Settings
        val gamePanel = createSection("게임 설정")
        gamePanel.add(soundCheckBox)
        gamePanel.add(animationCheckBox)
        gamePanel.add(showCoordinatesCheckBox)
        gamePanel.add(showTimerCheckBox)
        contentPanel.add(gamePanel)
        
        contentPanel.add(Box.createVerticalStrut(UITheme.Spacing.MD))
        
        // Accessibility Settings
        val accessibilityPanel = createSection("접근성")
        accessibilityPanel.add(colorBlindCheckBox)
        accessibilityPanel.add(highContrastCheckBox)
        
        val fontSizePanel = JPanel(FlowLayout(FlowLayout.LEFT))
        fontSizePanel.background = UITheme.Colors.BACKGROUND
        fontSizePanel.add(JLabel("글자 크기:"))
        
        val fontSizeSlider = JSlider(12, 20, 16)
        fontSizeSlider.majorTickSpacing = 2
        fontSizeSlider.paintTicks = true
        fontSizeSlider.paintLabels = true
        fontSizeSlider.background = UITheme.Colors.BACKGROUND
        fontSizePanel.add(fontSizeSlider)
        
        accessibilityPanel.add(fontSizePanel)
        contentPanel.add(accessibilityPanel)
        
        contentPanel.add(Box.createVerticalStrut(UITheme.Spacing.MD))
        
        // Keyboard Shortcuts Info
        val shortcutsPanel = createSection("키보드 단축키")
        val shortcutsInfo = JTextArea(
            """
            새 게임: Ctrl+N
            무르기: Ctrl+Z
            설정: Ctrl+,
            도움말: F1
            종료: Ctrl+Q
            
            보드 탐색: 화살표 키
            돌 놓기: Enter 또는 Space
            """.trimIndent()
        )
        shortcutsInfo.isEditable = false
        shortcutsInfo.background = UITheme.Colors.SURFACE
        shortcutsInfo.font = UITheme.Fonts.BODY_SMALL
        shortcutsInfo.foreground = UITheme.Colors.GRAY_700
        shortcutsInfo.border = EmptyBorder(UITheme.Spacing.SM, UITheme.Spacing.SM, UITheme.Spacing.SM, UITheme.Spacing.SM)
        shortcutsPanel.add(shortcutsInfo)
        contentPanel.add(shortcutsPanel)
        
        // ScrollPane에 추가하기 위해 제거 (아래서 처리)
        
        // Buttons
        val saveButton = ModernButton("저장", ModernButton.ButtonStyle.PRIMARY)
        saveButton.addActionListener {
            saveSettings()
            dispose()
        }
        
        val cancelButton = ModernButton("취소", ModernButton.ButtonStyle.GHOST)
        cancelButton.addActionListener {
            dispose()
        }
        
        val buttonPanel = JPanel(FlowLayout(FlowLayout.RIGHT, UITheme.Spacing.SM, 0))
        buttonPanel.background = UITheme.Colors.SURFACE
        buttonPanel.border = EmptyBorder(UITheme.Spacing.SM, UITheme.Spacing.MD, UITheme.Spacing.SM, UITheme.Spacing.MD)
        buttonPanel.add(cancelButton)
        buttonPanel.add(saveButton)
        
        add(buttonPanel, BorderLayout.SOUTH)
        
        // 컨텐츠 크기에 따라 자동 크기 조정
        val scrollPane = JScrollPane(contentPanel)
        scrollPane.border = BorderFactory.createEmptyBorder()
        scrollPane.verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
        // Let content determine the size
        
        add(scrollPane, BorderLayout.CENTER)
        
        pack()
        // Ensure minimum size for readability
        minimumSize = Dimension(400, 300)
        setLocationRelativeTo(parent)
    }
    
    private fun createSection(title: String): JPanel {
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
        panel.background = UITheme.Colors.BACKGROUND
        panel.border = BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(UITheme.Colors.GRAY_300),
                title,
                TitledBorder.LEFT,
                TitledBorder.TOP,
                UITheme.Fonts.BODY,
                UITheme.Colors.GRAY_700
            ),
            EmptyBorder(UITheme.Spacing.SM, UITheme.Spacing.MD, UITheme.Spacing.SM, UITheme.Spacing.MD)
        )
        return panel
    }
    
    private fun saveSettings() {
        SoundEffects.setSoundEnabled(soundCheckBox.isSelected)
        // Save other settings...
    }
}