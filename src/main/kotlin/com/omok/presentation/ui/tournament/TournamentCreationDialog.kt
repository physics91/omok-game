package com.omok.presentation.ui.tournament

import com.omok.domain.tournament.*
import com.omok.domain.model.*
import com.omok.infrastructure.tournament.TournamentManager
import com.omok.presentation.ui.components.ModernButton
import com.omok.presentation.ui.theme.UITheme
import java.awt.*
import javax.swing.*
import javax.swing.border.EmptyBorder
import javax.swing.border.TitledBorder
import javax.swing.table.DefaultTableModel

/**
 * 토너먼트 생성 다이얼로그
 */
class TournamentCreationDialog(
    parent: Frame
) : JDialog(parent, "토너먼트 생성", true) {
    
    private lateinit var nameField: JTextField
    private lateinit var formatCombo: JComboBox<TournamentFormat>
    private lateinit var participantTable: JTable
    private lateinit var participantModel: DefaultTableModel
    private lateinit var gameModeCombo: JComboBox<String>
    private lateinit var timeLimitCombo: JComboBox<String>
    private lateinit var ruleCombo: JComboBox<GameRule>
    
    private var createdTournament: Tournament? = null
    
    init {
        initializeUI()
    }
    
    private fun initializeUI() {
        layout = BorderLayout()
        preferredSize = Dimension(700, 600)
        
        val contentPanel = JPanel(BorderLayout())
        contentPanel.background = UITheme.Colors.BACKGROUND
        contentPanel.border = EmptyBorder(UITheme.Spacing.MD, UITheme.Spacing.MD, UITheme.Spacing.MD, UITheme.Spacing.MD)
        
        // 상단: 기본 정보
        contentPanel.add(createBasicInfoPanel(), BorderLayout.NORTH)
        
        // 중앙: 참가자 관리
        contentPanel.add(createParticipantPanel(), BorderLayout.CENTER)
        
        // 하단: 게임 설정
        contentPanel.add(createGameSettingsPanel(), BorderLayout.SOUTH)
        
        add(contentPanel, BorderLayout.CENTER)
        
        // 버튼 패널
        add(createButtonPanel(), BorderLayout.SOUTH)
        
        pack()
        setLocationRelativeTo(parent)
    }
    
    private fun createBasicInfoPanel(): JPanel {
        val panel = JPanel(GridBagLayout())
        panel.background = UITheme.Colors.SURFACE
        panel.border = BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(UITheme.Colors.GRAY_300),
                "토너먼트 정보",
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
        
        // 토너먼트 이름
        gbc.gridx = 0
        gbc.gridy = 0
        gbc.gridwidth = 1
        gbc.weightx = 0.0
        panel.add(JLabel("토너먼트 이름:"), gbc)
        
        nameField = JTextField()
        nameField.font = UITheme.Fonts.BODY
        gbc.gridx = 1
        gbc.weightx = 1.0
        panel.add(nameField, gbc)
        
        // 토너먼트 형식
        gbc.gridx = 0
        gbc.gridy = 1
        gbc.weightx = 0.0
        panel.add(JLabel("토너먼트 형식:"), gbc)
        
        formatCombo = JComboBox(TournamentFormat.values())
        formatCombo.font = UITheme.Fonts.BODY
        formatCombo.renderer = object : DefaultListCellRenderer() {
            override fun getListCellRendererComponent(
                list: JList<*>?, value: Any?, index: Int, isSelected: Boolean, cellHasFocus: Boolean
            ): Component {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
                if (value is TournamentFormat) {
                    text = value.displayName
                    toolTipText = value.description
                }
                return this
            }
        }
        formatCombo.addActionListener { updatePreview() }
        gbc.gridx = 1
        gbc.weightx = 1.0
        panel.add(formatCombo, gbc)
        
        return panel
    }
    
    private fun createParticipantPanel(): JPanel {
        val panel = JPanel(BorderLayout())
        panel.background = UITheme.Colors.SURFACE
        panel.border = BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(UITheme.Colors.GRAY_300),
                "참가자 관리",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                UITheme.Fonts.BUTTON,
                UITheme.Colors.GRAY_700
            ),
            EmptyBorder(UITheme.Spacing.SM, UITheme.Spacing.MD, UITheme.Spacing.SM, UITheme.Spacing.MD)
        )
        
        // 참가자 테이블
        val columnNames = arrayOf("이름", "레이팅", "시드")
        participantModel = object : DefaultTableModel(columnNames, 0) {
            override fun isCellEditable(row: Int, column: Int) = column == 0 // 이름만 편집 가능
            override fun getColumnClass(columnIndex: Int): Class<*> {
                return when (columnIndex) {
                    1, 2 -> Integer::class.java
                    else -> String::class.java
                }
            }
        }
        
        participantTable = JTable(participantModel)
        participantTable.font = UITheme.Fonts.BODY
        participantTable.rowHeight = 30
        participantTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
        
        val scrollPane = JScrollPane(participantTable)
        scrollPane.preferredSize = Dimension(0, 200)
        panel.add(scrollPane, BorderLayout.CENTER)
        
        // 버튼 패널
        val buttonPanel = JPanel(FlowLayout(FlowLayout.LEFT, UITheme.Spacing.SM, 0))
        buttonPanel.background = UITheme.Colors.SURFACE
        
        val addButton = ModernButton("참가자 추가", ModernButton.ButtonStyle.SECONDARY)
        addButton.addActionListener { addParticipant() }
        
        val removeButton = ModernButton("선택 삭제", ModernButton.ButtonStyle.SECONDARY)
        removeButton.addActionListener { removeSelectedParticipant() }
        
        val clearButton = ModernButton("모두 삭제", ModernButton.ButtonStyle.SECONDARY)
        clearButton.addActionListener { clearAllParticipants() }
        
        val autoSeedButton = ModernButton("자동 시드", ModernButton.ButtonStyle.SECONDARY)
        autoSeedButton.addActionListener { autoAssignSeeds() }
        
        buttonPanel.add(addButton)
        buttonPanel.add(removeButton)
        buttonPanel.add(clearButton)
        buttonPanel.add(autoSeedButton)
        
        panel.add(buttonPanel, BorderLayout.SOUTH)
        
        // 기본 참가자 추가
        addDefaultParticipants()
        
        return panel
    }
    
    private fun createGameSettingsPanel(): JPanel {
        val panel = JPanel(GridBagLayout())
        panel.background = UITheme.Colors.SURFACE
        panel.border = BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(UITheme.Colors.GRAY_300),
                "게임 설정",
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
        
        // 게임 모드
        gbc.gridx = 0
        gbc.gridy = 0
        gbc.weightx = 0.0
        panel.add(JLabel("게임 모드:"), gbc)
        
        gameModeCombo = JComboBox(arrayOf("사람 vs 사람"))
        gameModeCombo.font = UITheme.Fonts.BODY
        gbc.gridx = 1
        gbc.weightx = 1.0
        panel.add(gameModeCombo, gbc)
        
        // 시간 제한
        gbc.gridx = 0
        gbc.gridy = 1
        gbc.weightx = 0.0
        panel.add(JLabel("시간 제한:"), gbc)
        
        timeLimitCombo = JComboBox(arrayOf("없음", "10분", "20분", "30분"))
        timeLimitCombo.font = UITheme.Fonts.BODY
        gbc.gridx = 1
        gbc.weightx = 1.0
        panel.add(timeLimitCombo, gbc)
        
        // 게임 규칙
        gbc.gridx = 0
        gbc.gridy = 2
        gbc.weightx = 0.0
        panel.add(JLabel("게임 규칙:"), gbc)
        
        ruleCombo = JComboBox(GameRule.values())
        ruleCombo.font = UITheme.Fonts.BODY
        ruleCombo.renderer = object : DefaultListCellRenderer() {
            override fun getListCellRendererComponent(
                list: JList<*>?, value: Any?, index: Int, isSelected: Boolean, cellHasFocus: Boolean
            ): Component {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
                if (value is GameRule) {
                    text = value.displayName
                }
                return this
            }
        }
        gbc.gridx = 1
        gbc.weightx = 1.0
        panel.add(ruleCombo, gbc)
        
        return panel
    }
    
    private fun createButtonPanel(): JPanel {
        val panel = JPanel(FlowLayout(FlowLayout.RIGHT, UITheme.Spacing.SM, 0))
        panel.background = UITheme.Colors.BACKGROUND
        panel.border = EmptyBorder(0, UITheme.Spacing.MD, UITheme.Spacing.SM, UITheme.Spacing.MD)
        
        val createButton = ModernButton("토너먼트 생성", ModernButton.ButtonStyle.PRIMARY)
        createButton.addActionListener { createTournament() }
        
        val cancelButton = ModernButton("취소", ModernButton.ButtonStyle.SECONDARY)
        cancelButton.addActionListener { dispose() }
        
        panel.add(cancelButton)
        panel.add(createButton)
        
        return panel
    }
    
    private fun addDefaultParticipants() {
        val defaultNames = listOf("Player 1", "Player 2", "Player 3", "Player 4")
        defaultNames.forEach { name ->
            participantModel.addRow(arrayOf(name, 1500, participantModel.rowCount + 1))
        }
    }
    
    private fun addParticipant() {
        val name = JOptionPane.showInputDialog(
            this,
            "참가자 이름을 입력하세요:",
            "참가자 추가",
            JOptionPane.PLAIN_MESSAGE
        )
        
        if (!name.isNullOrBlank()) {
            participantModel.addRow(arrayOf(name, 1500, participantModel.rowCount + 1))
        }
    }
    
    private fun removeSelectedParticipant() {
        val selectedRow = participantTable.selectedRow
        if (selectedRow >= 0) {
            participantModel.removeRow(selectedRow)
            updateSeeds()
        }
    }
    
    private fun clearAllParticipants() {
        val result = JOptionPane.showConfirmDialog(
            this,
            "모든 참가자를 삭제하시겠습니까?",
            "확인",
            JOptionPane.YES_NO_OPTION
        )
        
        if (result == JOptionPane.YES_OPTION) {
            participantModel.rowCount = 0
        }
    }
    
    private fun autoAssignSeeds() {
        // 레이팅 기준으로 시드 자동 배정
        val participants = mutableListOf<Triple<String, Int, Int>>()
        
        for (i in 0 until participantModel.rowCount) {
            val name = participantModel.getValueAt(i, 0) as String
            val rating = participantModel.getValueAt(i, 1) as Int
            participants.add(Triple(name, rating, i))
        }
        
        // 레이팅 높은 순으로 정렬
        participants.sortByDescending { it.second }
        
        // 시드 번호 업데이트
        participants.forEachIndexed { index, (_, _, row) ->
            participantModel.setValueAt(index + 1, row, 2)
        }
    }
    
    private fun updateSeeds() {
        for (i in 0 until participantModel.rowCount) {
            participantModel.setValueAt(i + 1, i, 2)
        }
    }
    
    private fun updatePreview() {
        // 선택된 형식에 따라 예상 라운드 수 등을 표시할 수 있음
    }
    
    private fun createTournament() {
        // 입력 검증
        val name = nameField.text.trim()
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(
                this,
                "토너먼트 이름을 입력하세요.",
                "입력 오류",
                JOptionPane.ERROR_MESSAGE
            )
            return
        }
        
        if (participantModel.rowCount < 2) {
            JOptionPane.showMessageDialog(
                this,
                "최소 2명 이상의 참가자가 필요합니다.",
                "입력 오류",
                JOptionPane.ERROR_MESSAGE
            )
            return
        }
        
        // 참가자 목록 생성
        val participants = mutableListOf<TournamentParticipant>()
        for (i in 0 until participantModel.rowCount) {
            val participantName = participantModel.getValueAt(i, 0) as String
            val rating = participantModel.getValueAt(i, 1) as Int
            val seed = participantModel.getValueAt(i, 2) as Int
            
            participants.add(
                TournamentParticipant(
                    name = participantName,
                    rating = rating,
                    seed = seed
                )
            )
        }
        
        // 게임 설정
        val timeLimit = when (timeLimitCombo.selectedItem) {
            "10분" -> TimeLimit.RAPID_10_MINUTES
            "20분" -> TimeLimit(1200L, 0L, TimeLimitMode.TOTAL_TIME)
            "30분" -> TimeLimit.CLASSICAL_30_MINUTES
            else -> TimeLimit.NONE
        }
        
        val gameSettings = GameSettings(
            mode = GameMode.PLAYER_VS_PLAYER,
            gameRule = ruleCombo.selectedItem as GameRule,
            timeLimit = timeLimit
        )
        
        val tournamentSettings = TournamentSettings(
            gameSettings = gameSettings,
            timeControlEnabled = timeLimit != TimeLimit.NONE
        )
        
        // 토너먼트 생성
        val tournamentManager = TournamentManager.getInstance()
        createdTournament = tournamentManager.createTournament(
            name = name,
            format = formatCombo.selectedItem as TournamentFormat,
            participants = participants,
            settings = tournamentSettings
        )
        
        JOptionPane.showMessageDialog(
            this,
            "토너먼트가 생성되었습니다: $name",
            "토너먼트 생성",
            JOptionPane.INFORMATION_MESSAGE
        )
        
        dispose()
    }
    
    fun getCreatedTournament(): Tournament? = createdTournament
}