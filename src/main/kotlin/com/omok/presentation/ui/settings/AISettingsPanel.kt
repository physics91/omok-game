package com.omok.presentation.ui.settings

import com.omok.presentation.ui.components.ModernButton
import com.omok.presentation.ui.theme.UITheme
import java.awt.*
import javax.swing.*
import javax.swing.border.EmptyBorder
import javax.swing.border.TitledBorder
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

private val AI_SETTINGS_FILE = File(System.getProperty("user.home"), ".omok/ai_settings.json")

/**
 * AI 설정 패널
 */
class AISettingsPanel : JPanel() {
    
    // AI 알고리즘 선택
    private val algorithmCombo = JComboBox<AIAlgorithm>()
    
    // AI 사고 시간 슬라이더
    private val thinkingTimeSlider = JSlider(JSlider.HORIZONTAL, 1, 10, 5)
    private val thinkingTimeLabel = JLabel("5초")
    
    // AI 강도 미세 조정
    private val strengthSlider = JSlider(JSlider.HORIZONTAL, 0, 100, 50)
    private val strengthLabel = JLabel("50%")
    
    // 패턴 인식 활성화
    private val patternRecognitionCheck = JCheckBox("패턴 인식 사용", true)
    
    // MCTS 설정
    private val mctsIterationsSpinner = JSpinner(SpinnerNumberModel(5000, 1000, 50000, 1000))
    
    // 개방 라이브러리 사용
    private val useOpeningBookCheck = JCheckBox("정석 데이터베이스 사용", true)
    
    // 학습 모드
    private val learningModeCheck = JCheckBox("학습 모드 (게임에서 배우기)", false)
    
    init {
        initializeUI()
        loadSettings()
    }
    
    private fun initializeUI() {
        layout = BorderLayout()
        background = UITheme.Colors.BACKGROUND
        
        val contentPanel = JPanel()
        contentPanel.layout = BoxLayout(contentPanel, BoxLayout.Y_AXIS)
        contentPanel.background = UITheme.Colors.BACKGROUND
        
        // AI 알고리즘 설정
        contentPanel.add(createAlgorithmPanel())
        contentPanel.add(Box.createVerticalStrut(UITheme.Spacing.MD))
        
        // AI 성능 설정
        contentPanel.add(createPerformancePanel())
        contentPanel.add(Box.createVerticalStrut(UITheme.Spacing.MD))
        
        // 고급 설정
        contentPanel.add(createAdvancedPanel())
        contentPanel.add(Box.createVerticalStrut(UITheme.Spacing.MD))
        
        // 실험적 기능
        contentPanel.add(createExperimentalPanel())
        
        val scrollPane = JScrollPane(contentPanel)
        scrollPane.border = null
        scrollPane.verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
        
        add(scrollPane, BorderLayout.CENTER)
        
        // 하단 버튼
        add(createButtonPanel(), BorderLayout.SOUTH)
    }
    
