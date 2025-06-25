package com.omok.presentation.ui.components

import com.omok.presentation.ui.theme.UITheme
import java.awt.*
import javax.swing.*
import javax.swing.border.EmptyBorder

class SplashScreen : JWindow() {
    private val progressBar = JProgressBar()
    private var progress = 0
    private lateinit var timer: Timer
    
    init {
        val contentPane = JPanel(BorderLayout())
        contentPane.background = UITheme.Colors.BACKGROUND
        contentPane.border = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UITheme.Colors.PRIMARY, 2),
            EmptyBorder(UITheme.Spacing.XL, UITheme.Spacing.XL, UITheme.Spacing.XL, UITheme.Spacing.XL)
        )
        
        // Logo and title
        val centerPanel = JPanel()
        centerPanel.layout = BoxLayout(centerPanel, BoxLayout.Y_AXIS)
        centerPanel.background = UITheme.Colors.BACKGROUND
        centerPanel.alignmentX = Component.CENTER_ALIGNMENT
        
        // Logo
        val logoLabel = JLabel("♟")
        logoLabel.font = Font(UITheme.Fonts.TITLE.family, Font.PLAIN, 72)
        logoLabel.foreground = UITheme.Colors.PRIMARY
        logoLabel.alignmentX = Component.CENTER_ALIGNMENT
        centerPanel.add(logoLabel)
        
        centerPanel.add(Box.createVerticalStrut(UITheme.Spacing.MD))
        
        // Title
        val titleLabel = JLabel("렌주룰 오목")
        titleLabel.font = Font(UITheme.Fonts.TITLE.family, Font.BOLD, 28)
        titleLabel.foreground = UITheme.Colors.GRAY_900
        titleLabel.alignmentX = Component.CENTER_ALIGNMENT
        centerPanel.add(titleLabel)
        
        // Subtitle
        val subtitleLabel = JLabel("Renju Gomoku")
        subtitleLabel.font = UITheme.Fonts.SUBHEADING
        subtitleLabel.foreground = UITheme.Colors.GRAY_600
        subtitleLabel.alignmentX = Component.CENTER_ALIGNMENT
        centerPanel.add(subtitleLabel)
        
        centerPanel.add(Box.createVerticalStrut(UITheme.Spacing.XL))
        
        // Version
        val versionLabel = JLabel("Version 2.0")
        versionLabel.font = UITheme.Fonts.CAPTION
        versionLabel.foreground = UITheme.Colors.GRAY_500
        versionLabel.alignmentX = Component.CENTER_ALIGNMENT
        centerPanel.add(versionLabel)
        
        contentPane.add(centerPanel, BorderLayout.CENTER)
        
        // Progress bar
        val bottomPanel = JPanel(BorderLayout())
        bottomPanel.background = UITheme.Colors.BACKGROUND
        bottomPanel.border = EmptyBorder(UITheme.Spacing.LG, 0, 0, 0)
        
        progressBar.preferredSize = Dimension(300, 4)
        progressBar.background = UITheme.Colors.GRAY_200
        progressBar.foreground = UITheme.Colors.PRIMARY
        progressBar.isBorderPainted = false
        progressBar.value = 0
        
        val loadingLabel = JLabel("게임을 준비하고 있습니다...")
        loadingLabel.font = UITheme.Fonts.CAPTION
        loadingLabel.foreground = UITheme.Colors.GRAY_600
        loadingLabel.horizontalAlignment = SwingConstants.CENTER
        
        bottomPanel.add(loadingLabel, BorderLayout.NORTH)
        bottomPanel.add(progressBar, BorderLayout.CENTER)
        
        contentPane.add(bottomPanel, BorderLayout.SOUTH)
        
        setContentPane(contentPane)
        preferredSize = Dimension(350, 400)
        pack()
        setLocationRelativeTo(null)
        
        // Animation timer
        timer = Timer(20) {
            progress += 2
            progressBar.value = progress
            
            if (progress >= 100) {
                timer.stop()
                Timer(200) {
                    isVisible = false
                    dispose()
                }.apply {
                    isRepeats = false
                    start()
                }
            }
        }
    }
    
    fun showSplash() {
        isVisible = true
        timer.start()
    }
}