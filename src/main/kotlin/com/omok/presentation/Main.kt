package com.omok.presentation

import com.omok.infrastructure.DependencyContainer
import com.omok.presentation.controller.GameController
import com.omok.presentation.ui.GameWindow
import com.omok.presentation.ui.theme.UITheme
import com.omok.presentation.ui.components.SplashScreen
import com.omok.infrastructure.logging.Logger
import com.omok.infrastructure.logging.ExceptionHandler
import javax.swing.SwingUtilities
import javax.swing.UIManager
import javax.swing.Timer

fun main() {
    try {
        // 로그 레벨 설정 (개발 시에는 DEBUG, 배포 시에는 INFO)
        Logger.setLogLevel(Logger.Level.INFO)
        
        // 전역 예외 핸들러 설정
        ExceptionHandler.setupGlobalExceptionHandler()
        
        // 한글 깨짐 문제 해결을 위한 인코딩 설정
        System.setProperty("file.encoding", "UTF-8")
        System.setProperty("sun.jnu.encoding", "UTF-8")
        System.setProperty("sun.java2d.uiScale", "1.0")  // UI 스케일링 방지
        
        Logger.info("Application", "Starting Omok Game Application")
        
        SwingUtilities.invokeLater {
            try {
                // Apply modern theme
                UITheme.applyTheme()
                Logger.info("Application", "UI Theme applied")
                
                // Show splash screen
                val splash = SplashScreen()
                splash.showSplash()
                Logger.info("Application", "Splash screen shown")
                
                // Initialize in background
                Timer(1500) {
                    try {
                        Logger.info("Application", "Initializing game components")
                        val dependencyContainer = DependencyContainer()
                        val gameApplicationService = dependencyContainer.createGameApplicationService()
                        
                        val gameWindow = GameWindow()
                        val gameController = GameController(gameApplicationService, gameWindow, dependencyContainer)
                        
                        // 이벤트 핸들러 등록
                        dependencyContainer.eventBus.subscribe(gameController)
                        
                        gameWindow.setController(gameController)
                        Logger.info("Application", "Game components initialized successfully")
                        
                        // Show window after splash
                        Timer(1000) {
                            gameWindow.isVisible = true
                            Logger.info("Application", "Main window displayed")
                        }.apply {
                            isRepeats = false
                            start()
                        }
                        
                        // 종료 시 정리
                        Runtime.getRuntime().addShutdownHook(Thread {
                            Logger.info("Application", "Shutting down application")
                            gameController.cleanup()
                        })
                    } catch (e: Exception) {
                        Logger.error("Application", "Failed to initialize game", e)
                        // HeadlessException의 경우 GUI 대화상자 대신 콘솔에만 출력
                        if (e !is java.awt.HeadlessException) {
                            try {
                                javax.swing.JOptionPane.showMessageDialog(
                                    null,
                                    "게임 초기화 중 오류가 발생했습니다: ${e.message}",
                                    "오류",
                                    javax.swing.JOptionPane.ERROR_MESSAGE
                                )
                            } catch (headlessException: java.awt.HeadlessException) {
                                Logger.info("Application", "Cannot show GUI error dialog in headless environment")
                            }
                        }
                    }
                }.apply {
                    isRepeats = false
                    start()
                }
            } catch (e: Exception) {
                Logger.error("Application", "Failed to start application", e)
                // HeadlessException의 경우 GUI 대화상자 대신 콘솔에만 출력
                if (e !is java.awt.HeadlessException) {
                    try {
                        javax.swing.JOptionPane.showMessageDialog(
                            null,
                            "애플리케이션 시작 중 오류가 발생했습니다: ${e.message}",
                            "오류",
                            javax.swing.JOptionPane.ERROR_MESSAGE
                        )
                    } catch (headlessException: java.awt.HeadlessException) {
                        Logger.info("Application", "Cannot show GUI error dialog in headless environment")
                    }
                }
            }
        }
    } catch (e: Exception) {
        System.err.println("Critical error starting application: ${e.message}")
        e.printStackTrace()
    }
}