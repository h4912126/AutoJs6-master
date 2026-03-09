package org.autojs.autojs.ui.floating.layoutinspector

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.yqritc.recyclerviewflexibledivider.HorizontalDividerItemDecoration
import org.autojs.autojs.core.accessibility.AssistNodeInfo
import org.autojs.autojs.util.ClipboardUtils
import org.autojs.autojs.util.ViewUtils
import org.autojs.autojs6.R
import org.autojs.autojs6.databinding.NodeInfoViewHeaderBinding
import org.autojs.autojs6.databinding.NodeInfoViewItemBinding

/**
 * Digital Assistant version of NodeInfoView.
 * Displays node information using AssistNodeInfo instead of NodeInfo.
 */
class AssistantNodeInfoView : RecyclerView {

    private val data = Array(FIELD_NAMES.size + 1) { Array(2) { "" } }

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle)

    init {
        initData()
        adapter = Adapter()
        layoutManager = LinearLayoutManager(context)
        addItemDecoration(
            HorizontalDividerItemDecoration.Builder(context)
                .color(context.getColor(R.color.layout_node_info_view_decoration_line))
                .size(context.resources.getInteger(R.integer.layout_node_info_view_decoration_line))
                .build()
        )
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setNodeInfo(nodeInfo: AssistNodeInfo) {
        for (i in FIELD_NAMES.indices) {
            try {
                data[i + 1][1] = when (FIELD_NAMES[i]) {
                    "packageName" -> nodeInfo.packageName ?: ""
                    "id" -> nodeInfo.id ?: ""
                    "fullId" -> nodeInfo.fullId ?: ""
                    "desc" -> nodeInfo.desc ?: ""
                    "text" -> nodeInfo.text ?: ""
                    "bounds" -> nodeInfo.bounds.let { "[ ${it.left}, ${it.top}, ${it.right}, ${it.bottom} ]" }
                    "center" -> nodeInfo.center.let { "[ ${it.x}, ${it.y} ]" }
                    "className" -> nodeInfo.className ?: ""
                    "clickable" -> nodeInfo.clickable.toString()
                    "longClickable" -> nodeInfo.longClickable.toString()
                    "scrollable" -> nodeInfo.scrollable.toString()
                    "indexInParent" -> nodeInfo.indexInParent.toString()
                    "childCount" -> nodeInfo.childCount.toString()
                    "depth" -> nodeInfo.depth.toString()
                    "checked" -> nodeInfo.checked.toString()
                    "enabled" -> nodeInfo.enabled.toString()
                    "editable" -> nodeInfo.editable.toString()
                    "focusable" -> nodeInfo.focusable.toString()
                    "checkable" -> nodeInfo.checkable.toString()
                    "selected" -> nodeInfo.selected.toString()
                    "visibleToUser" -> nodeInfo.visibleToUser.toString()
                    "focused" -> nodeInfo.focused.toString()
                    else -> ""
                }
            } catch (e: Exception) {
                data[i + 1][1] = ""
            }
        }
        adapter!!.notifyDataSetChanged()
    }

    private fun initData() {
        data[0][0] = resources.getString(R.string.text_attribute)
        data[0][1] = resources.getString(R.string.text_value)
        for (i in 1 until data.size) {
            data[i][0] = FIELD_NAMES[i - 1]
            data[i][1] = ""
        }
    }

    private inner class Adapter : RecyclerView.Adapter<ViewHolder>() {

        val mViewTypeHeader = 0
        val mViewTypeItem = 1

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = when (viewType) {
            mViewTypeHeader -> NodeInfoViewHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            else -> NodeInfoViewItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        }.let { ViewHolder(it) }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.apply {
                data[position].let {
                    attrName.text = it[0]
                    attrValue.text = it[1]
                }
            }
        }

        override fun getItemCount(): Int = data.size

        override fun getItemViewType(position: Int): Int = if (position == 0) mViewTypeHeader else mViewTypeItem

    }

    private inner class ViewHolder(itemViewBinding: ViewBinding) : RecyclerView.ViewHolder(itemViewBinding.root) {

        val attrName: TextView
        val attrValue: TextView

        init {
            itemViewBinding.root.setOnClickListener {
                bindingAdapterPosition.takeIf { it in 1.until(data.size) }?.let { i ->
                    ClipboardUtils.setClip(context, this@AssistantNodeInfoView.data[i][1])
                    ViewUtils.showSnack(this@AssistantNodeInfoView, R.string.text_already_copied_to_clip)
                }
            }
            when (itemViewBinding) {
                is NodeInfoViewHeaderBinding -> {
                    attrName = itemViewBinding.name
                    attrValue = itemViewBinding.value
                }
                is NodeInfoViewItemBinding -> {
                    attrName = itemViewBinding.name
                    attrValue = itemViewBinding.value
                }
                else -> throw IllegalArgumentException("Unknown binding: $itemViewBinding")
            }
        }

    }

    companion object {

        private val FIELD_NAMES = arrayOf(
            "packageName", "id", "fullId",
            "desc", "text",
            "bounds", "center", "className",
            "clickable", "longClickable", "scrollable",
            "indexInParent", "childCount", "depth",
            "checked", "enabled", "editable", "focusable", "checkable",
            "selected", "visibleToUser", "focused",
        )

    }

}
