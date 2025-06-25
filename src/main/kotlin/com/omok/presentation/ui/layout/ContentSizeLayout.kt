package com.omok.presentation.ui.layout

import java.awt.*
import javax.swing.JComponent

/**
 * Android의 wrap_content와 유사한 레이아웃 시스템
 * 컴포넌트의 내용에 따라 자동으로 크기를 조정
 */
class ContentSizeLayout(
    private val orientation: Orientation = Orientation.VERTICAL,
    private val gap: Int = 0
) : LayoutManager2 {
    
    enum class Orientation {
        HORIZONTAL, VERTICAL
    }
    
    private val constraints = mutableMapOf<Component, LayoutConstraints>()
    
    override fun addLayoutComponent(comp: Component, constraints: Any?) {
        if (constraints is LayoutConstraints) {
            this.constraints[comp] = constraints
        } else {
            this.constraints[comp] = LayoutConstraints()
        }
    }
    
    override fun addLayoutComponent(name: String?, comp: Component) {
        constraints[comp] = LayoutConstraints()
    }
    
    override fun removeLayoutComponent(comp: Component) {
        constraints.remove(comp)
    }
    
    override fun preferredLayoutSize(parent: Container): Dimension {
        synchronized(parent.treeLock) {
            val insets = parent.insets
            var totalWidth = insets.left + insets.right
            var totalHeight = insets.top + insets.bottom
            var maxWidth = 0
            var maxHeight = 0
            
            val componentCount = parent.componentCount
            var visibleCount = 0
            
            for (i in 0 until componentCount) {
                val comp = parent.getComponent(i)
                if (!comp.isVisible) continue
                
                val pref = comp.preferredSize
                val constraint = constraints[comp] ?: LayoutConstraints()
                
                when (orientation) {
                    Orientation.HORIZONTAL -> {
                        totalWidth += pref.width
                        maxHeight = maxOf(maxHeight, pref.height)
                        if (visibleCount > 0) totalWidth += gap
                    }
                    Orientation.VERTICAL -> {
                        totalHeight += pref.height
                        maxWidth = maxOf(maxWidth, pref.width)
                        if (visibleCount > 0) totalHeight += gap
                    }
                }
                visibleCount++
            }
            
            return when (orientation) {
                Orientation.HORIZONTAL -> Dimension(totalWidth, maxHeight + insets.top + insets.bottom)
                Orientation.VERTICAL -> Dimension(maxWidth + insets.left + insets.right, totalHeight)
            }
        }
    }
    
    override fun minimumLayoutSize(parent: Container): Dimension {
        return preferredLayoutSize(parent)
    }
    
    override fun maximumLayoutSize(target: Container): Dimension {
        return Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE)
    }
    
    override fun layoutContainer(parent: Container) {
        synchronized(parent.treeLock) {
            val insets = parent.insets
            val bounds = parent.bounds
            val availableWidth = bounds.width - insets.left - insets.right
            val availableHeight = bounds.height - insets.top - insets.bottom
            
            var currentX = insets.left
            var currentY = insets.top
            
            val componentCount = parent.componentCount
            
            // First pass: calculate total weight and fixed space
            var totalWeight = 0.0
            var fixedSpace = 0
            val visibleComponents = mutableListOf<Component>()
            
            for (i in 0 until componentCount) {
                val comp = parent.getComponent(i)
                if (!comp.isVisible) continue
                
                visibleComponents.add(comp)
                val constraint = constraints[comp] ?: LayoutConstraints()
                
                if (constraint.weight > 0) {
                    totalWeight += constraint.weight
                } else {
                    val pref = comp.preferredSize
                    when (orientation) {
                        Orientation.HORIZONTAL -> fixedSpace += pref.width
                        Orientation.VERTICAL -> fixedSpace += pref.height
                    }
                }
            }
            
            // Add gaps to fixed space
            if (visibleComponents.size > 1) {
                fixedSpace += gap * (visibleComponents.size - 1)
            }
            
            // Calculate remaining space for weighted components
            val remainingSpace = when (orientation) {
                Orientation.HORIZONTAL -> availableWidth - fixedSpace
                Orientation.VERTICAL -> availableHeight - fixedSpace
            }
            
            // Second pass: layout components
            for (comp in visibleComponents) {
                val constraint = constraints[comp] ?: LayoutConstraints()
                val pref = comp.preferredSize
                
                val compWidth: Int
                val compHeight: Int
                
                when (orientation) {
                    Orientation.HORIZONTAL -> {
                        compWidth = if (constraint.weight > 0) {
                            (remainingSpace * constraint.weight / totalWeight).toInt()
                        } else {
                            pref.width
                        }
                        compHeight = when (constraint.verticalAlignment) {
                            Alignment.FILL -> availableHeight
                            else -> pref.height
                        }
                        
                        val yOffset = when (constraint.verticalAlignment) {
                            Alignment.START -> 0
                            Alignment.CENTER -> (availableHeight - compHeight) / 2
                            Alignment.END -> availableHeight - compHeight
                            Alignment.FILL -> 0
                        }
                        
                        comp.setBounds(currentX, currentY + yOffset, compWidth, compHeight)
                        currentX += compWidth + gap
                    }
                    Orientation.VERTICAL -> {
                        compHeight = if (constraint.weight > 0) {
                            (remainingSpace * constraint.weight / totalWeight).toInt()
                        } else {
                            pref.height
                        }
                        compWidth = when (constraint.horizontalAlignment) {
                            Alignment.FILL -> availableWidth
                            else -> pref.width
                        }
                        
                        val xOffset = when (constraint.horizontalAlignment) {
                            Alignment.START -> 0
                            Alignment.CENTER -> (availableWidth - compWidth) / 2
                            Alignment.END -> availableWidth - compWidth
                            Alignment.FILL -> 0
                        }
                        
                        comp.setBounds(currentX + xOffset, currentY, compWidth, compHeight)
                        currentY += compHeight + gap
                    }
                }
            }
        }
    }
    
    override fun getLayoutAlignmentX(target: Container): Float = 0.5f
    override fun getLayoutAlignmentY(target: Container): Float = 0.5f
    override fun invalidateLayout(target: Container) {}
    
    companion object {
        fun wrapContent() = LayoutConstraints(weight = 0.0)
        fun fillParent(weight: Double = 1.0) = LayoutConstraints(weight = weight)
        fun fixed(weight: Double = 0.0) = LayoutConstraints(weight = weight)
    }
}

