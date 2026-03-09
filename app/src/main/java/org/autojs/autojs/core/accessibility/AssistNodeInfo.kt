package org.autojs.autojs.core.accessibility

import android.app.assist.AssistStructure
import android.graphics.Matrix
import android.graphics.Point
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import androidx.annotation.Keep

/**
 * Created for Digital Assistant layout inspection.
 * Wraps AssistStructure.ViewNode to provide NodeInfo-like interface.
 */
@Suppress("unused", "MemberVisibilityCanBePrivate")
@Keep
class AssistNodeInfo(private val node: AssistStructure.ViewNode, var parent: AssistNodeInfo?) {

    // 滚动偏移量
    val scrollX: Int = node.scrollX
    val scrollY: Int = node.scrollY
    
    // 变换矩阵
    val transformation: Matrix? = node.transformation
    
    val boundsInParent: Rect = Rect(node.left, node.top, node.left + node.width, node.top + node.height)

    /**
     * 计算屏幕坐标
     * 需要考虑：
     * 1. 父节点的位置
     * 2. 父节点的滚动偏移 (scrollX, scrollY)
     * 3. 节点的变换矩阵 (transformation) - 包含平移、缩放、旋转等
     */
    val boundsInScreen: Rect by lazy {
        // 先计算本节点在父节点坐标系中的位置
        var left = node.left.toFloat()
        var top = node.top.toFloat()
        var right = (node.left + node.width).toFloat()
        var bottom = (node.top + node.height).toFloat()
        
        // 应用本节点的变换矩阵
        node.transformation?.let { matrix ->
            val rectF = RectF(left, top, right, bottom)
            matrix.mapRect(rectF)
            left = rectF.left
            top = rectF.top
            right = rectF.right
            bottom = rectF.bottom
        }
        
        // 遍历父节点链，累加偏移
        var p = parent
        while (p != null) {
            // 父节点的位置减去父节点的滚动偏移
            val offsetX = p.node.left - p.scrollX
            val offsetY = p.node.top - p.scrollY
            left += offsetX
            top += offsetY
            right += offsetX
            bottom += offsetY
            
            // 应用父节点的变换矩阵
            p.transformation?.let { matrix ->
                val rectF = RectF(left, top, right, bottom)
                matrix.mapRect(rectF)
                left = rectF.left
                top = rectF.top
                right = rectF.right
                bottom = rectF.bottom
            }
            
            p = p.parent
        }
        
        Rect(left.toInt(), top.toInt(), right.toInt(), bottom.toInt())
    }

    var isVisible: Boolean = node.visibility == 0
    val packageName: String? = node.idPackage
    val id: String? = node.idEntry
    val fullId: String? = node.idEntry?.let { "${node.idPackage}:id/$it" }
    val idHex: String? = null
    val desc: String? = node.contentDescription?.toString()
    val text: String? = node.text?.toString()
    val bounds: Rect = boundsInScreen
    val center: Point = Point(boundsInScreen.centerX(), boundsInScreen.centerY())
    val className: String? = node.className
    val clickable: Boolean = node.isClickable
    val longClickable: Boolean = node.isLongClickable
    // 通过 scrollX/scrollY 判断是否可滚动
    val scrollable: Boolean = scrollX != 0 || scrollY != 0
    val indexInParent: Int = 0
    val childCount: Int = node.getChildCount()
    val depth: Int by lazy {
        var d = 0
        var p = parent
        while (p != null) {
            d++
            p = p.parent
        }
        d
    }
    val checked: Boolean = node.isChecked
    val enabled: Boolean = node.isEnabled
    // ViewNode 没有 isEditable，使用 inputType 判断
    val editable: Boolean = node.inputType != 0
    val focusable: Boolean = node.isFocusable
    val checkable: Boolean = node.isCheckable
    val selected: Boolean = node.isSelected
    val dismissable: Boolean = false
    val visibleToUser: Boolean = node.visibility == 0
    val contextClickable: Boolean = node.isContextClickable
    val focused: Boolean = node.isFocused
    val accessibilityFocused: Boolean = node.isAccessibilityFocused
    val rowCount: Int = 0
    val columnCount: Int = 0
    val row: Int = 0
    val column: Int = 0
    val rowSpan: Int = 0
    val columnSpan: Int = 0
    val drawingOrder: Int = 0
    val actionNames: List<String> = emptyList()
    var hidden: Boolean = node.visibility != 0 && node.isActivated && node.alpha > 0

    val children: MutableList<AssistNodeInfo> = mutableListOf()

    override fun toString() = "$className { text=$text, desc=$desc, bounds=$boundsInScreen }"

    companion object {

        @JvmStatic
        fun capture(viewNode: AssistStructure.ViewNode, parent: AssistNodeInfo?): AssistNodeInfo {
            return AssistNodeInfo(viewNode, parent).apply {
                for (i in 0 until viewNode.getChildCount()) {
                    viewNode.getChildAt(i)?.let { child ->
                        children.add(capture(child, this))
                    }
                }
            }
        }

        @JvmStatic
        fun captureFromStructure(structure: AssistStructure): List<AssistWindowInfo> {
            val windows = mutableListOf<AssistWindowInfo>()
            for (i in 0 until structure.windowNodeCount) {
                val windowNode = structure.getWindowNodeAt(i)
                val rootViewNode = windowNode.rootViewNode
                if (rootViewNode != null) {
                    val rootNodeInfo = capture(rootViewNode, null)
                    windows.add(AssistWindowInfo(rootNodeInfo, windowNode.title, i, 0))
                }
            }
            return windows
        }

    }

}


/**
 * WindowInfo equivalent for AssistStructure
 */
class AssistWindowInfo(
    val root: AssistNodeInfo?,
    val title: CharSequence?,
    val order: Int,
    val type: Int
) {
    val packageName: String? = root?.packageName
    val rootClassName: String? = root?.className
}

/**
 * Capture equivalent for AssistStructure
 */
class AssistCapture(val windows: List<AssistWindowInfo>, var root: AssistNodeInfo)
