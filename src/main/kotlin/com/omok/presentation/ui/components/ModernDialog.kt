package com.omok.presentation.ui.components

import com.omok.presentation.ui.theme.UITheme
import java.awt.*
import javax.swing.*
import javax.swing.border.EmptyBorder

class ModernDialog(
    parent: Frame?,
    title: String,
    modal: Boolean = true
) : JDialog(parent, title, modal) {
    
    private val contentPanel = JPanel()
    private val buttonPanel = JPanel()
    
    init {
        isUndecorated = false
        defaultCloseOperation = DISPOSE_ON_CLOSE
        
        rootPane.border = UITheme.Elevation.getElevation(3)
        
        layout = BorderLayout()
        
        val headerPanel = createHeaderPanel(title)
        add(headerPanel, BorderLayout.NORTH)
        
        contentPanel.background = UITheme.Colors.BACKGROUND
        contentPanel.border = EmptyBorder(UITheme.Spacing.LG, UITheme.Spacing.LG, UITheme.Spacing.MD, UITheme.Spacing.LG)
        add(contentPanel, BorderLayout.CENTER)
        
        buttonPanel.background = UITheme.Colors.SURFACE
        buttonPanel.border = BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, UITheme.Colors.GRAY_200),
            EmptyBorder(UITheme.Spacing.MD, UITheme.Spacing.LG, UITheme.Spacing.MD, UITheme.Spacing.LG)
        )
        buttonPanel.layout = FlowLayout(FlowLayout.RIGHT, UITheme.Spacing.SM, 0)
        add(buttonPanel, BorderLayout.SOUTH)
        
        setLocationRelativeTo(parent)
    }
    
    private fun createHeaderPanel(title: String): JPanel {
        val panel = JPanel(BorderLayout())
        panel.background = UITheme.Colors.SURFACE
        panel.border = BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, UITheme.Colors.GRAY_200),
            EmptyBorder(UITheme.Spacing.MD, UITheme.Spacing.LG, UITheme.Spacing.MD, UITheme.Spacing.LG)
        )
        
        val titleLabel = JLabel(title)
        titleLabel.font = UITheme.Fonts.HEADING
        titleLabel.foreground = UITheme.Colors.GRAY_900
        panel.add(titleLabel, BorderLayout.WEST)
        
        return panel
    }
    
    fun setContent(component: JComponent) {
        contentPanel.removeAll()
        contentPanel.layout = BorderLayout()
        contentPanel.add(component, BorderLayout.CENTER)
        revalidate()
        repaint()
    }
    
    fun addButton(button: ModernButton) {
        buttonPanel.add(button)
    }
    
    companion object {
        fun showOptions(
            parent: Component?,
            title: String,
            message: String,
            options: Array<String>,
            defaultOption: String? = null
        ): String? {
            val dialog = ModernDialog(parent as? Frame, title, true)
            
            val contentPanel = JPanel(BorderLayout())
            contentPanel.background = UITheme.Colors.BACKGROUND
            
            val messageLabel = JLabel(message)
            messageLabel.font = UITheme.Fonts.BODY
            messageLabel.foreground = UITheme.Colors.GRAY_700
            messageLabel.border = EmptyBorder(0, 0, UITheme.Spacing.LG, 0)
            contentPanel.add(messageLabel, BorderLayout.NORTH)
            
            val optionsPanel = JPanel(GridLayout(0, 1, 0, UITheme.Spacing.SM))
            optionsPanel.background = UITheme.Colors.BACKGROUND
            
            val buttonGroup = ButtonGroup()
            val radioButtons = mutableListOf<JRadioButton>()
            
            for (option in options) {
                val radioButton = JRadioButton(option)
                radioButton.font = UITheme.Fonts.BODY
                radioButton.foreground = UITheme.Colors.GRAY_800
                radioButton.background = UITheme.Colors.BACKGROUND
                radioButton.isSelected = option == (defaultOption ?: options.firstOrNull())
                buttonGroup.add(radioButton)
                radioButtons.add(radioButton)
                optionsPanel.add(radioButton)
            }
            
            contentPanel.add(optionsPanel, BorderLayout.CENTER)
            dialog.setContent(contentPanel)
            
            var result: String? = null
            
            val confirmButton = ModernButton("확인", ModernButton.ButtonStyle.PRIMARY)
            confirmButton.addActionListener {
                result = radioButtons.firstOrNull { it.isSelected }?.text
                dialog.dispose()
            }
            
            val cancelButton = ModernButton("취소", ModernButton.ButtonStyle.GHOST)
            cancelButton.addActionListener {
                dialog.dispose()
            }
            
            dialog.addButton(cancelButton)
            dialog.addButton(confirmButton)
            
            dialog.preferredSize = Dimension(400, 250)
            dialog.pack()
            dialog.isVisible = true
            
            return result
        }
        
        fun showInfo(
            parent: Component?,
            title: String,
            htmlContent: String
        ) {
            val dialog = ModernDialog(parent as? Frame, title, true)
            
            val textPane = JEditorPane("text/html", htmlContent)
            textPane.isEditable = false
            textPane.background = UITheme.Colors.BACKGROUND
            textPane.border = EmptyBorder(0, 0, 0, 0)
            textPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true)
            textPane.font = UITheme.Fonts.BODY
            
            val scrollPane = JScrollPane(textPane)
            scrollPane.border = BorderFactory.createEmptyBorder()
            scrollPane.verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
            
            dialog.setContent(scrollPane)
            
            val okButton = ModernButton("확인", ModernButton.ButtonStyle.PRIMARY)
            okButton.addActionListener {
                dialog.dispose()
            }
            
            dialog.addButton(okButton)
            
            dialog.preferredSize = Dimension(500, 400)
            dialog.pack()
            dialog.isVisible = true
        }
        
        fun showWarning(
            parent: Component?,
            title: String,
            message: String
        ) {
            val dialog = ModernDialog(parent as? Frame, title, true)
            
            val contentPanel = JPanel(BorderLayout())
            contentPanel.background = UITheme.Colors.BACKGROUND
            
            val iconLabel = JLabel("⚠")
            iconLabel.font = Font(UITheme.Fonts.TITLE.family, Font.PLAIN, 40)
            iconLabel.foreground = UITheme.Colors.WARNING
            iconLabel.horizontalAlignment = SwingConstants.CENTER
            iconLabel.border = EmptyBorder(0, 0, UITheme.Spacing.MD, 0)
            contentPanel.add(iconLabel, BorderLayout.NORTH)
            
            val messageLabel = JLabel("<html><center>$message</center></html>")
            messageLabel.font = UITheme.Fonts.BODY
            messageLabel.foreground = UITheme.Colors.GRAY_700
            messageLabel.horizontalAlignment = SwingConstants.CENTER
            contentPanel.add(messageLabel, BorderLayout.CENTER)
            
            dialog.setContent(contentPanel)
            
            val okButton = ModernButton("확인", ModernButton.ButtonStyle.PRIMARY)
            okButton.addActionListener {
                dialog.dispose()
            }
            
            dialog.addButton(okButton)
            
            dialog.preferredSize = Dimension(350, 200)
            dialog.pack()
            dialog.isVisible = true
        }
    }
}