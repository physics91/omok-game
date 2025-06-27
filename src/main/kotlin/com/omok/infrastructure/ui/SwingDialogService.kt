package com.omok.infrastructure.ui

import com.omok.application.ui.DialogService
import com.omok.presentation.ui.components.UnifiedDialog
import com.omok.presentation.ui.components.DialogResult as UIDialogResult
import java.awt.Window

/**
 * Swing 기반 다이얼로그 서비스 구현체
 */
class SwingDialogService(private val parentWindow: Window? = null) : DialogService {
    
    override fun showInfo(title: String, message: String): DialogService.DialogResult {
        val result = UnifiedDialog.showInfo(parentWindow, title, message)
        return mapDialogResult(result)
    }
    
    override fun showConfirm(title: String, message: String, confirmText: String, cancelText: String): DialogService.DialogResult {
        val result = UnifiedDialog.showConfirm(parentWindow, title, message, confirmText = confirmText, cancelText = cancelText)
        return mapDialogResult(result)
    }
    
    override fun showError(title: String, message: String): DialogService.DialogResult {
        val result = UnifiedDialog.showError(parentWindow, title, message)
        return mapDialogResult(result)
    }
    
    override fun showSelection(title: String, message: String, options: Array<String>, defaultOption: String?): Pair<DialogService.DialogResult, String?> {
        val (result, selected) = UnifiedDialog.showSelection(parentWindow, title, message, options, defaultOption)
        return mapDialogResult(result) to selected
    }
    
    override fun showInput(title: String, message: String, defaultValue: String): Pair<DialogService.DialogResult, String> {
        val (result, input) = UnifiedDialog.showInput(parentWindow, title, message, defaultValue)
        return mapDialogResult(result) to input
    }
    
    private fun mapDialogResult(uiResult: UIDialogResult): DialogService.DialogResult {
        return when (uiResult) {
            UIDialogResult.CONFIRMED -> DialogService.DialogResult.CONFIRMED
            UIDialogResult.CANCELLED -> DialogService.DialogResult.CANCELLED
            UIDialogResult.YES -> DialogService.DialogResult.YES
            UIDialogResult.NO -> DialogService.DialogResult.NO
            UIDialogResult.CUSTOM -> DialogService.DialogResult.CONFIRMED
        }
    }
}