    private fun createAlgorithmPanel(): JPanel {
        val panel = JPanel(GridBagLayout())
        panel.background = UITheme.Colors.SURFACE
        panel.border = BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(UITheme.Colors.GRAY_300),
                "AI 알고리즘",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                UITheme.Fonts.BUTTON,
                UITheme.Colors.GRAY_700
            ),
            EmptyBorder(UITheme.Spacing.SM, UITheme.Spacing.MD, UITheme.Spacing.SM, UITheme.Spacing.MD)
        )
        
        val gbc = GridBagConstraints()
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.insets = Insets(UITheme.Spacing.XS, UITheme.Spacing.SM, UITheme.Spacing.XS, UITheme.Spacing.SM)
        
        // 알고리즘 선택
        gbc.gridx = 0
        gbc.gridy = 0
        gbc.weightx = 0.0
        panel.add(JLabel("알고리즘:"), gbc)
        
        algorithmCombo.model = DefaultComboBoxModel(AIAlgorithm.values())
        algorithmCombo.font = UITheme.Fonts.BODY
        algorithmCombo.renderer = object : DefaultListCellRenderer() {
            override fun getListCellRendererComponent(
                list: JList<*>?, value: Any?, index: Int, isSelected: Boolean, cellHasFocus: Boolean
            ): Component {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
                if (value is AIAlgorithm) {
                    text = value.displayName
                    toolTipText = value.description
                }
                return this
            }
        }
        algorithmCombo.addActionListener { updateAlgorithmSettings() }
        
        gbc.gridx = 1
        gbc.weightx = 1.0
        panel.add(algorithmCombo, gbc)
        
        // 알고리즘 설명
        val descriptionArea = JTextArea()
        descriptionArea.isEditable = false
        descriptionArea.lineWrap = true
        descriptionArea.wrapStyleWord = true
        descriptionArea.font = UITheme.Fonts.BODY_SMALL
        descriptionArea.foreground = UITheme.Colors.GRAY_600
        descriptionArea.background = UITheme.Colors.SURFACE
        descriptionArea.border = EmptyBorder(UITheme.Spacing.XS, 0, 0, 0)
        
        gbc.gridx = 0
        gbc.gridy = 1
        gbc.gridwidth = 2
        panel.add(descriptionArea, gbc)
        
        algorithmCombo.addActionListener {
            val selected = algorithmCombo.selectedItem as? AIAlgorithm
            descriptionArea.text = selected?.description ?: ""
        }
        
        return panel
    }
    
    private fun createPerformancePanel(): JPanel {
        val panel = JPanel(GridBagLayout())
        panel.background = UITheme.Colors.SURFACE
        panel.border = BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(UITheme.Colors.GRAY_300),
                "성능 설정",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                UITheme.Fonts.BUTTON,
                UITheme.Colors.GRAY_700
            ),
            EmptyBorder(UITheme.Spacing.SM, UITheme.Spacing.MD, UITheme.Spacing.SM, UITheme.Spacing.MD)
        )
        
        val gbc = GridBagConstraints()
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.insets = Insets(UITheme.Spacing.XS, UITheme.Spacing.SM, UITheme.Spacing.XS, UITheme.Spacing.SM)
        
        // 사고 시간
        gbc.gridx = 0
        gbc.gridy = 0
        gbc.weightx = 0.0
        panel.add(JLabel("사고 시간:"), gbc)
        
        thinkingTimeSlider.majorTickSpacing = 2
        thinkingTimeSlider.minorTickSpacing = 1
        thinkingTimeSlider.paintTicks = true
        thinkingTimeSlider.snapToTicks = true
        thinkingTimeSlider.addChangeListener {
            thinkingTimeLabel.text = "${thinkingTimeSlider.value}초"
        }
        
        gbc.gridx = 1
        gbc.weightx = 1.0
        panel.add(thinkingTimeSlider, gbc)
        
        gbc.gridx = 2
        gbc.weightx = 0.0
        panel.add(thinkingTimeLabel, gbc)
        
        // AI 강도
        gbc.gridx = 0
        gbc.gridy = 1
        panel.add(JLabel("AI 강도:"), gbc)
        
        strengthSlider.majorTickSpacing = 25
        strengthSlider.minorTickSpacing = 5
        strengthSlider.paintTicks = true
        strengthSlider.addChangeListener {
            strengthLabel.text = "${strengthSlider.value}%"
        }
        
        gbc.gridx = 1
        gbc.weightx = 1.0
        panel.add(strengthSlider, gbc)
        
        gbc.gridx = 2
        gbc.weightx = 0.0
        panel.add(strengthLabel, gbc)
        
        return panel
    }
    
    private fun createAdvancedPanel(): JPanel {
        val panel = JPanel(GridBagLayout())
        panel.background = UITheme.Colors.SURFACE
        panel.border = BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(UITheme.Colors.GRAY_300),
                "고급 설정",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                UITheme.Fonts.BUTTON,
                UITheme.Colors.GRAY_700
            ),
            EmptyBorder(UITheme.Spacing.SM, UITheme.Spacing.MD, UITheme.Spacing.SM, UITheme.Spacing.MD)
        )
        
        val gbc = GridBagConstraints()
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.insets = Insets(UITheme.Spacing.XS, UITheme.Spacing.SM, UITheme.Spacing.XS, UITheme.Spacing.SM)
        
        // 패턴 인식
        gbc.gridx = 0
        gbc.gridy = 0
        gbc.gridwidth = 2
        patternRecognitionCheck.font = UITheme.Fonts.BODY
        panel.add(patternRecognitionCheck, gbc)
        
        // MCTS 반복 횟수
        gbc.gridx = 0
        gbc.gridy = 1
        gbc.gridwidth = 1
        gbc.weightx = 0.0
        panel.add(JLabel("MCTS 반복:"), gbc)
        
        mctsIterationsSpinner.font = UITheme.Fonts.BODY
        gbc.gridx = 1
        gbc.weightx = 1.0
        panel.add(mctsIterationsSpinner, gbc)
        
        // 개방 라이브러리
        gbc.gridx = 0
        gbc.gridy = 2
        gbc.gridwidth = 2
        useOpeningBookCheck.font = UITheme.Fonts.BODY
        panel.add(useOpeningBookCheck, gbc)
        
        return panel
    }
    
    private fun createExperimentalPanel(): JPanel {
        val panel = JPanel(GridBagLayout())
        panel.background = UITheme.Colors.SURFACE
        panel.border = BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(UITheme.Colors.GRAY_300),
                "실험적 기능",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                UITheme.Fonts.BUTTON,
                UITheme.Colors.GRAY_700
            ),
            EmptyBorder(UITheme.Spacing.SM, UITheme.Spacing.MD, UITheme.Spacing.SM, UITheme.Spacing.MD)
        )
        
        val gbc = GridBagConstraints()
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.insets = Insets(UITheme.Spacing.XS, UITheme.Spacing.SM, UITheme.Spacing.XS, UITheme.Spacing.SM)
        
        // 학습 모드
        gbc.gridx = 0
        gbc.gridy = 0
        learningModeCheck.font = UITheme.Fonts.BODY
        learningModeCheck.toolTipText = "AI가 플레이한 게임에서 학습하여 점진적으로 향상됩니다"
        panel.add(learningModeCheck, gbc)
        
        // 경고 메시지
        val warningLabel = JLabel("⚠ 실험적 기능은 불안정할 수 있습니다")
        warningLabel.font = UITheme.Fonts.BODY_SMALL
        warningLabel.foreground = UITheme.Colors.WARNING
        gbc.gridy = 1
        panel.add(warningLabel, gbc)
        
        return panel
    }
    
    private fun createButtonPanel(): JPanel {
        val panel = JPanel(FlowLayout(FlowLayout.RIGHT, UITheme.Spacing.SM, 0))
        panel.background = UITheme.Colors.BACKGROUND
        panel.border = EmptyBorder(UITheme.Spacing.SM, UITheme.Spacing.MD, UITheme.Spacing.SM, UITheme.Spacing.MD)
        
        val resetButton = ModernButton("기본값 복원", ModernButton.ButtonStyle.SECONDARY)
        resetButton.addActionListener { resetToDefaults() }
        
        val applyButton = ModernButton("적용", ModernButton.ButtonStyle.PRIMARY)
        applyButton.addActionListener { applySettings() }
        
        panel.add(resetButton)
        panel.add(applyButton)
        
        return panel
    }
    
    private fun updateAlgorithmSettings() {
        val selected = algorithmCombo.selectedItem as? AIAlgorithm ?: return
        
        // 알고리즘에 따라 관련 설정 활성화/비활성화
        when (selected) {
            AIAlgorithm.MINIMAX -> {
                mctsIterationsSpinner.isEnabled = false
                patternRecognitionCheck.isEnabled = true
            }
            AIAlgorithm.MCTS -> {
                mctsIterationsSpinner.isEnabled = true
                patternRecognitionCheck.isEnabled = false
            }
            AIAlgorithm.ENHANCED -> {
                mctsIterationsSpinner.isEnabled = true
                patternRecognitionCheck.isEnabled = true
            }
        }
    }
    
    private fun loadSettings() {
        val settings = AISettingsRepository.load()
        algorithmCombo.selectedItem = settings.algorithm
        thinkingTimeSlider.value = settings.thinkingTimeSeconds
        strengthSlider.value = settings.strengthPercentage
        patternRecognitionCheck.isSelected = settings.usePatternRecognition
        mctsIterationsSpinner.value = settings.mctsIterations
        useOpeningBookCheck.isSelected = settings.useOpeningBook
        learningModeCheck.isSelected = settings.learningMode

        updateAlgorithmSettings()
    }
    
    private fun resetToDefaults() {
        val defaultSettings = AISettings()
        algorithmCombo.selectedItem = defaultSettings.algorithm
        thinkingTimeSlider.value = defaultSettings.thinkingTimeSeconds
        strengthSlider.value = defaultSettings.strengthPercentage
        patternRecognitionCheck.isSelected = defaultSettings.usePatternRecognition
        mctsIterationsSpinner.value = defaultSettings.mctsIterations
        useOpeningBookCheck.isSelected = defaultSettings.useOpeningBook
        learningModeCheck.isSelected = defaultSettings.learningMode

        updateAlgorithmSettings()
        
        JOptionPane.showMessageDialog(
            this,
            "설정이 기본값으로 복원되었습니다.",
            "설정 복원",
            JOptionPane.INFORMATION_MESSAGE
        )
    }
    
    private fun applySettings() {
        val settings = AISettings(
            algorithm = algorithmCombo.selectedItem as AIAlgorithm,
            thinkingTimeSeconds = thinkingTimeSlider.value,
            strengthPercentage = strengthSlider.value,
            usePatternRecognition = patternRecognitionCheck.isSelected,
            mctsIterations = mctsIterationsSpinner.value as Int,
            useOpeningBook = useOpeningBookCheck.isSelected,
            learningMode = learningModeCheck.isSelected
        )
        
        AISettingsRepository.save(settings)
        applyAISettings(settings)
        
        JOptionPane.showMessageDialog(
            this,
            "AI 설정이 적용되었습니다.",
            "설정 적용",
            JOptionPane.INFORMATION_MESSAGE
        )
    }
    
    private fun applyAISettings(settings: AISettings) {
        // 실제 AI 설정 적용 로직
        // DependencyContainer를 통해 AI 설정 업데이트
    }
}

