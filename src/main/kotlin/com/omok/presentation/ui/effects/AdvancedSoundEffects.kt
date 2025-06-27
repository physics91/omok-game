package com.omok.presentation.ui.effects

import com.omok.infrastructure.logging.Logger
import com.omok.presentation.ui.settings.UIGameSettings
import javax.sound.sampled.*
import java.io.BufferedInputStream
import java.net.URL
import kotlin.concurrent.thread

/**
 * 고급 사운드 효과 시스템
 */
object AdvancedSoundEffects {
    
    enum class Sound(val resourcePath: String, val volume: Float = 0.8f) {
        STONE_PLACE("stone_place.wav", 0.7f),
        STONE_CAPTURE("stone_capture.wav", 0.8f),
        GAME_START("game_start.wav", 0.6f),
        GAME_WIN("game_win.wav", 0.9f),
        GAME_LOSE("game_lose.wav", 0.7f),
        GAME_DRAW("game_draw.wav", 0.7f),
        MOVE_HOVER("move_hover.wav", 0.3f),
        MOVE_INVALID("move_invalid.wav", 0.8f),
        TIME_WARNING("time_warning.wav", 0.9f),
        TIME_UP("time_up.wav", 1.0f),
        BUTTON_CLICK("button_click.wav", 0.5f),
        MENU_OPEN("menu_open.wav", 0.4f),
        UNDO_MOVE("undo_move.wav", 0.6f),
        CORRECT_ANSWER("correct_answer.wav", 0.8f),
        GOOD_MOVE("good_move.wav", 0.7f)
    }
    
    private val audioCache = mutableMapOf<Sound, Clip?>()
    private var masterVolume = 1.0f
    
    init {
        // 사운드 파일들을 미리 로드
        preloadSounds()
    }
    
    private fun preloadSounds() {
        thread {
            Sound.values().forEach { sound ->
                try {
                    val clip = loadSound(sound)
                    audioCache[sound] = clip
                } catch (e: Exception) {
                    Logger.warn("AdvancedSoundEffects", "Failed to preload sound: ${sound.name}", e)
                    audioCache[sound] = null
                }
            }
            Logger.info("AdvancedSoundEffects", "Sound preloading complete")
        }
    }
    
    private fun loadSound(sound: Sound): Clip? {
        return try {
            // 리소스에서 사운드 파일 로드 시도
            val resourceUrl = javaClass.getResource("/sounds/${sound.resourcePath}")
            if (resourceUrl != null) {
                loadSoundFromUrl(resourceUrl)
            } else {
                // 리소스가 없으면 합성음 생성
                createSyntheticSound(sound)
            }
        } catch (e: Exception) {
            Logger.error("AdvancedSoundEffects", "Error loading sound: ${sound.name}", e)
            null
        }
    }
    
    private fun loadSoundFromUrl(url: URL): Clip {
        val audioInputStream = AudioSystem.getAudioInputStream(BufferedInputStream(url.openStream()))
        val clip = AudioSystem.getClip()
        clip.open(audioInputStream)
        return clip
    }
    
    private fun createSyntheticSound(sound: Sound): Clip? {
        // 실제 사운드 파일이 없을 때 간단한 합성음 생성
        return try {
            val format = AudioFormat(44100f, 16, 1, true, false)
            val info = DataLine.Info(Clip::class.java, format)
            val clip = AudioSystem.getLine(info) as Clip
            
            // 사운드 타입에 따른 간단한 톤 생성
            val duration = when (sound) {
                Sound.STONE_PLACE -> 0.1f
                Sound.GAME_WIN -> 0.5f
                Sound.MOVE_INVALID -> 0.2f
                Sound.TIME_WARNING -> 0.3f
                else -> 0.15f
            }
            
            val frequency = when (sound) {
                Sound.STONE_PLACE -> 600f
                Sound.GAME_WIN -> 800f
                Sound.MOVE_INVALID -> 300f
                Sound.TIME_WARNING -> 1000f
                Sound.BUTTON_CLICK -> 500f
                else -> 440f
            }
            
            val sampleRate = format.sampleRate.toInt()
            val numSamples = (duration * sampleRate).toInt()
            val samples = ByteArray(numSamples * 2)
            
            for (i in 0 until numSamples) {
                val angle = 2.0 * Math.PI * i / (sampleRate / frequency)
                val sample = (Math.sin(angle) * 32767.0).toInt().toShort()
                
                // 페이드 인/아웃 효과
                val fadeLength = numSamples / 10
                val amplitude = when {
                    i < fadeLength -> i.toFloat() / fadeLength
                    i > numSamples - fadeLength -> (numSamples - i).toFloat() / fadeLength
                    else -> 1.0f
                }
                
                val fadedSample = (sample * amplitude).toInt().toShort()
                samples[i * 2] = (fadedSample.toInt() and 0xff).toByte()
                samples[i * 2 + 1] = (fadedSample.toInt() shr 8 and 0xff).toByte()
            }
            
            clip.open(format, samples, 0, samples.size)
            clip
        } catch (e: Exception) {
            Logger.error("AdvancedSoundEffects", "Failed to create synthetic sound", e)
            null
        }
    }
    
    fun play(sound: Sound, volumeMultiplier: Float = 1.0f) {
        if (!UIGameSettings.getInstance().soundEnabled) return
        
        thread {
            try {
                val clip = audioCache[sound]
                if (clip != null) {
                    // 볼륨 설정
                    setVolume(clip, sound.volume * masterVolume * volumeMultiplier)
                    
                    // 처음부터 재생
                    clip.framePosition = 0
                    clip.start()
                } else {
                    // 캐시에 없으면 기본 비프음
                    java.awt.Toolkit.getDefaultToolkit().beep()
                }
            } catch (e: Exception) {
                Logger.error("AdvancedSoundEffects", "Error playing sound: ${sound.name}", e)
            }
        }
    }
    
    private fun setVolume(clip: Clip, volume: Float) {
        try {
            val gainControl = clip.getControl(FloatControl.Type.MASTER_GAIN) as? FloatControl
            if (gainControl != null) {
                val dB = 20f * Math.log10(volume.toDouble()).toFloat()
                val clampedDB = dB.coerceIn(gainControl.minimum, gainControl.maximum)
                gainControl.value = clampedDB
            }
        } catch (e: Exception) {
            // 볼륨 컨트롤이 지원되지 않는 경우 무시
        }
    }
    
    fun setMasterVolume(volume: Float) {
        masterVolume = volume.coerceIn(0f, 1f)
    }
    
    fun getMasterVolume(): Float = masterVolume
    
    fun setSoundEnabled(enabled: Boolean) {
        val settings = UIGameSettings.getInstance()
        val newSettings = settings.copy(soundEnabled = enabled)
        UIGameSettings.updateSettings(newSettings)
        
        if (enabled) {
            // 사운드를 켤 때 환영음 재생
            play(Sound.MENU_OPEN)
        }
    }
    
    fun isSoundEnabled(): Boolean = UIGameSettings.getInstance().soundEnabled
    
    fun cleanup() {
        audioCache.values.forEach { clip ->
            try {
                clip?.stop()
                clip?.close()
            } catch (e: Exception) {
                // 무시
            }
        }
        audioCache.clear()
    }
}