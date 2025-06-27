package com.omok.domain.logging

/**
 * 도메인 계층을 위한 로깅 인터페이스
 * 의존성 역전 원칙에 따라 도메인에서 정의하고 인프라에서 구현
 */
interface DomainLogger {
    fun debug(message: String)
    fun info(message: String)
    fun warn(message: String, throwable: Throwable? = null)
    fun error(message: String, throwable: Throwable? = null)
}

/**
 * 기본 NoOp 구현체 (테스트용)
 */
object NoOpLogger : DomainLogger {
    override fun debug(message: String) {}
    override fun info(message: String) {}
    override fun warn(message: String, throwable: Throwable?) {}
    override fun error(message: String, throwable: Throwable?) {}
}