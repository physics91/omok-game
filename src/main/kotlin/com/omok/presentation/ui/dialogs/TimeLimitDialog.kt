package com.omok.presentation.ui.dialogs

import com.omok.application.dto.*
import com.omok.presentation.ui.components.ModernButton
import com.omok.presentation.ui.theme.UITheme
import java.awt.*
import javax.swing.*
import javax.swing.border.EmptyBorder

/**
 * 시간 제한 설정 다이얼로그
 */
class TimeLimitDialog(parent: Frame) : JDialog(parent, "시간 제한 설정", true) {
    
    private var selectedTimeLimit: TimeLimitDto? = TimeLimitDto.NONE
    private val buttonGroup = ButtonGroup()
    
    init {
        initializeUI()
    }
    
    private fun initializeUI() {
        defaultCloseOperation = DISPOSE_ON_CLOSE
        layout = BorderLayout()
        
        val contentPanel = JPanel()
        contentPanel.layout = BoxLayout(contentPanel, BoxLayout.Y_AXIS)
        contentPanel.background = UITheme.Colors.BACKGROUND
        contentPanel.border = EmptyBorder(UITheme.Spacing.MD, UITheme.Spacing.MD, UITheme.Spacing.MD, UITheme.Spacing.MD)
        
        // 제목
        val titleLabel = JLabel("시간 제한 모드를 선택하세요")
        titleLabel.font = UITheme.Fonts.HEADING
        titleLabel.foreground = UITheme.Colors.GRAY_900
        titleLabel.alignmentX = Component.LEFT_ALIGNMENT
        contentPanel.add(titleLabel)
        contentPanel.add(Box.createRigidArea(Dimension(0, UITheme.Spacing.MD)))
        
        // 미리 정의된 시간 제한들
        val presets = listOf(
            TimeLimitDto.NONE to "제한 없음 - 시간 제한이 없습니다",
            TimeLimitDto(180L, 0L, TimeLimitModeDto.TOTAL_TIME) to "블리츠 3분 - 총 3분",
            TimeLimitDto(300L, 3L, TimeLimitModeDto.FISCHER) to "5+3 피셔 - 5분 + 수당 3초",
            TimeLimitDto(600L, 0L, TimeLimitModeDto.TOTAL_TIME) to "래피드 10분 - 총 10분",
            TimeLimitDto(600L, 30L, TimeLimitModeDto.BYOYOMI) to "10+30초 초읽기 - 10분 + 초읽기 30초",
            TimeLimitDto(1800L, 0L, TimeLimitModeDto.TOTAL_TIME) to "클래시컬 30분 - 총 30분"
        )
        
        for ((timeLimit, description) in presets) {
            val radioButton = createTimeLimitOption(timeLimit, description)
            if (timeLimit == TimeLimitDto.NONE) {
                radioButton.isSelected = true
                selectedTimeLimit = timeLimit
            }
            contentPanel.add(radioButton)
            contentPanel.add(Box.createRigidArea(Dimension(0, UITheme.Spacing.SM)))
        }
        
        val scrollPane = JScrollPane(contentPanel)
        scrollPane.border = null
        scrollPane.verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
        scrollPane.preferredSize = Dimension(500, 400)
        
        add(scrollPane, BorderLayout.CENTER)
        
        // 버튼 패널
        val buttonPanel = JPanel(FlowLayout(FlowLayout.RIGHT))
        buttonPanel.background = UITheme.Colors.BACKGROUND
        
        val confirmButton = ModernButton("확인", ModernButton.ButtonStyle.PRIMARY)
        confirmButton.addActionListener {
            dispose()
        }
        
        val cancelButton = ModernButton("취소", ModernButton.ButtonStyle.GHOST)
        cancelButton.addActionListener {
            selectedTimeLimit = null
            dispose()
        }
        
        buttonPanel.add(cancelButton)
        buttonPanel.add(confirmButton)
        add(buttonPanel, BorderLayout.SOUTH)
    }
    
    private fun createTimeLimitOption(timeLimit: TimeLimitDto, description: String): JRadioButton {
        val radioButton = JRadioButton()
        radioButton.font = UITheme.Fonts.BODY
        radioButton.foreground = UITheme.Colors.GRAY_900
        radioButton.background = UITheme.Colors.BACKGROUND
        radioButton.alignmentX = Component.LEFT_ALIGNMENT
        radioButton.maximumSize = Dimension(Integer.MAX_VALUE, radioButton.preferredSize.height)
        
        // 라디오 버튼 텍스트 설정
        val displayText = if (timeLimit.mode == TimeLimitModeDto.NONE) {
            description
        } else {
            val timeText = formatTimeForDisplay(timeLimit)
            "$timeText - $description"
        }
        radioButton.text = displayText
        
        buttonGroup.add(radioButton)
        radioButton.addActionListener {
            selectedTimeLimit = timeLimit
        }
        
        return radioButton
    }
    
    private fun formatTimeForDisplay(timeLimit: TimeLimitDto): String {
        val minutes = timeLimit.totalTimePerPlayer / 60
        return when (timeLimit.mode) {
            TimeLimitModeDto.NONE -> "제한 없음"
            TimeLimitModeDto.TOTAL_TIME -> "${minutes}분"
            TimeLimitModeDto.FISCHER -> "${minutes}+${timeLimit.incrementPerMove} 피셔"
            TimeLimitModeDto.BYOYOMI -> "${minutes}+${timeLimit.incrementPerMove}초 초읽기"
        }
    }
    
    fun getSelectedTimeLimit(): TimeLimitDto? = selectedTimeLimit
    
    companion object {
        fun showDialog(parent: Frame): TimeLimitDto? {
            val dialog = TimeLimitDialog(parent)
            dialog.pack()
            dialog.setLocationRelativeTo(parent)
            dialog.isVisible = true
            return dialog.getSelectedTimeLimit()
        }
    }
}