/**
 * 레이아웃 제약 조건
 */
data class LayoutConstraints(
    val weight: Double = 0.0,  // 0 = wrap_content, >0 = weighted size
    val horizontalAlignment: Alignment = Alignment.START,
    val verticalAlignment: Alignment = Alignment.START,
    val margin: Insets = Insets(0, 0, 0, 0)
)

enum class Alignment {
    START, CENTER, END, FILL
}

/**
 * 레이아웃 빌더를 위한 확장 함수들
 */
fun Container.contentSizeLayout(
    orientation: ContentSizeLayout.Orientation = ContentSizeLayout.Orientation.VERTICAL,
    gap: Int = 0,
    builder: ContentSizeLayoutBuilder.() -> Unit
) {
    layout = ContentSizeLayout(orientation, gap)
    ContentSizeLayoutBuilder(this).apply(builder)
}

class ContentSizeLayoutBuilder(private val container: Container) {
    fun add(
        component: Component,
        constraints: LayoutConstraints = LayoutConstraints()
    ) {
        container.add(component, constraints)
    }
    
    fun addWrapContent(component: Component) {
        add(component, ContentSizeLayout.wrapContent())
    }
    
    fun addFillParent(component: Component, weight: Double = 1.0) {
        add(component, ContentSizeLayout.fillParent(weight))
    }
}

/**
 * 자동 크기 조정 패널
 */
open class AutoSizePanel : JComponent() {
    init {
        layout = ContentSizeLayout()
    }
    
    override fun getPreferredSize(): Dimension {
        return layout?.preferredLayoutSize(this) ?: super.getPreferredSize()
    }
    
    override fun getMinimumSize(): Dimension {
        return preferredSize
    }
}