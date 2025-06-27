package com.omok.infrastructure.logging

import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.concurrent.thread

/**
 * 게임 로깅 시스템
 * - 파일과 콘솔에 동시 로깅
 * - 비동기 처리로 성능 영향 최소화
 * - 레벨별 로그 관리
 */
object Logger {
    
    enum class Level(val priority: Int, val displayName: String) {
        DEBUG(0, "DEBUG"),
        INFO(1, "INFO"),
        WARN(2, "WARN"),
        ERROR(3, "ERROR");
        
        fun shouldLog(threshold: Level): Boolean = priority >= threshold.priority
    }
    
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
    private val logQueue = ConcurrentLinkedQueue<LogEntry>()
    private var isRunning = true
    private var logLevel = Level.INFO
    private val logFile = File("logs/omok-game.log")
    
    init {
        // 로그 디렉토리 생성
        logFile.parentFile?.mkdirs()
        
        // 로그 파일 초기화 (앱 시작시)
        if (logFile.exists()) {
            // 기존 로그 파일을 백업
            val backupFile = File("logs/omok-game-${System.currentTimeMillis()}.log")
            logFile.renameTo(backupFile)
        }
        
        // 비동기 로그 처리 스레드 시작
        startLoggerThread()
        
        // JVM 종료시 정리
        Runtime.getRuntime().addShutdownHook(thread(start = false) {
            shutdown()
        })
        
        info("Logger", "Logging system initialized")
    }
    
    private fun startLoggerThread() {
        thread(isDaemon = true, name = "Logger-Thread") {
            while (isRunning) {
                try {
                    val entry = logQueue.poll()
                    if (entry != null) {
                        writeToFile(entry)
                        writeToConsole(entry)
                    } else {
                        Thread.sleep(10) // 큐가 비어있으면 잠시 대기
                    }
                } catch (e: Exception) {
                    System.err.println("Logger error: ${e.message}")
                    e.printStackTrace()
                }
            }
            
            // 종료 시 남은 로그 처리
            while (logQueue.isNotEmpty()) {
                logQueue.poll()?.let { entry ->
                    writeToFile(entry)
                    writeToConsole(entry)
                }
            }
        }
    }
    
    private fun writeToFile(entry: LogEntry) {
        try {
            FileWriter(logFile, true).use { writer ->
                PrintWriter(writer).use { printWriter ->
                    printWriter.println(entry.toFileFormat())
                    entry.throwable?.printStackTrace(printWriter)
                }
            }
        } catch (e: Exception) {
            System.err.println("Failed to write to log file: ${e.message}")
        }
    }
    
    private fun writeToConsole(entry: LogEntry) {
        val output = entry.toConsoleFormat()
        when (entry.level) {
            Level.ERROR -> System.err.println(output)
            Level.WARN -> System.err.println(output)
            else -> println(output)
        }
        
        entry.throwable?.printStackTrace()
    }
    
    fun setLogLevel(level: Level) {
        logLevel = level
        info("Logger", "Log level set to $level")
    }
    
    fun debug(tag: String, message: String, throwable: Throwable? = null) {
        log(Level.DEBUG, tag, message, throwable)
    }
    
    fun info(tag: String, message: String, throwable: Throwable? = null) {
        log(Level.INFO, tag, message, throwable)
    }
    
    fun warn(tag: String, message: String, throwable: Throwable? = null) {
        log(Level.WARN, tag, message, throwable)
    }
    
    fun error(tag: String, message: String, throwable: Throwable? = null) {
        log(Level.ERROR, tag, message, throwable)
    }
    
    private fun log(level: Level, tag: String, message: String, throwable: Throwable?) {
        if (!level.shouldLog(logLevel)) return
        
        val entry = LogEntry(
            timestamp = LocalDateTime.now(),
            level = level,
            tag = tag,
            message = message,
            throwable = throwable,
            thread = Thread.currentThread().name
        )
        
        logQueue.offer(entry)
    }
    
    fun shutdown() {
        info("Logger", "Shutting down logger...")
        isRunning = false
        
        // 잠시 대기하여 남은 로그들이 처리되도록 함
        Thread.sleep(100)
    }
    
    private data class LogEntry(
        val timestamp: LocalDateTime,
        val level: Level,
        val tag: String,
        val message: String,
        val throwable: Throwable?,
        val thread: String
    ) {
        fun toFileFormat(): String {
            return "${timestamp.format(dateFormatter)} [${level.displayName}] [$thread] [$tag] $message"
        }
        
        fun toConsoleFormat(): String {
            val colorCode = when (level) {
                Level.DEBUG -> "\u001B[36m" // Cyan
                Level.INFO -> "\u001B[32m"  // Green
                Level.WARN -> "\u001B[33m"  // Yellow
                Level.ERROR -> "\u001B[31m" // Red
            }
            val resetCode = "\u001B[0m"
            
            return "$colorCode[${level.displayName}] [$tag] $message$resetCode"
        }
    }
}

/**
 * 편의를 위한 확장 함수들
 */
fun Any.logDebug(message: String, throwable: Throwable? = null) {
    Logger.debug(this::class.simpleName ?: "Unknown", message, throwable)
}

fun Any.logInfo(message: String, throwable: Throwable? = null) {
    Logger.info(this::class.simpleName ?: "Unknown", message, throwable)
}

fun Any.logWarn(message: String, throwable: Throwable? = null) {
    Logger.warn(this::class.simpleName ?: "Unknown", message, throwable)
}

fun Any.logError(message: String, throwable: Throwable? = null) {
    Logger.error(this::class.simpleName ?: "Unknown", message, throwable)
}