private object AISettingsRepository {
    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    fun save(settings: AISettings) {
        try {
            AI_SETTINGS_FILE.parentFile.mkdirs()
            AI_SETTINGS_FILE.writeText(json.encodeToString(settings))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun load(): AISettings {
        return try {
            if (AI_SETTINGS_FILE.exists()) {
                json.decodeFromString<AISettings>(AI_SETTINGS_FILE.readText())
            } else {
                AISettings()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            AISettings()
        }
    }
}

@Serializable
enum class AIAlgorithm(val displayName: String, val description: String) {
    MINIMAX(
        "Minimax (기본)",
        "전통적인 미니맥스 알고리즘입니다. 안정적이고 예측 가능한 플레이를 합니다."
    ),
    MCTS(
        "MCTS (몬테카를로)",
        "몬테카를로 트리 탐색을 사용합니다. 더 창의적이고 다양한 수를 둡니다."
    ),
    ENHANCED(
        "Enhanced (향상된 AI)",
        "패턴 인식과 MCTS를 결합한 향상된 AI입니다. 가장 강력한 성능을 보입니다."
    )
}

@Serializable
data class AISettings(
    val algorithm: AIAlgorithm = AIAlgorithm.ENHANCED,
    val thinkingTimeSeconds: Int = 5,
    val strengthPercentage: Int = 50,
    val usePatternRecognition: Boolean = true,
    val mctsIterations: Int = 5000,
    val useOpeningBook: Boolean = true,
    val learningMode: Boolean = false
)