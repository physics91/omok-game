package com.omok.infrastructure.logging

import com.omok.domain.logging.DomainLogger

/**
 * 인프라 계층의 DomainLogger 구현체
 * 기존 Logger를 래핑하여 도메인 인터페이스 구현
 */
class DomainLoggerImpl(
    private val logger: Logger,
    private val defaultTag: String = "Domain"
) : DomainLogger {
    
    override fun debug(message: String) {
        logger.debug(defaultTag, message)
    }
    
    override fun info(message: String) {
        logger.info(defaultTag, message)
    }
    
    override fun warn(message: String, throwable: Throwable?) {
        logger.warn(defaultTag, message, throwable)
    }
    
    override fun error(message: String, throwable: Throwable?) {
        logger.error(defaultTag, message, throwable)
    }
}