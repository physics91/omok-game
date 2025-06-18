package com.omok

import javax.swing.SwingUtilities

fun main() {
    SwingUtilities.invokeLater {
        try {
            for (info in javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus" == info.name) {
                    javax.swing.UIManager.setLookAndFeel(info.className)
                    break
                }
            }
        } catch (e: Exception) {
            // 기본 Look and Feel 사용
        }
        
        val gameView = GameView()
        gameView.isVisible = true
    }
}