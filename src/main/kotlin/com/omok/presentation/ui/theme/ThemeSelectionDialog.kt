package com.omok.presentation.ui.theme

import com.omok.presentation.ui.components.ModernButton
import com.omok.presentation.ui.components.UnifiedDialog
import com.omok.presentation.ui.effects.AdvancedSoundEffects
import com.omok.domain.model.Position
import com.omok.domain.model.Player
import java.awt.*
import java.awt.image.BufferedImage
import javax.swing.*
import javax.swing.border.EmptyBorder
import javax.swing.border.LineBorder
import javax.swing.border.TitledBorder
import kotlin.math.min

/**
 * 테마 선택 다이얼로그
 */
class ThemeSelectionDialog(parent: Frame) : JDialog(parent, "테마 선택", true) {
    
    private val themeCards = mutableListOf<ThemeCard>()
    private var selectedTheme: GameTheme = ThemeManager.getCurrentTheme()
    
    init {
        layout = BorderLayout()
        preferredSize = Dimension(900, 600)
        
        // 헤더
        add(createHeader(), BorderLayout.NORTH)
        
        // 테마 목록
        add(createThemePanel(), BorderLayout.CENTER)
        
        // 버튼 패널
        add(createButtonPanel(), BorderLayout.SOUTH)
        
        pack()
        setLocationRelativeTo(parent)
    }
    
    private fun createHeader(): JPanel {
        val panel = JPanel(BorderLayout())
        panel.background = UITheme.Colors.SURFACE
        panel.border = EmptyBorder(20, 20, 20, 20)
        
        val titleLabel = JLabel("테마 선택")
        titleLabel.font = UITheme.Fonts.TITLE
        titleLabel.foreground = UITheme.Colors.GRAY_900
        
        val descLabel = JLabel("게임의 외관을 변경할 테마를 선택하세요")
        descLabel.font = UITheme.Fonts.BODY
        descLabel.foreground = UITheme.Colors.GRAY_600
        
        val textPanel = JPanel()
        textPanel.layout = BoxLayout(textPanel, BoxLayout.Y_AXIS)
        textPanel.isOpaque = false
        textPanel.add(titleLabel)
        textPanel.add(Box.createVerticalStrut(5))
        textPanel.add(descLabel)
        
        panel.add(textPanel, BorderLayout.WEST)
        
        // 커스텀 테마 추가 버튼
        val addCustomButton = ModernButton("커스텀 테마 추가", ModernButton.ButtonStyle.GHOST)
        addCustomButton.addActionListener { showCustomThemeDialog() }
        panel.add(addCustomButton, BorderLayout.EAST)
        
        return panel
    }
    
    private fun createThemePanel(): JScrollPane {
        val panel = JPanel(GridBagLayout())
        panel.background = UITheme.Colors.BACKGROUND
        panel.border = EmptyBorder(10, 20, 10, 20)
        
        val gbc = GridBagConstraints()
        gbc.insets = Insets(10, 10, 10, 10)
        gbc.fill = GridBagConstraints.BOTH
        gbc.weightx = 1.0
        gbc.weighty = 1.0
        
        // 기본 테마들
        val allThemes = ThemeManager.getAllThemes()
        var row = 0
        var col = 0
        
        allThemes.forEach { theme ->
            gbc.gridx = col
            gbc.gridy = row
            
            val card = ThemeCard(theme, theme == selectedTheme)
            card.addActionListener {
                selectTheme(theme, card)
            }
            themeCards.add(card)
            panel.add(card, gbc)
            
            col++
            if (col >= 3) {
                col = 0
                row++
            }
        }
        
        // 빈 공간 채우기
        if (col > 0) {
            while (col < 3) {
                gbc.gridx = col
                gbc.gridy = row
                panel.add(Box.createGlue(), gbc)
                col++
            }
        }
        
        val scrollPane = JScrollPane(panel)
        scrollPane.border = null
        scrollPane.verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
        
        return scrollPane
    }
    
