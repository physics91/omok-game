package com.omok.presentation.ui.components

import com.omok.presentation.ui.GameWindow
import com.omok.presentation.ui.theme.UITheme
import com.omok.presentation.ui.icons.IconLoader
import java.awt.event.KeyEvent
import javax.swing.*

/**
 * 간소화된 메뉴바
 * 핵심 기능만 포함하여 UI 복잡도 감소
 */
class SimplifiedMenuBar(private val gameWindow: GameWindow) : JMenuBar() {
    
    private lateinit var undoItem: JMenuItem
    
    init {
        background = UITheme.Colors.SURFACE
        border = BorderFactory.createMatteBorder(0, 0, 1, 0, UITheme.Colors.GRAY_200)
        
        setupMenus()
    }
    
    fun setUndoEnabled(enabled: Boolean) {
        if (::undoItem.isInitialized) {
            undoItem.isEnabled = enabled
        }
    }
    
    private fun setupMenus() {
        add(createGameMenu())
        add(createToolsMenu())
        add(createHelpMenu())
    }
    
    private fun createGameMenu(): JMenu {
        val menu = JMenu("게임")
        menu.mnemonic = KeyEvent.VK_G
        
        // 새 게임
        val newGameItem = JMenuItem("새 게임")
        newGameItem.icon = IconLoader.getIcon(IconLoader.Icon.LOGO, 16, 16)
        newGameItem.accelerator = KeyStroke.getKeyStroke("ctrl N")
        newGameItem.addActionListener { 
            val result = UnifiedDialog.showConfirm(
                gameWindow,
                "새 게임",
                "현재 게임을 종료하고 새 게임을 시작하시겠습니까?"
            )
            if (result == DialogResult.CONFIRMED) {
                gameWindow.showNewGameDialog()
            }
        }
        
        // 무르기
        undoItem = JMenuItem("무르기")
        undoItem.icon = IconLoader.getIcon(IconLoader.Icon.UNDO, 16, 16)
        undoItem.accelerator = KeyStroke.getKeyStroke("ctrl Z")
        undoItem.isEnabled = false // 초기에는 비활성화
        undoItem.addActionListener { gameWindow.controller.undoMove() }
        
        menu.add(newGameItem)
        menu.add(undoItem)
        menu.addSeparator()
        
        // 저장/불러오기
        val saveItem = JMenuItem("저장")
        saveItem.icon = IconLoader.getIcon(IconLoader.Icon.SAVE, 16, 16)
        saveItem.accelerator = KeyStroke.getKeyStroke("ctrl S")
        saveItem.addActionListener { gameWindow.saveGame() }
        
        val loadItem = JMenuItem("불러오기")
        loadItem.icon = IconLoader.getIcon(IconLoader.Icon.LOAD, 16, 16)
        loadItem.accelerator = KeyStroke.getKeyStroke("ctrl O")
        loadItem.addActionListener { gameWindow.loadGame() }
        
        menu.add(saveItem)
        menu.add(loadItem)
        menu.addSeparator()
        
        // 종료
        val exitItem = JMenuItem("종료")
        exitItem.icon = IconLoader.getIcon(IconLoader.Icon.SETTINGS, 16, 16)
        exitItem.accelerator = KeyStroke.getKeyStroke("alt F4")
        exitItem.addActionListener { 
            val result = UnifiedDialog.showConfirm(
                gameWindow,
                "게임 종료",
                "게임을 종료하시겠습니까?"
            )
            if (result == DialogResult.CONFIRMED) {
                System.exit(0)
            }
        }
        
        menu.add(exitItem)
        
        return menu
    }
    
