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
 * Digital Assistant version of LayoutHierarchyFloatyWindow.
 * Shows layout hierarchy using AssistStructure instead of AccessibilityService.
 */
open class AssistantLayoutHierarchyFloatyWindow @JvmOverloads constructor(
    private val capture: AssistCapture,
    private val context: Context,
    isServiceRelied: Boolean = false,
) : AssistantLayoutFloatyWindow(capture, context, isServiceRelied) {

    private lateinit var mLayoutHierarchyView: AssistantLayoutHierarchyView

    override val popMenuActions = listOf(
        R.string.text_show_widget_information to ::showNodeInfo,
        R.string.text_show_layout_bounds to ::showLayoutBounds,
        SPLIT_LINE,
        R.string.text_switch_window to ::switchWindow,
        R.string.text_exit to ::close,
    )

    override fun onCreateView(floatyService: FloatyService): View {
        onCreate(floatyService)

        return object : AssistantLayoutHierarchyView(context) {
            override fun dispatchKeyEvent(e: KeyEvent) = when {
                EventUtils.isKeyBackAndActionUp(e) -> true.also { close() }
                EventUtils.isKeyVolumeDownAndActionDown(e) -> true.also { close() }
                else -> super.dispatchKeyEvent(e)
            }
        }.also { mLayoutHierarchyView = it }
    }

    override fun onViewCreated(v: View) {
        mLayoutHierarchyView.apply {
            setBackgroundColor(context.getColor(R.color.layout_hierarchy_window_background))
            setShowClickedNodeBounds(true)
            boundsPaint.strokeWidth = 3f
            boundsPaint.color = context.getColor(R.color.layout_hierarchy_window_node_bounds)
            setClickedBackgroundColor(context.getColor(R.color.layout_hierarchy_window_clicked_background))
            setOnItemLongClickListener { view: View, info: AssistNodeInfo ->
                setLayoutSelectedNode(info)
                getBubblePopMenu().let { menu ->
                    val width = menu.contentView.measuredWidth
                    val x = view.width / 2 - width / 2
                    val y = view.bottom
                    if (width <= 0) {
                        menu.preMeasure()
                    }
                    menu.showAsDropDownAtLocation(view, view.height, x, y)
                }
            }
            setRootNode(capture.root)
            getLayoutSelectedNode()?.let { setSelectedNode(it) }
        }
    }

}
