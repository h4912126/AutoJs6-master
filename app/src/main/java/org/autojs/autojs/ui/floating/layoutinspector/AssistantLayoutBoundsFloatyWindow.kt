package org.autojs.autojs.ui.floating.layoutinspector

import android.content.Context
import android.view.KeyEvent
import android.view.View
import org.autojs.autojs.core.accessibility.AssistCapture
import org.autojs.autojs.core.accessibility.AssistNodeInfo
import org.autojs.autojs.ui.enhancedfloaty.FloatyService
import org.autojs.autojs.ui.floating.AssistantLayoutFloatyWindow
import org.autojs.autojs.util.EventUtils
import org.autojs.autojs6.R

/**
 * Digital Assistant version of LayoutBoundsFloatyWindow.
 * Shows layout bounds using AssistStructure instead of AccessibilityService.
 */
open class AssistantLayoutBoundsFloatyWindow @JvmOverloads constructor(
    private val capture: AssistCapture,
    private val context: Context,
    isServiceRelied: Boolean = false,
) : AssistantLayoutFloatyWindow(capture, context, isServiceRelied) {

    private lateinit var mLayoutBoundsView: AssistantLayoutBoundsView

    override val popMenuActions = listOf(
        R.string.text_show_widget_information to ::showNodeInfo,
        R.string.text_show_layout_hierarchy to ::showLayoutHierarchy,
        SPLIT_LINE,
        R.string.text_hide_node to ::excludeNode,
        R.string.text_hide_same_frame_nodes to ::excludeAllBoundsSameNodeInternal,
        SPLIT_LINE,
        R.string.text_switch_window to ::switchWindow,
        R.string.text_exit to ::close,
    )

    override fun onCreateView(floatyService: FloatyService): View {
        onCreate(floatyService)

        return object : AssistantLayoutBoundsView(context) {
            override fun dispatchKeyEvent(e: KeyEvent) = when {
                EventUtils.isKeyBackAndActionUp(e) -> true.also { close() }
                EventUtils.isKeyVolumeDownAndActionDown(e) -> true.also { close() }
                else -> super.dispatchKeyEvent(e)
            }
        }.also { mLayoutBoundsView = it }
    }

    override fun onViewCreated(v: View) {
        mLayoutBoundsView.let { view ->
            view.setOnNodeInfoSelectListener { info: AssistNodeInfo ->
                view.isFocusable = false
                setLayoutSelectedNode(info)
                getBubblePopMenu().let { menu ->
                    val width = menu.contentView.measuredWidth
                    val bounds = info.boundsInScreen
                    val x = bounds.centerX() - width / 2
                    val y = bounds.bottom - view.statusBarHeight
                    if (width <= 0) {
                        try {
                            menu.preMeasure()
                        } catch (e: Exception) {
                            /* Ignored. */
                        }
                    }
                    menu.showAsDropDownAtLocation(view, bounds.height(), x, y)
                }
                view.isFocusable = true
            }
            view.boundsPaint.strokeWidth = 2f
            view.setRootNode(capture.root)
            getLayoutSelectedNode()?.let { view.setSelectedNode(it) }
        }
    }

    private fun excludeAllBoundsSameNodeInternal() {
        excludeAllBoundsSameNode(mLayoutBoundsView)
    }

}