    private fun createToolsMenu(): JMenu {
        val menu = JMenu("도구")
        menu.mnemonic = KeyEvent.VK_T
        
        // 설정
        val settingsItem = JMenuItem("설정")
        settingsItem.icon = IconLoader.getIcon(IconLoader.Icon.SETTINGS, 16, 16)
        settingsItem.accelerator = KeyStroke.getKeyStroke("ctrl comma")
        settingsItem.addActionListener { gameWindow.showSettingsDialog() }
        
        // 테마 선택
        val themeItem = JMenuItem("테마")
        themeItem.icon = IconLoader.getIcon(IconLoader.Icon.SETTINGS, 16, 16)
        themeItem.accelerator = KeyStroke.getKeyStroke("ctrl T")
        themeItem.addActionListener { gameWindow.showThemeSelectionDialog() }
        
        menu.add(settingsItem)
        menu.add(themeItem)
        menu.addSeparator()
        
        // 고급 기능들 (서브메뉴)
        val advancedMenu = JMenu("고급 기능")
        
        val analyzeItem = JMenuItem("게임 분석")
        analyzeItem.icon = IconLoader.getIcon(IconLoader.Icon.PLAYER_VS_AI, 16, 16)
        analyzeItem.addActionListener { gameWindow.analyzeGame() }
        
        val puzzleItem = JMenuItem("퍼즐 모드")
        puzzleItem.icon = IconLoader.getIcon(IconLoader.Icon.HELP, 16, 16)
        puzzleItem.addActionListener { gameWindow.showPuzzleDialog() }
        
        val tournamentItem = JMenuItem("토너먼트")
        tournamentItem.icon = IconLoader.getIcon(IconLoader.Icon.PLAYER_VS_PLAYER, 16, 16)
        tournamentItem.addActionListener { gameWindow.showTournamentDialog() }
        
        val replayItem = JMenuItem("기보 재생")
        replayItem.icon = IconLoader.getIcon(IconLoader.Icon.PLAYER_VS_PLAYER, 16, 16)
        replayItem.addActionListener { gameWindow.toggleReplayMode() }
        
        val exportItem = JMenuItem("SGF 내보내기")
        exportItem.icon = IconLoader.getIcon(IconLoader.Icon.SAVE, 16, 16)
        exportItem.addActionListener { gameWindow.exportToSGF() }
        
        advancedMenu.add(analyzeItem)
        advancedMenu.add(puzzleItem)
        advancedMenu.add(tournamentItem)
        advancedMenu.addSeparator()
        advancedMenu.add(replayItem)
        advancedMenu.add(exportItem)
        
        menu.add(advancedMenu)
        
        return menu
    }
    
    private fun createHelpMenu(): JMenu {
        val menu = JMenu("도움말")
        menu.mnemonic = KeyEvent.VK_H
        
        // 성취도
        val achievementsItem = JMenuItem("성취도")
        achievementsItem.icon = IconLoader.getIcon(IconLoader.Icon.LOGO, 16, 16)
        achievementsItem.accelerator = KeyStroke.getKeyStroke("F3")
        achievementsItem.addActionListener { gameWindow.showAchievementDialog() }
        
        // 통계
        val statisticsItem = JMenuItem("통계")
        statisticsItem.icon = IconLoader.getIcon(IconLoader.Icon.PLAYER_VS_PLAYER, 16, 16)
        statisticsItem.accelerator = KeyStroke.getKeyStroke("F2")
        statisticsItem.addActionListener { gameWindow.showStatisticsDialog() }
        
        menu.add(achievementsItem)
        menu.add(statisticsItem)
        menu.addSeparator()
        
        // 렌주룰 설명
        val rulesItem = JMenuItem("렌주룰 설명")
        rulesItem.icon = IconLoader.getIcon(IconLoader.Icon.HELP, 16, 16)
        rulesItem.addActionListener { gameWindow.showRulesDialog() }
        
        // 정보
        val aboutItem = JMenuItem("정보")
        aboutItem.icon = IconLoader.getIcon(IconLoader.Icon.LOGO, 16, 16)
        aboutItem.addActionListener { gameWindow.showAboutDialog() }
        
        menu.add(rulesItem)
        menu.add(aboutItem)
        
        return menu
    }
}