    private fun createButtonPanel(): JPanel {
        val panel = JPanel(FlowLayout(FlowLayout.RIGHT, 10, 0))
        panel.background = UITheme.Colors.SURFACE
        panel.border = EmptyBorder(10, 20, 20, 20)
        
        val previewButton = ModernButton("미리보기", ModernButton.ButtonStyle.SECONDARY)
        previewButton.addActionListener { showPreview() }
        
        val applyButton = ModernButton("적용", ModernButton.ButtonStyle.PRIMARY)
        applyButton.addActionListener { applyTheme() }
        
        val cancelButton = ModernButton("취소", ModernButton.ButtonStyle.GHOST)
        cancelButton.addActionListener { dispose() }
        
        panel.add(cancelButton)
        panel.add(previewButton)
        panel.add(applyButton)
        
        return panel
    }
    
    private fun selectTheme(theme: GameTheme, selectedCard: ThemeCard) {
        selectedTheme = theme
        themeCards.forEach { card ->
            card.setSelected(card == selectedCard)
        }
        // TODO: 사운드 효과 추가
        // AdvancedSoundEffects.playSound(AdvancedSoundEffects.Sound.MENU_SELECT)
    }
    
    private fun showPreview() {
        val previewDialog = ThemePreviewDialog(SwingUtilities.getWindowAncestor(this) as Frame, selectedTheme)
        previewDialog.isVisible = true
    }
    
    private fun applyTheme() {
        ThemeManager.setTheme(selectedTheme)
        // TODO: 사운드 효과 추가
        // AdvancedSoundEffects.playSound(AdvancedSoundEffects.Sound.MENU_CONFIRM)
        
        UnifiedDialog.showInfo(
            this,
            "테마 적용",
            "테마가 적용되었습니다: ${selectedTheme.name}"
        )
        dispose()
    }
    
    private fun showCustomThemeDialog() {
        val dialog = CustomThemeCreationDialog(SwingUtilities.getWindowAncestor(this) as Frame)
        dialog.isVisible = true
        
        // 다이얼로그가 닫힌 후 테마 목록 새로고침
        if (dialog.getCreatedTheme() != null) {
            dispose()
            // 새 다이얼로그를 열어 새로고침
            SwingUtilities.invokeLater {
                ThemeSelectionDialog(parent as Frame).isVisible = true
            }
        }
    }
}

/**
 * 테마 카드 컴포넌트
 */
