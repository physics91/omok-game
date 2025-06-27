package com.omok.presentation.ui.components

import com.omok.presentation.ui.theme.UITheme
import java.awt.*
import java.awt.event.KeyEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.*
import javax.swing.border.EmptyBorder

/**
 * 통합된 다이얼로그 시스템
 * 모든 팝업 UI를 표준화하고 일관성 있는 UX 제공
 */
class UnifiedDialog private constructor(
    parent: Window?,
    private val config: DialogConfig
) : JDialog(parent, config.title, ModalityType.APPLICATION_MODAL) {
    
    private var result: DialogResult = DialogResult.CANCELLED
    private val contentContainer = JPanel(BorderLayout())
    
    init {
        setupDialog()
        setupKeyBindings()
        setupContent()
        
        pack()
        setLocationRelativeTo(parent)
        
        // 페이드 인 애니메이션 효과
        setupFadeInAnimation()
    }
    
    private fun setupFadeInAnimation() {
        try {
            // decorated dialog에서는 opacity 설정이 제한적이므로 try-catch로 처리
            opacity = 0.0f
            isVisible = true
            
            val timer = javax.swing.Timer(10) { e ->
                try {
                    opacity = (opacity + 0.05f).coerceAtMost(1.0f)
                    if (opacity >= 1.0f) {
                        (e.source as javax.swing.Timer).stop()
                    }
                } catch (ex: IllegalComponentStateException) {
                    // 플랫폼에서 opacity 설정을 지원하지 않는 경우 애니메이션 중단
                    (e.source as javax.swing.Timer).stop()
                    opacity = 1.0f
                }
            }
            timer.start()
        } catch (ex: IllegalComponentStateException) {
            // opacity 설정이 불가능한 경우 즉시 표시
            isVisible = true
        }
    }
    
    private fun setupDialog() {
        isResizable = config.resizable
        defaultCloseOperation = DO_NOTHING_ON_CLOSE
        
        // 창 닫기 이벤트 처리
        addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent) {
                if (config.allowEscapeToClose) {
                    result = DialogResult.CANCELLED
                    dispose()
                }
            }
        })
    }
    
    private fun setupKeyBindings() {
        val rootPane = this.rootPane
        
        // ESC 키로 닫기
        if (config.allowEscapeToClose) {
            rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "escape"
            )
            rootPane.actionMap.put("escape", object : javax.swing.AbstractAction() {
                override fun actionPerformed(e: java.awt.event.ActionEvent?) {
                    result = DialogResult.CANCELLED
                    dispose()
                }
            })
        }
        
        // Enter 키로 확인
        if (config.allowEnterToConfirm) {
            rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "confirm"
            )
            rootPane.actionMap.put("confirm", object : javax.swing.AbstractAction() {
                override fun actionPerformed(e: java.awt.event.ActionEvent?) {
                    result = DialogResult.CONFIRMED
                    dispose()
                }
            })
        }
    }
    
    private fun setupContent() {
        layout = BorderLayout()
        background = UITheme.Colors.BACKGROUND
        
        // 헤더 (선택적)
        config.headerContent?.let { header ->
            val headerPanel = createHeaderPanel(header)
            add(headerPanel, BorderLayout.NORTH)
        }
        
        // 메인 컨텐츠
        contentContainer.background = UITheme.Colors.BACKGROUND
        contentContainer.border = EmptyBorder(
            if (config.headerContent == null) 20 else 0, 
            20, 20, 20
        )
        add(contentContainer, BorderLayout.CENTER)
        
        // 버튼 패널
        if (config.buttons.isNotEmpty()) {
            val buttonPanel = createButtonPanel()
            add(buttonPanel, BorderLayout.SOUTH)
        }
        
        // 컨텐츠 설정
        setContent(config.content)
    }
    
    private fun createHeaderPanel(header: DialogHeader): JPanel {
        val panel = JPanel(BorderLayout())
        panel.background = config.headerBackgroundColor ?: UITheme.Colors.PRIMARY
        panel.border = EmptyBorder(20, 20, 15, 20)
        
        // 아이콘
        header.icon?.let { icon ->
            val iconLabel = JLabel(icon)
            iconLabel.border = EmptyBorder(0, 0, 0, 15)
            panel.add(iconLabel, BorderLayout.WEST)
        }
        
        // 텍스트 영역
        val textPanel = JPanel()
        textPanel.layout = BoxLayout(textPanel, BoxLayout.Y_AXIS)
        textPanel.isOpaque = false
        
        val titleLabel = JLabel(header.title)
        titleLabel.font = UITheme.Fonts.TITLE
        titleLabel.foreground = header.titleColor ?: Color.WHITE
        titleLabel.alignmentX = Component.LEFT_ALIGNMENT
        textPanel.add(titleLabel)
        
        header.subtitle?.let { subtitle ->
            textPanel.add(Box.createVerticalStrut(5))
            val subtitleLabel = JLabel(subtitle)
            subtitleLabel.font = UITheme.Fonts.BODY
            subtitleLabel.foreground = header.subtitleColor ?: Color(255, 255, 255, 200)
            subtitleLabel.alignmentX = Component.LEFT_ALIGNMENT
            textPanel.add(subtitleLabel)
        }
        
        panel.add(textPanel, BorderLayout.CENTER)
        
        // 닫기 버튼 (선택적)
        if (config.showCloseButton) {
            val closeButton = ModernButton("×", ModernButton.ButtonStyle.GHOST)
            closeButton.foreground = Color.WHITE
            closeButton.preferredSize = Dimension(30, 30)
            closeButton.addActionListener {
                result = DialogResult.CANCELLED
                dispose()
            }
            panel.add(closeButton, BorderLayout.EAST)
        }
        
        return panel
    }
    
    private fun createButtonPanel(): JPanel {
        val panel = JPanel(FlowLayout(FlowLayout.RIGHT, 10, 0))
        panel.background = UITheme.Colors.SURFACE
        panel.border = EmptyBorder(15, 20, 20, 20)
        
        config.buttons.forEach { buttonConfig ->
            val button = ModernButton(buttonConfig.text, buttonConfig.style)
            
            if (buttonConfig.isDefault) {
                rootPane.defaultButton = button
            }
            
            button.addActionListener {
                result = buttonConfig.result
                buttonConfig.action?.invoke()
                if (buttonConfig.closeOnClick) {
                    dispose()
                }
            }
            
            panel.add(button)
        }
        
        return panel
    }
    
    fun setContent(content: JComponent) {
        contentContainer.removeAll()
        contentContainer.add(content, BorderLayout.CENTER)
        revalidate()
        repaint()
    }
    
    fun getResult(): DialogResult = result
    
    companion object {
        /**
         * 정보 다이얼로그
         */
        fun showInfo(
            parent: Window? = null,
            title: String,
            message: String,
            icon: Icon? = null
        ): DialogResult {
            val messageLabel = JLabel("<html><div style='width: 300px;'>$message</div></html>")
            messageLabel.font = UITheme.Fonts.BODY
            messageLabel.foreground = UITheme.Colors.GRAY_900
            
            val config = DialogConfig(
                title = title,
                content = messageLabel,
                buttons = listOf(
                    ButtonConfig("확인", ModernButton.ButtonStyle.PRIMARY, DialogResult.CONFIRMED, isDefault = true)
                ),
                headerContent = DialogHeader(title, icon = icon),
                headerBackgroundColor = UITheme.Colors.PRIMARY
            )
            
            return UnifiedDialog(parent, config).apply { isVisible = true }.getResult()
        }
        
        /**
         * 확인 다이얼로그
         */
        fun showConfirm(
            parent: Window? = null,
            title: String,
            message: String,
            icon: Icon? = null,
            confirmText: String = "확인",
            cancelText: String = "취소"
        ): DialogResult {
            val messageLabel = JLabel("<html><div style='width: 300px;'>$message</div></html>")
            messageLabel.font = UITheme.Fonts.BODY
            messageLabel.foreground = UITheme.Colors.GRAY_900
            
            val config = DialogConfig(
                title = title,
                content = messageLabel,
                buttons = listOf(
                    ButtonConfig(cancelText, ModernButton.ButtonStyle.GHOST, DialogResult.CANCELLED),
                    ButtonConfig(confirmText, ModernButton.ButtonStyle.PRIMARY, DialogResult.CONFIRMED, isDefault = true)
                ),
                headerContent = DialogHeader(title, icon = icon),
                headerBackgroundColor = UITheme.Colors.WARNING
            )
            
            return UnifiedDialog(parent, config).apply { isVisible = true }.getResult()
        }
        
        /**
         * 에러 다이얼로그
         */
        fun showError(
            parent: Window? = null,
            title: String = "오류",
            message: String,
            icon: Icon? = null
        ): DialogResult {
            val messageLabel = JLabel("<html><div style='width: 300px;'>$message</div></html>")
            messageLabel.font = UITheme.Fonts.BODY
            messageLabel.foreground = UITheme.Colors.GRAY_900
            
            val config = DialogConfig(
                title = title,
                content = messageLabel,
                buttons = listOf(
                    ButtonConfig("확인", ModernButton.ButtonStyle.PRIMARY, DialogResult.CONFIRMED, isDefault = true)
                ),
                headerContent = DialogHeader(title, icon = icon),
                headerBackgroundColor = UITheme.Colors.DANGER
            )
            
            return UnifiedDialog(parent, config).apply { isVisible = true }.getResult()
        }
        
        /**
         * 커스텀 다이얼로그
         */
        fun showCustom(
            parent: Window? = null,
            config: DialogConfig
        ): UnifiedDialog {
            return UnifiedDialog(parent, config)
        }
        
        /**
         * 선택 다이얼로그
         */
        fun showSelection(
            parent: Window? = null,
            title: String,
            message: String,
            options: Array<String>,
            defaultOption: String? = null
        ): Pair<DialogResult, String?> {
            val selectionPanel = JPanel(BorderLayout(0, 10))
            selectionPanel.isOpaque = false
            
            val messageLabel = JLabel("<html><div style='width: 300px;'>$message</div></html>")
            messageLabel.font = UITheme.Fonts.BODY
            messageLabel.foreground = UITheme.Colors.GRAY_900
            
            val buttonGroup = ButtonGroup()
            val optionsPanel = JPanel()
            optionsPanel.layout = BoxLayout(optionsPanel, BoxLayout.Y_AXIS)
            optionsPanel.isOpaque = false
            
            var selectedOption: String? = defaultOption
            
            options.forEach { option ->
                val radioButton = JRadioButton(option)
                radioButton.font = UITheme.Fonts.BODY
                radioButton.foreground = UITheme.Colors.GRAY_900
                radioButton.isOpaque = false
                radioButton.isSelected = (option == defaultOption)
                
                radioButton.addActionListener {
                    selectedOption = option
                }
                
                buttonGroup.add(radioButton)
                optionsPanel.add(radioButton)
                optionsPanel.add(Box.createVerticalStrut(5))
            }
            
            selectionPanel.add(messageLabel, BorderLayout.NORTH)
            selectionPanel.add(optionsPanel, BorderLayout.CENTER)
            
            val config = DialogConfig(
                title = title,
                content = selectionPanel,
                buttons = listOf(
                    ButtonConfig("취소", ModernButton.ButtonStyle.GHOST, DialogResult.CANCELLED),
                    ButtonConfig("확인", ModernButton.ButtonStyle.PRIMARY, DialogResult.CONFIRMED, isDefault = true)
                ),
                headerContent = DialogHeader(title)
            )
            
            val dialog = UnifiedDialog(parent, config)
            dialog.isVisible = true
            
            return dialog.getResult() to selectedOption
        }
        
        /**
         * 입력 다이얼로그
         */
        fun showInput(
            parent: Window? = null,
            title: String,
            message: String,
            defaultValue: String = "",
            placeholder: String = ""
        ): Pair<DialogResult, String> {
            val inputField = JTextField(defaultValue, 20)
            inputField.font = UITheme.Fonts.BODY
            
            if (placeholder.isNotEmpty() && defaultValue.isEmpty()) {
                // 플레이스홀더 효과 구현 가능
            }
            
            val panel = JPanel(BorderLayout(0, 10))
            panel.isOpaque = false
            
            val messageLabel = JLabel("<html><div style='width: 300px;'>$message</div></html>")
            messageLabel.font = UITheme.Fonts.BODY
            messageLabel.foreground = UITheme.Colors.GRAY_900
            
            panel.add(messageLabel, BorderLayout.NORTH)
            panel.add(inputField, BorderLayout.CENTER)
            
            val config = DialogConfig(
                title = title,
                content = panel,
                buttons = listOf(
                    ButtonConfig("취소", ModernButton.ButtonStyle.GHOST, DialogResult.CANCELLED),
                    ButtonConfig("확인", ModernButton.ButtonStyle.PRIMARY, DialogResult.CONFIRMED, isDefault = true)
                ),
                headerContent = DialogHeader(title),
                allowEnterToConfirm = true
            )
            
            val dialog = UnifiedDialog(parent, config)
            
            // 포커스를 입력 필드로
            SwingUtilities.invokeLater { inputField.requestFocusInWindow() }
            
            dialog.isVisible = true
            
            return dialog.getResult() to inputField.text
        }
    }
}

/**
 * 다이얼로그 설정
 */
data class DialogConfig(
    val title: String,
    val content: JComponent,
    val buttons: List<ButtonConfig> = emptyList(),
    val headerContent: DialogHeader? = null,
    val headerBackgroundColor: Color? = null,
    val resizable: Boolean = false,
    val allowEscapeToClose: Boolean = true,
    val allowEnterToConfirm: Boolean = false,
    val showCloseButton: Boolean = false
)

/**
 * 다이얼로그 헤더
 */
data class DialogHeader(
    val title: String,
    val subtitle: String? = null,
    val icon: Icon? = null,
    val titleColor: Color? = null,
    val subtitleColor: Color? = null
)

/**
 * 버튼 설정
 */
data class ButtonConfig(
    val text: String,
    val style: ModernButton.ButtonStyle,
    val result: DialogResult,
    val action: (() -> Unit)? = null,
    val closeOnClick: Boolean = true,
    val isDefault: Boolean = false
)

/**
 * 다이얼로그 결과
 */
enum class DialogResult {
    CONFIRMED,
    CANCELLED,
    YES,
    NO,
    CUSTOM
}