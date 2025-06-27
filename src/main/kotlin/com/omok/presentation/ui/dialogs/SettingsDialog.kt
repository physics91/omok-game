package com.omok.presentation.ui.dialogs

import com.omok.presentation.ui.components.ModernButton
import com.omok.presentation.ui.components.ModernDialog
import com.omok.presentation.ui.theme.UITheme
import com.omok.presentation.ui.effects.SoundEffects
import com.omok.presentation.ui.settings.UIGameSettings
import com.omok.presentation.ui.settings.AIThinkingTime
import com.omok.presentation.ui.settings.AISettingsPanel
import java.awt.*
import javax.swing.*
import javax.swing.border.EmptyBorder
import javax.swing.border.TitledBorder

class SettingsDialog(parent: Frame) : JDialog(parent, "설정", true) {
    private val currentSettings = UIGameSettings.getInstance()
    
    private val soundCheckBox = JCheckBox("효과음 사용", currentSettings.soundEnabled)
    private val animationCheckBox = JCheckBox("애니메이션 사용", currentSettings.animationEnabled)
    private val colorBlindCheckBox = JCheckBox("색맹 모드", false)
    private val highContrastCheckBox = JCheckBox("고대비 모드", false)
    private val showCoordinatesCheckBox = JCheckBox("좌표 항상 표시", currentSettings.showCoordinates)
    private val showTimerCheckBox = JCheckBox("타이머 표시", true)
    private val aiThinkingTimeCombo = JComboBox(AIThinkingTime.values())
    
    init {
        layout = BorderLayout()
        preferredSize = Dimension(600, 500)
        
        // AI 사고시간 콤보박스 설정
        aiThinkingTimeCombo.selectedItem = currentSettings.aiThinkingTime
        aiThinkingTimeCombo.renderer = object : DefaultListCellRenderer() {
            override fun getListCellRendererComponent(
                list: JList<*>?, value: Any?, index: Int,
                isSelected: Boolean, cellHasFocus: Boolean
            ): Component {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
                if (value is AIThinkingTime) {
                    text = value.displayName
                }
                return this
            }
        }
        
        // 탭 패널 생성
        val tabbedPane = JTabbedPane()
        tabbedPane.font = UITheme.Fonts.BUTTON
        
        // 일반 설정 탭
        val generalPanel = createGeneralSettingsPanel()
        tabbedPane.addTab("일반", generalPanel)
        
        // AI 설정 탭
        val aiSettingsPanel = AISettingsPanel()
        tabbedPane.addTab("AI 설정", aiSettingsPanel)
        
        // 접근성 탭
        val accessibilityPanel = createAccessibilityPanel()
        tabbedPane.addTab("접근성", accessibilityPanel)
        
        add(tabbedPane, BorderLayout.CENTER)
        
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
        
        pack()
        setLocationRelativeTo(parent)
    }
    
    private fun createGeneralSettingsPanel(): Component {
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
        panel.background = UITheme.Colors.BACKGROUND
        panel.border = EmptyBorder(UITheme.Spacing.MD, UITheme.Spacing.MD, UITheme.Spacing.MD, UITheme.Spacing.MD)
        
        // 게임 설정
        val gamePanel = createSection("게임 설정")
        gamePanel.add(soundCheckBox)
        gamePanel.add(animationCheckBox)
        gamePanel.add(showCoordinatesCheckBox)
        gamePanel.add(showTimerCheckBox)
        
        // AI 사고시간 설정
        val aiPanel = JPanel(FlowLayout(FlowLayout.LEFT))
        aiPanel.background = UITheme.Colors.BACKGROUND
        aiPanel.add(JLabel("AI 사고시간:"))
        aiPanel.add(aiThinkingTimeCombo)
        gamePanel.add(aiPanel)
        
        panel.add(gamePanel)
        panel.add(Box.createVerticalStrut(UITheme.Spacing.MD))
        
        // 키보드 단축키 정보
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
        panel.add(shortcutsPanel)
        
        // 스크롤 가능하도록 래핑
        val scrollPane = JScrollPane(panel)
        scrollPane.border = null
        scrollPane.verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
        return scrollPane
    }
    
    private fun createAccessibilityPanel(): Component {
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
        panel.background = UITheme.Colors.BACKGROUND
        panel.border = EmptyBorder(UITheme.Spacing.MD, UITheme.Spacing.MD, UITheme.Spacing.MD, UITheme.Spacing.MD)
        
        // 접근성 설정
        val accessibilitySection = createSection("접근성")
        accessibilitySection.add(colorBlindCheckBox)
        accessibilitySection.add(highContrastCheckBox)
        
        val fontSizePanel = JPanel(FlowLayout(FlowLayout.LEFT))
        fontSizePanel.background = UITheme.Colors.BACKGROUND
        fontSizePanel.add(JLabel("글자 크기:"))
        
        val fontSizeSlider = JSlider(12, 20, 16)
        fontSizeSlider.majorTickSpacing = 2
        fontSizeSlider.paintTicks = true
        fontSizeSlider.paintLabels = true
        fontSizeSlider.background = UITheme.Colors.BACKGROUND
        fontSizePanel.add(fontSizeSlider)
        
        accessibilitySection.add(fontSizePanel)
        panel.add(accessibilitySection)
        
        // 스크롤 가능하도록 래핑
        val scrollPane = JScrollPane(panel)
        scrollPane.border = null
        scrollPane.verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
        return scrollPane
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
        // 새 설정 생성
        val newSettings = currentSettings.copy(
            soundEnabled = soundCheckBox.isSelected,
            animationEnabled = animationCheckBox.isSelected,
            showCoordinates = showCoordinatesCheckBox.isSelected,
            aiThinkingTime = aiThinkingTimeCombo.selectedItem as AIThinkingTime
        )
        
        // 설정 저장
        UIGameSettings.updateSettings(newSettings)
        SoundEffects.setSoundEnabled(newSettings.soundEnabled)
    }
}