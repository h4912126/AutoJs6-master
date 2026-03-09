package org.autojs.autojs.ui.floating.layoutinspector

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Region
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import org.autojs.autojs.core.accessibility.AssistNodeInfo
import org.autojs.autojs.util.ViewUtils
import org.autojs.autojs6.R
import java.util.Collections

/**
 * Digital Assistant version of LayoutBoundsView.
 * Displays layout bounds using AssistNodeInfo instead of NodeInfo.
 */
open class AssistantLayoutBoundsView : View {

    private var mRootNode: AssistNodeInfo? = null
    private var mTouchedNode: AssistNodeInfo? = null
    private var mOnNodeInfoSelectListener: OnAssistNodeInfoSelectListener? = null
    private var mTouchedNodeBounds: Rect? = null
    private var mBoundsInScreen: IntArray? = null

    open var touchedNodeBoundsColor = Color.RED
    open var normalNodeBoundsColor = Color.GREEN
    open var boundsPaint: Paint = Paint().apply {
        style = Paint.Style.STROKE
    }

    private var mFillingPaint: Paint = Paint().apply {
        style = Paint.Style.FILL
        color = context.getColor(R.color.layout_bounds_view_shadow)
    }

    var statusBarHeight = 0
        protected set

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)

    init {
        this.setWillNotDraw(false)
        statusBarHeight = ViewUtils.getStatusBarHeightByWindow(context)
    }

    fun interface OnAssistNodeInfoSelectListener {
        fun onNodeSelect(info: AssistNodeInfo)
    }

    fun setOnNodeInfoSelectListener(onNodeInfoSelectListener: OnAssistNodeInfoSelectListener?) {
        mOnNodeInfoSelectListener = onNodeInfoSelectListener
    }

    fun setRootNode(rootNode: AssistNodeInfo?) {
        mRootNode = rootNode
        mTouchedNode = null
    }

    fun hideAllBoundsSameNode(targetNode: AssistNodeInfo?) {
        if (mRootNode == null || targetNode == null) {
            return
        }
        hideNodes(mRootNode, targetNode)
    }

    private fun hideNodes(parent: AssistNodeInfo?, targetNode: AssistNodeInfo) {
        if (parent == null) {
            return
        }
        if (parent.boundsInScreen == targetNode.boundsInScreen) {
            parent.hidden = true
        }
        if (parent.children.isEmpty()) {
            return
        }
        for (child in parent.children) {
            hideNodes(child, targetNode)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (mBoundsInScreen == null) {
            mBoundsInScreen = IntArray(4)
            getLocationOnScreen(mBoundsInScreen)
            statusBarHeight = mBoundsInScreen!![1]
        }
        if (mTouchedNode != null) {
            canvas.save()
            if (mTouchedNodeBounds == null) {
                mTouchedNodeBounds = Rect(mTouchedNode!!.boundsInScreen)
                mTouchedNodeBounds!!.offset(0, -statusBarHeight)
            }
            @Suppress("DEPRECATION")
            canvas.clipRect(mTouchedNodeBounds!!, Region.Op.DIFFERENCE)
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), mFillingPaint)
        if (mTouchedNode != null) {
            canvas.restore()
        }
        boundsPaint.color = normalNodeBoundsColor
        draw(canvas, mRootNode)
        if (mTouchedNode != null) {
            boundsPaint.color = touchedNodeBoundsColor
            drawRect(canvas, mTouchedNode!!.boundsInScreen, statusBarHeight, boundsPaint)
        }
    }

    private fun draw(canvas: Canvas, node: AssistNodeInfo?) {
        if (node == null) return
        if (!node.hidden) {
            drawRect(canvas, node.boundsInScreen, statusBarHeight, boundsPaint)
        }
        for (child in node.children) {
            draw(canvas, child)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (mRootNode != null) {
            setSelectedNode(findNodeAt(mRootNode!!, event.rawX.toInt(), event.rawY.toInt()))
        }
        if (event.action == MotionEvent.ACTION_UP && mTouchedNode != null) {
            onNodeInfoClick(mTouchedNode!!)
            return true
        }
        return super.onTouchEvent(event)
    }

    private fun onNodeInfoClick(nodeInfo: AssistNodeInfo) {
        mOnNodeInfoSelectListener?.onNodeSelect(nodeInfo)
    }

    private fun findNodeAt(node: AssistNodeInfo, x: Int, y: Int): AssistNodeInfo? {
        val list = ArrayList<AssistNodeInfo>()
        findNodeAt(node, x, y, list)
        return if (list.isEmpty()) {
            null
        } else Collections.min(list, Comparator.comparingInt { o: AssistNodeInfo -> o.boundsInScreen.width() * o.boundsInScreen.height() })
    }

    private fun findNodeAt(node: AssistNodeInfo, x: Int, y: Int, list: MutableList<AssistNodeInfo>) {
        for (child in node.children) {
            if (child.boundsInScreen.contains(x, y)) {
                if (!child.hidden) {
                    list.add(child)
                }
                findNodeAt(child, x, y, list)
            }
        }
    }

    fun setSelectedNode(selectedNode: AssistNodeInfo?) {
        mTouchedNode = selectedNode
        mTouchedNodeBounds = null
        invalidate()
    }

    companion object {
        @JvmStatic
        fun drawRect(canvas: Canvas, rect: Rect?, statusBarHeight: Int, paint: Paint?) {
            val offsetRect = Rect(rect)
            offsetRect.offset(0, -statusBarHeight)
            canvas.drawRect(offsetRect, paint!!)
        }
    }

}
