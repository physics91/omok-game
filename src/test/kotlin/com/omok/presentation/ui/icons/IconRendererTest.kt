package com.omok.presentation.ui.icons

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import javax.swing.ImageIcon
import java.awt.image.BufferedImage

class IconRendererTest {
    
    @Test
    fun testAllIconsRenderWithoutError() {
        // 모든 아이콘이 에러 없이 렌더링되는지 테스트
        IconLoader.Icon.values().forEach { icon ->
            val result = IconRenderer.renderIcon(icon, 24, 24)
            assertNotNull(result, "Icon ${icon.name} should render successfully")
            assertTrue(result is ImageIcon, "Result should be ImageIcon")
            
            val image = result.image
            assertTrue(image is BufferedImage, "Image should be BufferedImage")
            assertEquals(24, image.getWidth(null), "Width should be 24")
            assertEquals(24, image.getHeight(null), "Height should be 24")
        }
    }
    
    @Test
    fun testIconLoaderWithCaching() {
        // 아이콘 로더가 캐싱과 함께 작동하는지 테스트
        val icon1 = IconLoader.getIcon(IconLoader.Icon.BLACK_STONE, 32, 32)
        val icon2 = IconLoader.getIcon(IconLoader.Icon.BLACK_STONE, 32, 32)
        
        assertNotNull(icon1)
        assertNotNull(icon2)
        // 같은 아이콘은 캐시에서 가져와야 함
        assertSame(icon1, icon2, "Same icon should be returned from cache")
    }
    
    @Test
    fun testDifferentSizes() {
        // 다른 크기로 아이콘을 렌더링할 수 있는지 테스트
        val sizes = listOf(16, 24, 32, 48, 64)
        
        sizes.forEach { size ->
            val icon = IconLoader.getIcon(IconLoader.Icon.LOGO, size, size)
            assertNotNull(icon)
            assertEquals(size, icon?.iconWidth)
            assertEquals(size, icon?.iconHeight)
        }
    }
}