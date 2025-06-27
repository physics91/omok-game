package com.omok.presentation.ui.effects

import java.awt.Toolkit
import javax.sound.sampled.*
import kotlin.concurrent.thread

object SoundEffects {
    
    fun playStonePlace() {
        AdvancedSoundEffects.play(AdvancedSoundEffects.Sound.STONE_PLACE)
    }
    
    fun playWin() {
        AdvancedSoundEffects.play(AdvancedSoundEffects.Sound.GAME_WIN)
    }
    
    fun playError() {
        AdvancedSoundEffects.play(AdvancedSoundEffects.Sound.MOVE_INVALID)
    }
    
    fun playHover() {
        AdvancedSoundEffects.play(AdvancedSoundEffects.Sound.MOVE_HOVER)
    }
    
    fun playButtonClick() {
        AdvancedSoundEffects.play(AdvancedSoundEffects.Sound.BUTTON_CLICK)
    }
    
    fun playUndo() {
        AdvancedSoundEffects.play(AdvancedSoundEffects.Sound.UNDO_MOVE)
    }
    
    fun playTimeWarning() {
        AdvancedSoundEffects.play(AdvancedSoundEffects.Sound.TIME_WARNING)
    }
    
    fun playTimeUp() {
        AdvancedSoundEffects.play(AdvancedSoundEffects.Sound.TIME_UP)
    }
    
    fun playGameStart() {
        AdvancedSoundEffects.play(AdvancedSoundEffects.Sound.GAME_START)
    }
    
    fun playCorrect() {
        AdvancedSoundEffects.play(AdvancedSoundEffects.Sound.CORRECT_ANSWER)
    }
    
    fun playGood() {
        AdvancedSoundEffects.play(AdvancedSoundEffects.Sound.GOOD_MOVE)
    }
    
    fun setSoundEnabled(enabled: Boolean) {
        AdvancedSoundEffects.setSoundEnabled(enabled)
    }
    
    fun isSoundEnabled() = AdvancedSoundEffects.isSoundEnabled()
}