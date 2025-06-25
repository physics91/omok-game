package com.omok.presentation

import com.omok.infrastructure.DependencyContainer
import com.omok.presentation.controller.GameController
import com.omok.presentation.ui.GameWindow
import com.omok.presentation.ui.theme.UITheme
import com.omok.presentation.ui.components.SplashScreen
import javax.swing.SwingUtilities
import javax.swing.UIManager
import javax.swing.Timer

fun main() {
    // 한글 깨짐 문제 해결을 위한 인코딩 설정
    System.setProperty("file.encoding", "UTF-8")
    System.setProperty("sun.jnu.encoding", "UTF-8")
    System.setProperty("sun.java2d.uiScale", "1.0")  // UI 스케일링 방지
    
    SwingUtilities.invokeLater {
        // Apply modern theme
        UITheme.applyTheme()
        
        // Show splash screen
        val splash = SplashScreen()
        splash.showSplash()
        
        // Initialize in background
        Timer(1500) {
            val dependencyContainer = DependencyContainer()
            val gameApplicationService = dependencyContainer.createGameApplicationService()
            
            val gameWindow = GameWindow()
            val gameController = GameController(gameApplicationService, gameWindow, dependencyContainer)
            
            // 이벤트 핸들러 등록
            dependencyContainer.eventBus.subscribe(gameController)
            
            gameWindow.setController(gameController)
            
            // Show window after splash
            Timer(1000) {
                gameWindow.isVisible = true
            }.apply {
                isRepeats = false
                start()
            }
            
            // 종료 시 정리
            Runtime.getRuntime().addShutdownHook(Thread {
                gameController.cleanup()
            })
        }.apply {
            isRepeats = false
            start()
        }
    }
}