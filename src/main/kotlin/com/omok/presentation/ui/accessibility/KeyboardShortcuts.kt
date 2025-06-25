package com.omok.presentation.ui.accessibility

import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import javax.swing.*

object KeyboardShortcuts {
    fun setupShortcuts(rootPane: JRootPane, actions: ShortcutActions) {
        val inputMap = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
        val actionMap = rootPane.actionMap
        
        // New Game - Ctrl+N
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_N, KeyEvent.CTRL_DOWN_MASK), "newGame")
        actionMap.put("newGame", object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent) {
                actions.onNewGame()
            }
        })
        
        // Undo - Ctrl+Z
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, KeyEvent.CTRL_DOWN_MASK), "undo")
        actionMap.put("undo", object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent) {
                actions.onUndo()
            }
        })
        
        // Help - F1
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0), "help")
        actionMap.put("help", object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent) {
                actions.onHelp()
            }
        })
        
        // Settings - Ctrl+,
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_COMMA, KeyEvent.CTRL_DOWN_MASK), "settings")
        actionMap.put("settings", object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent) {
                actions.onSettings()
            }
        })
        
        // Quit - Ctrl+Q
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Q, KeyEvent.CTRL_DOWN_MASK), "quit")
        actionMap.put("quit", object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent) {
                actions.onQuit()
            }
        })
        
        // Arrow keys for board navigation
        setupArrowKeys(inputMap, actionMap, actions)
    }
    
    private fun setupArrowKeys(inputMap: InputMap, actionMap: ActionMap, actions: ShortcutActions) {
        // Up arrow
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), "moveUp")
        actionMap.put("moveUp", object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent) {
                actions.onNavigate(Direction.UP)
            }
        })
        
        // Down arrow
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "moveDown")
        actionMap.put("moveDown", object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent) {
                actions.onNavigate(Direction.DOWN)
            }
        })
        
        // Left arrow
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "moveLeft")
        actionMap.put("moveLeft", object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent) {
                actions.onNavigate(Direction.LEFT)
            }
        })
        
        // Right arrow
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "moveRight")
        actionMap.put("moveRight", object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent) {
                actions.onNavigate(Direction.RIGHT)
            }
        })
        
        // Enter/Space to place stone
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "placeStone")
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "placeStone2")
        actionMap.put("placeStone", object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent) {
                actions.onPlaceStone()
            }
        })
        actionMap.put("placeStone2", actionMap.get("placeStone"))
    }
    
    interface ShortcutActions {
        fun onNewGame()
        fun onUndo()
        fun onHelp()
        fun onSettings()
        fun onQuit()
        fun onNavigate(direction: Direction)
        fun onPlaceStone()
    }
    
    enum class Direction {
        UP, DOWN, LEFT, RIGHT
    }
}