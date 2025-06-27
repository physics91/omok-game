package com.omok.presentation.ui.achievement

import com.omok.domain.achievement.AchievementManager
import com.omok.presentation.ui.theme.UITheme
import com.omok.presentation.ui.components.ModernButton
import java.awt.*
import javax.swing.*
import javax.swing.border.EmptyBorder

/**
 * 성취도 다이얼로그
 */
class AchievementDialog(
    parent: Frame,
    private val achievementManager: AchievementManager
) : JDialog(parent, "성취도", true) {
    
    private val achievementPanel = AchievementPanel(achievementManager)
    
    init {
        layout = BorderLayout()
        preferredSize = Dimension(1000, 700)
        
        setupUI()
        
        pack()
        setLocationRelativeTo(parent)
    }
    
    private fun setupUI() {
        // 메인 컨텐츠
        add(achievementPanel, BorderLayout.CENTER)
        
        // 하단 버튼 패널
        val buttonPanel = createButtonPanel()
        add(buttonPanel, BorderLayout.SOUTH)
    }
    
    private fun createButtonPanel(): JPanel {
        val panel = JPanel(FlowLayout(FlowLayout.RIGHT))
        panel.background = UITheme.Colors.SURFACE
        panel.border = EmptyBorder(10, 10, 10, 10)
        
        val closeButton = ModernButton("닫기", ModernButton.ButtonStyle.SECONDARY)
        closeButton.addActionListener { dispose() }
        
        panel.add(closeButton)
        
        return panel
    }
}