class ThemeCard(
    private val theme: GameTheme,
    private var isSelected: Boolean = false
) : JButton() {
    
    init {
        layout = BorderLayout()
        preferredSize = Dimension(250, 200)
        maximumSize = Dimension(250, 200)
        cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        
        updateAppearance()
        createContent()
    }
    
    private fun createContent() {
        // 미리보기 패널
        val previewPanel = JPanel()
        previewPanel.layout = BorderLayout()
        previewPanel.isOpaque = false
        previewPanel.border = EmptyBorder(10, 10, 10, 10)
        
        // 미리보기 이미지
        val preview = createPreviewImage()
        val previewLabel = JLabel(ImageIcon(preview))
        previewLabel.horizontalAlignment = SwingConstants.CENTER
        previewPanel.add(previewLabel, BorderLayout.CENTER)
        
        add(previewPanel, BorderLayout.CENTER)
        
        // 정보 패널
        val infoPanel = JPanel()
        infoPanel.layout = BoxLayout(infoPanel, BoxLayout.Y_AXIS)
        infoPanel.isOpaque = false
        infoPanel.border = EmptyBorder(10, 15, 15, 15)
        
        val nameLabel = JLabel(theme.name)
        nameLabel.font = UITheme.Fonts.BUTTON
        nameLabel.foreground = if (isSelected) UITheme.Colors.PRIMARY else UITheme.Colors.GRAY_900
        nameLabel.alignmentX = Component.LEFT_ALIGNMENT
        
        val descLabel = JLabel("<html>${theme.description}</html>")
        descLabel.font = UITheme.Fonts.BODY_SMALL
        descLabel.foreground = UITheme.Colors.GRAY_600
        descLabel.alignmentX = Component.LEFT_ALIGNMENT
        
        infoPanel.add(nameLabel)
        infoPanel.add(Box.createVerticalStrut(5))
        infoPanel.add(descLabel)
        
        add(infoPanel, BorderLayout.SOUTH)
    }
    
    private fun createPreviewImage(): Image {
        val size = 120
        val image = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
        val g = image.createGraphics()
        
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        
        // 배경
        g.color = theme.colorScheme.boardBackground
        g.fillRect(0, 0, size, size)
        
        // 보드 선
        g.color = theme.colorScheme.boardLines
        val cellSize = size / 5
        for (i in 0..5) {
            g.drawLine(0, i * cellSize, size, i * cellSize)
            g.drawLine(i * cellSize, 0, i * cellSize, size)
        }
        
        // 샘플 돌들
        drawSampleStone(g, cellSize * 1, cellSize * 1, cellSize / 2 - 2, true)
        drawSampleStone(g, cellSize * 3, cellSize * 2, cellSize / 2 - 2, false)
        drawSampleStone(g, cellSize * 2, cellSize * 3, cellSize / 2 - 2, true)
        
        g.dispose()
        return image
    }
    
    private fun drawSampleStone(g: Graphics2D, x: Int, y: Int, radius: Int, isBlack: Boolean) {
        val color = if (isBlack) theme.colorScheme.blackStone else theme.colorScheme.whiteStone
        g.color = color
        g.fillOval(x - radius, y - radius, radius * 2, radius * 2)
        
        val outline = if (isBlack) theme.colorScheme.blackStoneOutline else theme.colorScheme.whiteStoneOutline
        g.color = outline
        g.drawOval(x - radius, y - radius, radius * 2, radius * 2)
    }
    
    override fun setSelected(selected: Boolean) {
        isSelected = selected
        updateAppearance()
    }
    
    private fun updateAppearance() {
        background = if (isSelected) UITheme.Colors.PRIMARY_LIGHT else UITheme.Colors.SURFACE
        border = if (isSelected) {
            LineBorder(UITheme.Colors.PRIMARY, 2)
        } else {
            LineBorder(UITheme.Colors.GRAY_300, 1)
        }
    }
    
    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        
        // 선택된 상태 표시
        if (isSelected) {
            val g2d = g as Graphics2D
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            
            // 체크 마크
            g2d.color = UITheme.Colors.PRIMARY
            g2d.fillOval(width - 30, 10, 20, 20)
            
            g2d.color = Color.WHITE
            g2d.stroke = BasicStroke(2f)
            g2d.drawLine(width - 25, 20, width - 20, 25)
            g2d.drawLine(width - 20, 25, width - 15, 15)
        }
    }
}

/**
 * 테마 미리보기 다이얼로그
 */
class ThemePreviewDialog(parent: Frame, private val theme: GameTheme) : JDialog(parent, "테마 미리보기: ${theme.name}", true) {
    
    init {
        layout = BorderLayout()
        preferredSize = Dimension(700, 700)
        
        val previewPanel = BoardPreviewPanel(theme)
        add(previewPanel, BorderLayout.CENTER)
        
        val closeButton = ModernButton("닫기", ModernButton.ButtonStyle.SECONDARY)
        closeButton.addActionListener { dispose() }
        
        val buttonPanel = JPanel(FlowLayout(FlowLayout.CENTER))
        buttonPanel.background = UITheme.Colors.SURFACE
        buttonPanel.border = EmptyBorder(10, 10, 10, 10)
        buttonPanel.add(closeButton)
        
        add(buttonPanel, BorderLayout.SOUTH)
        
        pack()
        setLocationRelativeTo(parent)
    }
}

