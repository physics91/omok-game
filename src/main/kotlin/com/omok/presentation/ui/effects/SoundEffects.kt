package com.omok.presentation.ui.effects

import java.awt.Toolkit
import javax.sound.sampled.*
import kotlin.concurrent.thread

object SoundEffects {
    private var soundEnabled = true
    
    fun playStonePlace() {
        if (soundEnabled) {
            // Simple beep for now - in production, load actual sound files
            Toolkit.getDefaultToolkit().beep()
        }
    }
    
    fun playWin() {
        if (soundEnabled) {
            thread {
                repeat(3) {
                    Toolkit.getDefaultToolkit().beep()
                    Thread.sleep(200)
                }
            }
        }
    }
    
    fun playError() {
        if (soundEnabled) {
            Toolkit.getDefaultToolkit().beep()
        }
    }
    
    fun playHover() {
        // Subtle sound for hover - implement with actual sound file
    }
    
    fun setSoundEnabled(enabled: Boolean) {
        soundEnabled = enabled
    }
    
    fun isSoundEnabled() = soundEnabled
}