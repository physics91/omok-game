package com.omok.infrastructure.logging

import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineExceptionHandler

/**
 * 중앙 예외 처리 시스템
 * - 모든 catch되지 않은 예외를 자동으로 로깅
 * - 코루틴 예외도 처리
 * - 스택 트레이스와 함께 상세 로깅
 */
object ExceptionHandler {
    
    /**
     * 전역 예외 핸들러 설정
     * - 메인 스레드와 모든 스레드에서 발생하는 예외 처리
     */
    fun setupGlobalExceptionHandler() {
        // 메인 스레드의 예외 처리
        Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
            handleUncaughtException(thread.name, exception)
        }
        
        Logger.info("ExceptionHandler", "Global exception handler initialized")
    }
    
    /**
     * 코루틴 예외 핸들러
     * - 코루틴에서 발생하는 예외 처리
     */
    val coroutineExceptionHandler = CoroutineExceptionHandler { context, exception ->
        val contextInfo = context.toString()
        handleUncaughtException("Coroutine-$contextInfo", exception)
    }
    
    /**
     * 예외를 수동으로 로깅하는 함수
     * - try-catch 블록에서 예외를 잡았을 때 사용
     */
    fun logException(tag: String, message: String, exception: Throwable) {
        // HeadlessException은 WSL/서버 환경에서 정상적인 상황
        if (exception is java.awt.HeadlessException) {
            Logger.debug(tag, "GUI operation attempted in headless environment: $message")
            return
        }
        
        val detailedMessage = buildExceptionMessage(message, exception)
        Logger.error(tag, detailedMessage)
    }
    
    /**
     * 예외를 수동으로 로깅하는 함수 (태그 자동 추출)
     */
    fun logException(caller: Any, message: String, exception: Throwable) {
        val tag = caller::class.simpleName ?: "Unknown"
        logException(tag, message, exception)
    }
    
    /**
     * 잡히지 않은 예외 처리
     */
    private fun handleUncaughtException(threadName: String, exception: Throwable) {
        // HeadlessException은 WSL/서버 환경에서 정상적인 상황이므로 DEBUG 레벨로 로깅
        if (exception is java.awt.HeadlessException) {
            val message = "GUI not available in headless environment (thread: $threadName)"
            Logger.debug("UncaughtException", message)
            return
        }
        
        val message = "Uncaught exception in thread: $threadName"
        val detailedMessage = buildExceptionMessage(message, exception)
        
        Logger.error("UncaughtException", detailedMessage)
        
        // 중요한 예외의 경우 추가 처리
        when (exception) {
            is OutOfMemoryError -> {
                Logger.error("UncaughtException", "Critical: Out of memory error detected")
                // 메모리 상태 로깅
                logMemoryStatus()
            }
            is StackOverflowError -> {
                Logger.error("UncaughtException", "Critical: Stack overflow error detected")
            }
            is SecurityException -> {
                Logger.error("UncaughtException", "Security exception: ${exception.message}")
            }
        }
    }
    
    /**
     * 예외 메시지를 상세하게 구성
     */
    private fun buildExceptionMessage(baseMessage: String, exception: Throwable): String {
        return buildString {
            appendLine(baseMessage)
            appendLine("Exception Type: ${exception::class.simpleName}")
            
            // 메시지가 있는 경우에만 추가
            exception.message?.let { msg ->
                if (msg.isNotBlank()) {
                    appendLine("Exception Message: $msg")
                }
            }
            
            // 원인 예외가 있는 경우
            var cause = exception.cause
            var level = 1
            while (cause != null && level <= 3) { // 최대 3레벨로 줄임
                appendLine("Caused by: ${cause::class.simpleName} - ${cause.message ?: "No message"}")
                cause = cause.cause
                level++
            }
            
            // 스택 트레이스의 첫 몇 줄 (프로젝트 관련 코드만)
            val stackTrace = exception.stackTrace.filter { 
                it.className.contains("com.omok") 
            }
            
            if (stackTrace.isNotEmpty()) {
                appendLine("Relevant stack trace:")
                stackTrace.take(3).forEach { element ->
                    appendLine("  at $element")
                }
            }
        }
    }
    
    /**
     * 메모리 상태 로깅
     */
    private fun logMemoryStatus() {
        try {
            val runtime = Runtime.getRuntime()
            val maxMemory = runtime.maxMemory()
            val totalMemory = runtime.totalMemory()
            val freeMemory = runtime.freeMemory()
            val usedMemory = totalMemory - freeMemory
            
            val message = buildString {
                appendLine("Memory Status:")
                appendLine("  Max Memory: ${formatBytes(maxMemory)}")
                appendLine("  Total Memory: ${formatBytes(totalMemory)}")
                appendLine("  Used Memory: ${formatBytes(usedMemory)}")
                appendLine("  Free Memory: ${formatBytes(freeMemory)}")
                appendLine("  Memory Usage: ${String.format("%.1f", (usedMemory.toDouble() / maxMemory * 100))}%")
            }
            
            Logger.error("MemoryStatus", message)
        } catch (e: Exception) {
            Logger.error("MemoryStatus", "Failed to get memory status: ${e.message}")
        }
    }
    
    /**
     * 바이트를 사람이 읽기 쉬운 형태로 변환
     */
    private fun formatBytes(bytes: Long): String {
        val units = arrayOf("B", "KB", "MB", "GB")
        var size = bytes.toDouble()
        var unitIndex = 0
        
        while (size >= 1024 && unitIndex < units.size - 1) {
            size /= 1024
            unitIndex++
        }
        
        return String.format("%.1f %s", size, units[unitIndex])
    }
}

/**
 * 편의를 위한 확장 함수들
 */

/**
 * try-catch 블록을 간편하게 사용할 수 있는 확장 함수
 */
inline fun <T> safeCall(
    tag: String = "SafeCall",
    onError: (Throwable) -> T? = { null },
    action: () -> T
): T? {
    return try {
        action()
    } catch (e: Exception) {
        ExceptionHandler.logException(tag, "Exception in safe call", e)
        onError(e)
    }
}

/**
 * 객체 메서드에서 안전하게 호출할 수 있는 확장 함수
 */
inline fun <T> Any.safeCall(
    onError: (Throwable) -> T? = { null },
    action: () -> T
): T? {
    return try {
        action()
    } catch (e: Exception) {
        ExceptionHandler.logException(this, "Exception in ${this::class.simpleName}", e)
        onError(e)
    }
}

/**
 * 코루틴에서 안전하게 실행할 수 있는 함수
 */
suspend inline fun <T> safeCallSuspend(
    tag: String = "SafeCallSuspend",
    noinline onError: suspend (Throwable) -> T? = { null },
    action: suspend () -> T
): T? {
    return try {
        action()
    } catch (e: Exception) {
        ExceptionHandler.logException(tag, "Exception in suspend call", e)
        onError(e)
    }
}