/**
 * 보드 미리보기 패널
 */
class BoardPreviewPanel(private val theme: GameTheme) : JPanel() {
    
    private val cellSize = 40
    private val padding = 30
    private val renderer = BoardRenderer(cellSize, padding)
    
    // 샘플 돌 위치들
    private val sampleStones: List<Pair<Position, Player>> = listOf(
        Position(3, 3) to Player.BLACK,
        Position(3, 4) to Player.WHITE,
        Position(4, 3) to Player.WHITE,
        Position(4, 4) to Player.BLACK,
        Position(7, 7) to Player.BLACK,
        Position(7, 8) to Player.WHITE,
        Position(8, 7) to Player.WHITE,
        Position(8, 8) to Player.BLACK,
        Position(10, 10) to Player.BLACK,
        Position(10, 11) to Player.WHITE,
        Position(11, 10) to Player.WHITE
    )
    
    init {
        background = theme.colorScheme.background
        preferredSize = Dimension(
            15 * cellSize + 2 * padding,
            15 * cellSize + 2 * padding
        )
    }
    
    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        
        val g2d = g as Graphics2D
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        
        // 임시로 테마 설정
        val originalTheme = ThemeManager.getCurrentTheme()
        ThemeManager.setTheme(theme)
        
        // 보드 그리기
        renderer.drawBoard(g2d, width, height)
        
        // 샘플 돌들 그리기
        sampleStones.forEachIndexed { index, (position, player) ->
            val isLastMove = index == sampleStones.size - 1
            renderer.drawStone(g2d, position, player, isLastMove)
        }
        
        // 호버 효과 샘플
        renderer.drawHover(g2d, Position(6, 6))
        
        // 금수 표시 샘플
        renderer.drawForbidden(g2d, Position(5, 5))
        
        // 원래 테마로 복원
        ThemeManager.setTheme(originalTheme)
    }
}

/**
 * 커스텀 테마 생성 다이얼로그
 */
class CustomThemeCreationDialog(parent: Frame) : JDialog(parent, "커스텀 테마 만들기", true) {
    
    private var createdTheme: GameTheme? = null
    private lateinit var nameField: JTextField
    private lateinit var baseThemeCombo: JComboBox<GameTheme>
    private lateinit var boardStyleCombo: JComboBox<BoardStyle>
    private lateinit var stoneStyleCombo: JComboBox<StoneStyle>
    private val colorPickers = mutableMapOf<String, JButton>()
    
    init {
        layout = BorderLayout()
        preferredSize = Dimension(600, 700)
        
        add(createContentPanel(), BorderLayout.CENTER)
        add(createButtonPanel(), BorderLayout.SOUTH)
        
        pack()
        setLocationRelativeTo(parent)
    }
    
    private fun createContentPanel(): JScrollPane {
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
        panel.background = UITheme.Colors.BACKGROUND
        panel.border = EmptyBorder(20, 20, 20, 20)
        
        // 기본 정보
        panel.add(createBasicInfoPanel())
        panel.add(Box.createVerticalStrut(20))
        
        // 스타일 설정
        panel.add(createStylePanel())
        panel.add(Box.createVerticalStrut(20))
        
        // 색상 설정
        panel.add(createColorPanel())
        
        val scrollPane = JScrollPane(panel)
        scrollPane.border = null
        scrollPane.verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
        
        return scrollPane
    }
    
