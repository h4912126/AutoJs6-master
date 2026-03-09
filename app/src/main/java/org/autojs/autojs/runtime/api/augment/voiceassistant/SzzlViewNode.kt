package org.autojs.autojs.runtime.api.augment.voiceassistant

import android.app.assist.AssistStructure
import android.content.res.Resources
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.RectF
import android.os.Build
import org.autojs.autojs.annotation.ScriptInterface
import org.autojs.autojs.util.StringUtils
import org.opencv.core.Point

/**
 * Created by SuperMonster003 on Mar 5, 2026.
 * Wrapper class for AssistStructure.ViewNode that provides UiObject-like interface.
 * This allows scripts to access view information from digital assistant service.
 */
@Suppress("unused", "MemberVisibilityCanBePrivate")
class SzzlViewNode(
    private val viewNode: AssistStructure.ViewNode,
    private val mDepth: Int = 0,
    private val mIndexInParent: Int = -1,
    private val mParent: SzzlViewNode? = null,
) {

    /**
     * 缓存计算后的屏幕坐标
     * 需要考虑：
     * 1. 父节点的位置
     * 2. 父节点的滚动偏移 (scrollX, scrollY)
     * 3. 节点的变换矩阵 (transformation) - 包含平移、缩放、旋转等
     */
    private val boundsInScreen: Rect by lazy {
        // 先计算本节点在父节点坐标系中的位置
        var left = viewNode.left.toFloat()
        var top = viewNode.top.toFloat()
        var right = (viewNode.left + viewNode.width).toFloat()
        var bottom = (viewNode.top + viewNode.height).toFloat()
        
        // 应用本节点的变换矩阵
        viewNode.transformation?.let { matrix ->
            val rectF = RectF(left, top, right, bottom)
            matrix.mapRect(rectF)
            left = rectF.left
            top = rectF.top
            right = rectF.right
            bottom = rectF.bottom
        }
        
        // 遍历父节点链，累加偏移
        var p = mParent
        while (p != null) {
            // 父节点的位置减去父节点的滚动偏移
            val offsetX = p.viewNode.left - p.viewNode.scrollX
            val offsetY = p.viewNode.top - p.viewNode.scrollY
            left += offsetX
            top += offsetY
            right += offsetX
            bottom += offsetY
            
            // 应用父节点的变换矩阵
            p.viewNode.transformation?.let { matrix ->
                val rectF = RectF(left, top, right, bottom)
                matrix.mapRect(rectF)
                left = rectF.left
                top = rectF.top
                right = rectF.right
                bottom = rectF.bottom
            }
            
            p = p.mParent
        }
        
        Rect(left.toInt(), top.toInt(), right.toInt(), bottom.toInt())
    }

    @ScriptInterface
    fun className(): String? = viewNode.className?.toString()

    @ScriptInterface
    fun packageName(): String? = viewNode.idPackage

    @ScriptInterface
    fun text(): String = viewNode.text?.toString() ?: ""

    @ScriptInterface
    fun desc(): String? = viewNode.contentDescription?.toString()

    @ScriptInterface
    fun content(): String = desc() ?: text()

    @ScriptInterface
    fun id(): String? = viewNode.idEntry

    @ScriptInterface
    fun fullId(): String? = viewNode.idEntry?.let { entry ->
        viewNode.idPackage?.let { pkg ->
            "$pkg:id/$entry"
        } ?: entry
    }

    @ScriptInterface
    fun idEntry(): String? = viewNode.idEntry

    /**
     * 返回节点在屏幕上的实际坐标（考虑父节点偏移、滚动和变换矩阵）
     */
    @ScriptInterface
    fun bounds(): Rect = boundsInScreen

    /**
     * 返回节点相对于父节点的局部坐标
     */
    @ScriptInterface
    fun boundsInParent(): Rect = Rect(
        viewNode.left,
        viewNode.top,
        viewNode.left + viewNode.width,
        viewNode.top + viewNode.height
    )

    @ScriptInterface
    fun boundsLeft(): Int = boundsInScreen.left

    @ScriptInterface
    fun left(): Int = boundsLeft()

    @ScriptInterface
    fun boundsTop(): Int = boundsInScreen.top

    @ScriptInterface
    fun top(): Int = boundsTop()

    @ScriptInterface
    fun boundsRight(): Int = boundsInScreen.right

    @ScriptInterface
    fun right(): Int = boundsRight()

    @ScriptInterface
    fun boundsBottom(): Int = boundsInScreen.bottom

    @ScriptInterface
    fun bottom(): Int = boundsBottom()

    @ScriptInterface
    fun boundsWidth(): Int = boundsInScreen.width()

    @ScriptInterface
    fun width(): Int = boundsWidth()

    @ScriptInterface
    fun boundsHeight(): Int = boundsInScreen.height()

    @ScriptInterface
    fun height(): Int = boundsHeight()

    @ScriptInterface
    fun boundsCenterX(): Int = boundsInScreen.centerX()

    @ScriptInterface
    fun centerX(): Int = boundsCenterX()

    @ScriptInterface
    fun boundsCenterY(): Int = boundsInScreen.centerY()

    @ScriptInterface
    fun centerY(): Int = boundsCenterY()

    @ScriptInterface
    fun point(): Point = Point(centerX().toDouble(), centerY().toDouble())

    @ScriptInterface
    fun center(): Point = point()

    @ScriptInterface
    fun clickable(): Boolean = viewNode.isClickable

    @ScriptInterface
    fun longClickable(): Boolean = viewNode.isLongClickable

    @ScriptInterface
    fun checkable(): Boolean = viewNode.isCheckable

    @ScriptInterface
    fun checked(): Boolean = viewNode.isChecked

    @ScriptInterface
    fun focusable(): Boolean = viewNode.isFocusable

    @ScriptInterface
    fun focused(): Boolean = viewNode.isFocused

    @ScriptInterface
    fun selected(): Boolean = viewNode.isSelected

    @ScriptInterface
    fun enabled(): Boolean = viewNode.isEnabled

    @ScriptInterface
    fun scrollable(): Boolean = false

    @ScriptInterface
    fun visibleToUser(): Boolean = viewNode.visibility == 0

    @ScriptInterface
    fun accessibilityFocused(): Boolean = viewNode.isAccessibilityFocused

    @ScriptInterface
    fun contextClickable(): Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        viewNode.isContextClickable
    } else {
        false
    }

    @ScriptInterface
    fun editable(): Boolean = viewNode.className?.toString()?.contains("EditText") == true

    @ScriptInterface
    fun depth(): Int = mDepth

    @ScriptInterface
    fun indexInParent(): Int = mIndexInParent

    @ScriptInterface
    fun childCount(): Int = viewNode.childCount

    @ScriptInterface
    fun hasChildren(): Boolean = viewNode.childCount > 0

    @ScriptInterface
    fun child(index: Int): SzzlViewNode? {
        val actualIndex = if (index < 0) index + viewNode.childCount else index
        if (actualIndex < 0 || actualIndex >= viewNode.childCount) return null
        val childViewNode = viewNode.getChildAt(actualIndex) ?: return null
        return SzzlViewNode(childViewNode, mDepth + 1, actualIndex, this)
    }

    @ScriptInterface
    fun firstChild(): SzzlViewNode? = child(0)

    @ScriptInterface
    fun lastChild(): SzzlViewNode? = child(viewNode.childCount - 1)

    @ScriptInterface
    fun children(): List<SzzlViewNode> {
        val result = mutableListOf<SzzlViewNode>()
        for (i in 0 until viewNode.childCount) {
            child(i)?.let { result.add(it) }
        }
        return result
    }

    @ScriptInterface
    fun parent(): SzzlViewNode? = mParent

    @ScriptInterface
    fun hint(): String? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        viewNode.hint?.toString()
    } else {
        null
    }

    @ScriptInterface
    fun inputType(): Int = viewNode.inputType

    @ScriptInterface
    fun visibility(): Int = viewNode.visibility

    @ScriptInterface
    fun alpha(): Float = viewNode.alpha

    @ScriptInterface
    fun elevation(): Float = viewNode.elevation

    @ScriptInterface
    fun transformation(): Matrix? = viewNode.transformation
    
    @ScriptInterface
    fun scrollX(): Int = viewNode.scrollX
    
    @ScriptInterface
    fun scrollY(): Int = viewNode.scrollY

    @ScriptInterface
    fun textColor(): Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        viewNode.textColor
    } else {
        0
    }

    @ScriptInterface
    fun textSize(): Float = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        viewNode.textSize
    } else {
        0f
    }

    @ScriptInterface
    fun extras(): android.os.Bundle? = viewNode.extras

    @ScriptInterface
    fun find(filter: (SzzlViewNode) -> Boolean): List<SzzlViewNode> {
        val result = mutableListOf<SzzlViewNode>()
        findRecursive(this, filter, result)
        return result
    }

    @ScriptInterface
    fun findOne(filter: (SzzlViewNode) -> Boolean): SzzlViewNode? {
        return findOneRecursive(this, filter)
    }

    @ScriptInterface
    fun findByText(text: String): List<SzzlViewNode> = find { it.text().contains(text) }

    @ScriptInterface
    fun findOneByText(text: String): SzzlViewNode? = findOne { it.text().contains(text) }

    @ScriptInterface
    fun findById(id: String): List<SzzlViewNode> = find { it.id() == id || it.fullId()?.contains(id) == true }

    @ScriptInterface
    fun findOneById(id: String): SzzlViewNode? = findOne { it.id() == id || it.fullId()?.contains(id) == true }

    @ScriptInterface
    fun findByDesc(desc: String): List<SzzlViewNode> = find { it.desc()?.contains(desc) == true }

    @ScriptInterface
    fun findOneByDesc(desc: String): SzzlViewNode? = findOne { it.desc()?.contains(desc) == true }

    @ScriptInterface
    fun findByClassName(className: String): List<SzzlViewNode> = find { it.className()?.contains(className) == true }

    @ScriptInterface
    fun findOneByClassName(className: String): SzzlViewNode? = findOne { it.className()?.contains(className) == true }

    @ScriptInterface
    fun findClickable(): List<SzzlViewNode> = find { it.clickable() }

    @ScriptInterface
    fun findOneClickable(): SzzlViewNode? = findOne { it.clickable() }

    private fun findRecursive(node: SzzlViewNode, filter: (SzzlViewNode) -> Boolean, result: MutableList<SzzlViewNode>) {
        // 跳过屏幕外的节点
        if (!node.isOnScreen()) {
            return
        }
        if (filter(node)) {
            result.add(node)
        }
        for (i in 0 until node.childCount()) {
            node.child(i)?.let { findRecursive(it, filter, result) }
        }
    }

    private fun findOneRecursive(node: SzzlViewNode, filter: (SzzlViewNode) -> Boolean): SzzlViewNode? {
        // 跳过屏幕外的节点
        if (!node.isOnScreen()) {
            return null
        }
        if (filter(node)) {
            return node
        }
        for (i in 0 until node.childCount()) {
            node.child(i)?.let { 
                val found = findOneRecursive(it, filter)
                if (found != null) return found
            }
        }
        return null
    }
    
    /**
     * 检查节点是否在屏幕范围内
     * 如果节点完全在屏幕外，返回 false
     */
    @ScriptInterface
    fun isOnScreen(): Boolean {
        val b = boundsInScreen
        return b.right > 0 && 
               b.bottom > 0 && 
               b.left < screenWidth && 
               b.top < screenHeight
    }

    fun summary(): String = listOf(
        "packageName" to { packageName() },
        "id" to { id() },
        "fullId" to { fullId() },
        "desc" to { desc() },
        "text" to { text() },
        "bounds" to { bounds() },
        "center" to { center() },
        "className" to { className() },
        "clickable" to { clickable() },
        "longClickable" to { longClickable() },
        "scrollable" to { scrollable() },
        "indexInParent" to { indexInParent() },
        "childCount" to { childCount() },
        "depth" to { depth() },
        "checked" to { checked() },
        "enabled" to { enabled() },
        "editable" to { editable() },
        "focusable" to { focusable() },
        "checkable" to { checkable() },
        "selected" to { selected() },
        "visibleToUser" to { visibleToUser() },
    ).let { StringUtils.toFormattedSummary(it) }

    override fun toString(): String {
        val simpledClassName = "${className()}".substringAfterLast(".")
        return "[${SzzlViewNode::class.java.simpleName}] $simpledClassName ${summary()}"
    }
    
    companion object {
        // 获取屏幕尺寸
        private val screenWidth: Int by lazy {
            Resources.getSystem().displayMetrics.widthPixels
        }
        private val screenHeight: Int by lazy {
            Resources.getSystem().displayMetrics.heightPixels
        }
    }

}
