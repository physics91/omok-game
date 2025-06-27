package com.omok.application.ui

/**
 * 다이얼로그 서비스 인터페이스
 * 애플리케이션 계층에서 UI 추상화를 제공
 */
interface DialogService {
    
    enum class DialogResult {
        CONFIRMED,
        CANCELLED,
        YES,
        NO
    }
    
    /**
     * 정보 다이얼로그 표시
     */
    fun showInfo(title: String, message: String): DialogResult
    
    /**
     * 확인 다이얼로그 표시
     */
    fun showConfirm(title: String, message: String, confirmText: String = "확인", cancelText: String = "취소"): DialogResult
    
    /**
     * 에러 다이얼로그 표시
     */
    fun showError(title: String = "오류", message: String): DialogResult
    
    /**
     * 선택 다이얼로그 표시
     */
    fun showSelection(title: String, message: String, options: Array<String>, defaultOption: String? = null): Pair<DialogResult, String?>
    
    /**
     * 입력 다이얼로그 표시
     */
    fun showInput(title: String, message: String, defaultValue: String = ""): Pair<DialogResult, String>
}