    private fun createBasicInfoPanel(): JPanel {
        val panel = JPanel(GridBagLayout())
        panel.background = UITheme.Colors.SURFACE
        panel.border = BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(UITheme.Colors.GRAY_300),
                "기본 정보",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                UITheme.Fonts.BUTTON,
                UITheme.Colors.GRAY_700
            ),
            EmptyBorder(10, 15, 10, 15)
        )
        
        val gbc = GridBagConstraints()
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.insets = Insets(5, 5, 5, 5)
        
        // 테마 이름
        gbc.gridx = 0
        gbc.gridy = 0
        gbc.weightx = 0.0
        panel.add(JLabel("테마 이름:"), gbc)
        
        nameField = JTextField()
        nameField.font = UITheme.Fonts.BODY
        gbc.gridx = 1
        gbc.weightx = 1.0
        panel.add(nameField, gbc)
        
        // 기본 테마
        gbc.gridx = 0
        gbc.gridy = 1
        gbc.weightx = 0.0
        panel.add(JLabel("기본 테마:"), gbc)
        
        baseThemeCombo = JComboBox(GameTheme.DEFAULT_THEMES.toTypedArray())
        baseThemeCombo.font = UITheme.Fonts.BODY
        baseThemeCombo.renderer = object : DefaultListCellRenderer() {
            override fun getListCellRendererComponent(
                list: JList<*>?, value: Any?, index: Int,
                isSelected: Boolean, cellHasFocus: Boolean
            ): Component {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
                if (value is GameTheme) {
                    text = value.name
                }
                return this
            }
        }
        baseThemeCombo.addActionListener { updateColorPickers() }
        gbc.gridx = 1
        gbc.weightx = 1.0
        panel.add(baseThemeCombo, gbc)
        
        return panel
    }
    
    private fun createStylePanel(): JPanel {
        val panel = JPanel(GridBagLayout())
        panel.background = UITheme.Colors.SURFACE
        panel.border = BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(UITheme.Colors.GRAY_300),
                "스타일 설정",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                UITheme.Fonts.BUTTON,
                UITheme.Colors.GRAY_700
            ),
            EmptyBorder(10, 15, 10, 15)
        )
        
        val gbc = GridBagConstraints()
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.insets = Insets(5, 5, 5, 5)
        
        // 보드 스타일
        gbc.gridx = 0
        gbc.gridy = 0
        gbc.weightx = 0.0
        panel.add(JLabel("보드 스타일:"), gbc)
        
        boardStyleCombo = JComboBox(BoardStyle.values())
        boardStyleCombo.font = UITheme.Fonts.BODY
        boardStyleCombo.renderer = object : DefaultListCellRenderer() {
            override fun getListCellRendererComponent(
                list: JList<*>?, value: Any?, index: Int,
                isSelected: Boolean, cellHasFocus: Boolean
            ): Component {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
                if (value is BoardStyle) {
                    text = value.displayName
                }
                return this
            }
        }
        gbc.gridx = 1
        gbc.weightx = 1.0
        panel.add(boardStyleCombo, gbc)
        
        // 돌 스타일
        gbc.gridx = 0
        gbc.gridy = 1
        gbc.weightx = 0.0
        panel.add(JLabel("돌 스타일:"), gbc)
        
        stoneStyleCombo = JComboBox(StoneStyle.values())
        stoneStyleCombo.font = UITheme.Fonts.BODY
        stoneStyleCombo.renderer = object : DefaultListCellRenderer() {
            override fun getListCellRendererComponent(
                list: JList<*>?, value: Any?, index: Int,
                isSelected: Boolean, cellHasFocus: Boolean
            ): Component {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
                if (value is StoneStyle) {
                    text = value.displayName
                }
                return this
            }
        }
        gbc.gridx = 1
        gbc.weightx = 1.0
        panel.add(stoneStyleCombo, gbc)
        
        return panel
    }
    
    private fun createColorPanel(): JPanel {
        val panel = JPanel(GridBagLayout())
        panel.background = UITheme.Colors.SURFACE
        panel.border = BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(UITheme.Colors.GRAY_300),
                "색상 설정",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                UITheme.Fonts.BUTTON,
                UITheme.Colors.GRAY_700
            ),
            EmptyBorder(10, 15, 10, 15)
        )
        
        val gbc = GridBagConstraints()
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.insets = Insets(5, 5, 5, 5)
        
        val colorSettings = listOf(
            "배경" to "background",
            "보드 배경" to "boardBackground",
            "보드 선" to "boardLines",
            "검은 돌" to "blackStone",
            "흰 돌" to "whiteStone",
            "주 색상" to "primary",
            "보조 색상" to "secondary",
            "텍스트" to "textPrimary"
        )
        
        colorSettings.forEachIndexed { index, (label, key) ->
            gbc.gridx = 0
            gbc.gridy = index
            gbc.weightx = 0.0
            panel.add(JLabel("$label:"), gbc)
            
            val colorButton = JButton()
            colorButton.preferredSize = Dimension(100, 25)
            colorButton.addActionListener { chooseColor(key, colorButton) }
            colorPickers[key] = colorButton
            
            gbc.gridx = 1
            gbc.weightx = 1.0
            panel.add(colorButton, gbc)
        }
        
        updateColorPickers()
        
        return panel
    }
    
    private fun createButtonPanel(): JPanel {
        val panel = JPanel(FlowLayout(FlowLayout.RIGHT, 10, 0))
        panel.background = UITheme.Colors.SURFACE
        panel.border = EmptyBorder(10, 20, 20, 20)
        
        val createButton = ModernButton("생성", ModernButton.ButtonStyle.PRIMARY)
        createButton.addActionListener { createTheme() }
        
        val cancelButton = ModernButton("취소", ModernButton.ButtonStyle.GHOST)
        cancelButton.addActionListener { dispose() }
        
        panel.add(cancelButton)
        panel.add(createButton)
        
        return panel
    }
    
    private fun updateColorPickers() {
        val baseTheme = baseThemeCombo.selectedItem as? GameTheme ?: return
        val colors = baseTheme.colorScheme
        
        colorPickers["background"]?.background = colors.background
        colorPickers["boardBackground"]?.background = colors.boardBackground
        colorPickers["boardLines"]?.background = colors.boardLines
        colorPickers["blackStone"]?.background = colors.blackStone
        colorPickers["whiteStone"]?.background = colors.whiteStone
        colorPickers["primary"]?.background = colors.primary
        colorPickers["secondary"]?.background = colors.secondary
        colorPickers["textPrimary"]?.background = colors.textPrimary
    }
    
    private fun chooseColor(key: String, button: JButton) {
        val color = JColorChooser.showDialog(
            this,
            "색상 선택",
            button.background
        )
        if (color != null) {
            button.background = color
        }
    }
    
    private fun createTheme() {
        val name = nameField.text.trim()
        if (name.isEmpty()) {
            UnifiedDialog.showError(
                this,
                "입력 오류",
                "테마 이름을 입력하세요."
            )
            return
        }
        
        val baseTheme = baseThemeCombo.selectedItem as GameTheme
        
        createdTheme = ThemeManager.createCustomTheme(name, baseTheme) {
            board(boardStyleCombo.selectedItem as BoardStyle)
            stones(stoneStyleCombo.selectedItem as StoneStyle)
            
            colors {
                colorPickers["background"]?.background?.let { background(it) }
                colorPickers["boardBackground"]?.background?.let { boardBackground(it) }
                colorPickers["boardLines"]?.background?.let { boardLines(it) }
                colorPickers["blackStone"]?.background?.let { blackStone(it) }
                colorPickers["whiteStone"]?.background?.let { whiteStone(it) }
                colorPickers["primary"]?.background?.let { primary(it) }
                colorPickers["secondary"]?.background?.let { secondary(it) }
                colorPickers["textPrimary"]?.background?.let { textPrimary(it) }
            }
        }
        
        if (ThemeManager.addCustomTheme(createdTheme!!)) {
            UnifiedDialog.showInfo(
                this,
                "테마 생성",
                "커스텀 테마가 생성되었습니다: $name"
            )
            dispose()
        } else {
            UnifiedDialog.showError(
                this,
                "오류",
                "테마 생성에 실패했습니다."
            )
        }
    }
    
    fun getCreatedTheme(): GameTheme? = createdTheme
}