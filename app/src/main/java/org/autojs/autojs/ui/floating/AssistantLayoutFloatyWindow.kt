package org.autojs.autojs.ui.floating

import android.content.Context
import android.view.ContextThemeWrapper
import android.view.View
import android.view.ViewGroup
import org.autojs.autojs.app.AppLevelThemeDialogBuilder
import org.autojs.autojs.app.DialogUtils
import org.autojs.autojs.core.accessibility.AssistCapture
import org.autojs.autojs.core.accessibility.AssistNodeInfo
import org.autojs.autojs.core.accessibility.AssistWindowInfo
import org.autojs.autojs.ui.enhancedfloaty.FloatyService
import org.autojs.autojs.ui.floating.layoutinspector.AssistantLayoutBoundsFloatyWindow
import org.autojs.autojs.ui.floating.layoutinspector.AssistantLayoutBoundsView
import org.autojs.autojs.ui.floating.layoutinspector.AssistantLayoutHierarchyFloatyWindow
import org.autojs.autojs.ui.floating.layoutinspector.AssistantNodeInfoView
import org.autojs.autojs.ui.widget.BubblePopupMenu
import org.autojs.autojs6.R
import kotlin.reflect.KFunction0

/**
 * Base class for Digital Assistant layout inspection windows.
 * Similar to LayoutFloatyWindow but works with AssistStructure.
 */
abstract class AssistantLayoutFloatyWindow(
    private val capture: AssistCapture,
    private val context: Context,
    private val isServiceRelied: Boolean,
) : FullScreenFloatyWindow() {

    protected abstract val popMenuActions: List<Pair<Int, KFunction0<Unit>>?>

    private lateinit var mServiceContext: Context

    private var mLayoutSelectedNode: AssistNodeInfo? = null

    private val mNodeInfoView by lazy { AssistantNodeInfoView(mServiceContext) }

    private val mNodeInfoDialog by lazy {
        AppLevelThemeDialogBuilder(mServiceContext)
            .customView(mNodeInfoView, false)
            .build()
            .also { it.window!!.setType(FloatyWindowManger.getWindowType()) }
    }

    fun onCreate(floatyService: FloatyService) {
        mServiceContext = if (isServiceRelied) ContextThemeWrapper(floatyService, R.style.AppTheme) else context
    }

    fun setLayoutSelectedNode(selectedNode: AssistNodeInfo?) {
        mLayoutSelectedNode = selectedNode
    }

    protected fun getLayoutSelectedNode() = mLayoutSelectedNode

    protected fun getBubblePopMenu(): BubblePopupMenu {
        val (keys, values) = popMenuActions.filterNotNull().unzip()
        val dividerPositions = mutableListOf<Int>()
        var realIndex = -1
        for (pair in popMenuActions) {
            if (pair != null) {
                realIndex += 1
                continue
            }
            dividerPositions.add(realIndex)
        }
        return BubblePopupMenu(mServiceContext, keys.map { context.getString(it) }, dividerPositions)
            .apply {
                setOnItemClickListener { _: View?, position: Int ->
                    dismiss()
                    values.elementAtOrNull(position)?.invoke()
                }
                width = ViewGroup.LayoutParams.WRAP_CONTENT
                height = ViewGroup.LayoutParams.WRAP_CONTENT
            }
    }

    protected fun showNodeInfo() {
        mLayoutSelectedNode?.let { mNodeInfoView.setNodeInfo(it) }
        mNodeInfoDialog.show()
    }

    protected fun showLayoutBounds() {
        close()
        AssistantLayoutBoundsFloatyWindow(capture, context, isServiceRelied)
            .apply { setLayoutSelectedNode(mLayoutSelectedNode) }
            .let { FloatyService.addWindow(it) }
    }

    protected fun showLayoutHierarchy() {
        close()
        AssistantLayoutHierarchyFloatyWindow(capture, context, isServiceRelied)
            .apply { setLayoutSelectedNode(mLayoutSelectedNode) }
            .let { FloatyService.addWindow(it) }
    }

    protected fun switchWindow() {
        val windows = capture.windows
        val windowInfoList = windows.mapIndexed { index, win ->
            AssistWindowInfoDataSummary(
                win,
                title = "窗口 $index: ${win.title ?: "未知"}",
                packageName = win.packageName ?: "未知",
                rootClassName = win.rootClassName ?: "未知",
            )
        }
        val builder = AppLevelThemeDialogBuilder(mServiceContext)
            .title(context.getString(R.string.text_switch_window))
            .items(windowInfoList.map { "${it.title}\n${it.packageName}" })
            .itemsCallback { dialog, _, position, _ ->
                windowInfoList[position].window.root?.let {
                    dialog.dismiss()
                    capture.root = it
                    mLayoutSelectedNode = null
                    showLayoutBounds()
                }
            }
            .build()
        DialogUtils.showDialog(builder)
    }

    protected fun excludeNode() {
        mLayoutSelectedNode?.let {
            it.hidden = true
            mLayoutSelectedNode = null
        }
    }

    protected fun excludeAllBoundsSameNode(layoutBoundsView: AssistantLayoutBoundsView) {
        mLayoutSelectedNode?.let {
            layoutBoundsView.hideAllBoundsSameNode(it)
            mLayoutSelectedNode = null
        }
    }

    companion object {

        @JvmStatic
        protected val SPLIT_LINE: Nothing? = null

    }

    data class AssistWindowInfoDataSummary(
        val window: AssistWindowInfo,
        val title: String,
        val packageName: String,
        val rootClassName: String,
    )

}
