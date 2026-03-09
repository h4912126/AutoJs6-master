package org.autojs.autojs.runtime.api.augment.voiceassistant

import android.app.assist.AssistStructure
import android.content.res.Resources
import org.autojs.autojs.annotation.ScriptInterface
import org.autojs.autojs.core.voiceinteraction.VoiceInteractionSessionUsher
import org.autojs.autojs.extension.ArrayExtensions.toNativeArray
import org.mozilla.javascript.NativeArray
import java.util.regex.Pattern

/**
 * Created by SuperMonster003 on Mar 5, 2026.
 * Selector class for finding SzzlViewNodes from AssistStructure.
 * Similar to UiSelector but works with digital assistant's AssistStructure.
 */
@Suppress("unused", "MemberVisibilityCanBePrivate")
class SzzlSelector {

    private val filters = mutableListOf<(SzzlViewNode) -> Boolean>()

    @ScriptInterface
    fun text(text: String): SzzlSelector {
        filters.add { it.text() == text }
        return this
    }

    @ScriptInterface
    fun textContains(text: String): SzzlSelector {
        filters.add { it.text().contains(text) }
        return this
    }

    @ScriptInterface
    fun textStartsWith(prefix: String): SzzlSelector {
        filters.add { it.text().startsWith(prefix) }
        return this
    }

    @ScriptInterface
    fun textEndsWith(suffix: String): SzzlSelector {
        filters.add { it.text().endsWith(suffix) }
        return this
    }

    @ScriptInterface
    fun textMatches(regex: String): SzzlSelector {
        val pattern = Pattern.compile(regex)
        filters.add { pattern.matcher(it.text()).matches() }
        return this
    }

    @ScriptInterface
    fun desc(desc: String): SzzlSelector {
        filters.add { it.desc() == desc }
        return this
    }

    @ScriptInterface
    fun descContains(desc: String): SzzlSelector {
        filters.add { it.desc()?.contains(desc) == true }
        return this
    }

    @ScriptInterface
    fun descStartsWith(prefix: String): SzzlSelector {
        filters.add { it.desc()?.startsWith(prefix) == true }
        return this
    }

    @ScriptInterface
    fun descEndsWith(suffix: String): SzzlSelector {
        filters.add { it.desc()?.endsWith(suffix) == true }
        return this
    }

    @ScriptInterface
    fun descMatches(regex: String): SzzlSelector {
        val pattern = Pattern.compile(regex)
        filters.add { it.desc()?.let { d -> pattern.matcher(d).matches() } == true }
        return this
    }

    @ScriptInterface
    fun id(id: String): SzzlSelector {
        filters.add { it.id() == id || it.fullId() == id }
        return this
    }

    @ScriptInterface
    fun idContains(id: String): SzzlSelector {
        filters.add { it.id()?.contains(id) == true || it.fullId()?.contains(id) == true }
        return this
    }

    @ScriptInterface
    fun idStartsWith(prefix: String): SzzlSelector {
        filters.add { it.id()?.startsWith(prefix) == true || it.fullId()?.startsWith(prefix) == true }
        return this
    }

    @ScriptInterface
    fun idEndsWith(suffix: String): SzzlSelector {
        filters.add { it.id()?.endsWith(suffix) == true || it.fullId()?.endsWith(suffix) == true }
        return this
    }

    @ScriptInterface
    fun idMatches(regex: String): SzzlSelector {
        val pattern = Pattern.compile(regex)
        filters.add { 
            it.id()?.let { i -> pattern.matcher(i).matches() } == true ||
            it.fullId()?.let { f -> pattern.matcher(f).matches() } == true
        }
        return this
    }

    @ScriptInterface
    fun className(className: String): SzzlSelector {
        filters.add { it.className() == className }
        return this
    }

    @ScriptInterface
    fun classNameContains(className: String): SzzlSelector {
        filters.add { it.className()?.contains(className) == true }
        return this
    }

    @ScriptInterface
    fun classNameStartsWith(prefix: String): SzzlSelector {
        filters.add { it.className()?.startsWith(prefix) == true }
        return this
    }

    @ScriptInterface
    fun classNameEndsWith(suffix: String): SzzlSelector {
        filters.add { it.className()?.endsWith(suffix) == true }
        return this
    }

    @ScriptInterface
    fun classNameMatches(regex: String): SzzlSelector {
        val pattern = Pattern.compile(regex)
        filters.add { it.className()?.let { c -> pattern.matcher(c).matches() } == true }
        return this
    }

    @ScriptInterface
    fun packageName(packageName: String): SzzlSelector {
        filters.add { it.packageName() == packageName }
        return this
    }

    @ScriptInterface
    fun packageNameContains(packageName: String): SzzlSelector {
        filters.add { it.packageName()?.contains(packageName) == true }
        return this
    }

    @ScriptInterface
    fun clickable(clickable: Boolean = true): SzzlSelector {
        filters.add { it.clickable() == clickable }
        return this
    }

    @ScriptInterface
    fun longClickable(longClickable: Boolean = true): SzzlSelector {
        filters.add { it.longClickable() == longClickable }
        return this
    }

    @ScriptInterface
    fun checkable(checkable: Boolean = true): SzzlSelector {
        filters.add { it.checkable() == checkable }
        return this
    }

    @ScriptInterface
    fun checked(checked: Boolean = true): SzzlSelector {
        filters.add { it.checked() == checked }
        return this
    }

    @ScriptInterface
    fun focusable(focusable: Boolean = true): SzzlSelector {
        filters.add { it.focusable() == focusable }
        return this
    }

    @ScriptInterface
    fun focused(focused: Boolean = true): SzzlSelector {
        filters.add { it.focused() == focused }
        return this
    }

    @ScriptInterface
    fun selected(selected: Boolean = true): SzzlSelector {
        filters.add { it.selected() == selected }
        return this
    }

    @ScriptInterface
    fun enabled(enabled: Boolean = true): SzzlSelector {
        filters.add { it.enabled() == enabled }
        return this
    }

    @ScriptInterface
    fun scrollable(scrollable: Boolean = true): SzzlSelector {
        filters.add { it.scrollable() == scrollable }
        return this
    }

    @ScriptInterface
    fun editable(editable: Boolean = true): SzzlSelector {
        filters.add { it.editable() == editable }
        return this
    }

    @ScriptInterface
    fun visibleToUser(visible: Boolean = true): SzzlSelector {
        filters.add { it.visibleToUser() == visible }
        return this
    }

    @ScriptInterface
    fun depth(depth: Int): SzzlSelector {
        filters.add { it.depth() == depth }
        return this
    }

    @ScriptInterface
    fun minDepth(minDepth: Int): SzzlSelector {
        filters.add { it.depth() >= minDepth }
        return this
    }

    @ScriptInterface
    fun maxDepth(maxDepth: Int): SzzlSelector {
        filters.add { it.depth() <= maxDepth }
        return this
    }

    @ScriptInterface
    fun boundsInside(left: Int, top: Int, right: Int, bottom: Int): SzzlSelector {
        filters.add { 
            val bounds = it.bounds()
            bounds.left >= left && bounds.top >= top && bounds.right <= right && bounds.bottom <= bottom
        }
        return this
    }

    @ScriptInterface
    fun boundsContains(left: Int, top: Int, right: Int, bottom: Int): SzzlSelector {
        filters.add { 
            val bounds = it.bounds()
            bounds.left <= left && bounds.top <= top && bounds.right >= right && bounds.bottom >= bottom
        }
        return this
    }

    @ScriptInterface
    fun filter(predicate: (SzzlViewNode) -> Boolean): SzzlSelector {
        filters.add(predicate)
        return this
    }

    @ScriptInterface
    fun find(): List<SzzlViewNode> {
        val structure = VoiceInteractionSessionUsher.currentAssistStructure ?: return emptyList()
        val result = mutableListOf<SzzlViewNode>()
        
        for (i in 0 until structure.windowNodeCount) {
            val windowNode = structure.getWindowNodeAt(i)
            val rootViewNode = windowNode.rootViewNode ?: continue
            val rootSzzlNode = SzzlViewNode(rootViewNode, 0, 0, null)
            findRecursive(rootSzzlNode, result)
        }
        
        return result
    }

    @ScriptInterface
    fun findInWindow(windowIndex: Int): List<SzzlViewNode> {
        val structure = VoiceInteractionSessionUsher.currentAssistStructure ?: return emptyList()
        if (windowIndex < 0 || windowIndex >= structure.windowNodeCount) return emptyList()
        
        val result = mutableListOf<SzzlViewNode>()
        val windowNode = structure.getWindowNodeAt(windowIndex)
        val rootViewNode = windowNode.rootViewNode ?: return emptyList()
        val rootSzzlNode = SzzlViewNode(rootViewNode, 0, 0, null)
        findRecursive(rootSzzlNode, result)
        
        return result
    }

    @ScriptInterface
    fun findOne(): SzzlViewNode? {
        val structure = VoiceInteractionSessionUsher.currentAssistStructure ?: return null
        
        for (i in 0 until structure.windowNodeCount) {
            val windowNode = structure.getWindowNodeAt(i)
            val rootViewNode = windowNode.rootViewNode ?: continue
            val rootSzzlNode = SzzlViewNode(rootViewNode, 0, 0, null)
            val found = findOneRecursive(rootSzzlNode)
            if (found != null) return found
        }
        
        return null
    }

    @ScriptInterface
    fun findOneInWindow(windowIndex: Int): SzzlViewNode? {
        val structure = VoiceInteractionSessionUsher.currentAssistStructure ?: return null
        if (windowIndex < 0 || windowIndex >= structure.windowNodeCount) return null
        
        val windowNode = structure.getWindowNodeAt(windowIndex)
        val rootViewNode = windowNode.rootViewNode ?: return null
        val rootSzzlNode = SzzlViewNode(rootViewNode, 0, 0, null)
        return findOneRecursive(rootSzzlNode)
    }

    @ScriptInterface
    fun exists(): Boolean = findOne() != null

    @ScriptInterface
    fun count(): Int = find().size

    private fun matchesAllFilters(node: SzzlViewNode): Boolean {
        return filters.all { it(node) }
    }
    
    /**
     * 检查节点是否在屏幕范围内
     * 如果节点完全在屏幕外，返回 false
     */
    private fun isNodeOnScreen(node: SzzlViewNode): Boolean {
        val bounds = node.bounds()
        // 如果节点完全在屏幕外，返回 false
        // 允许部分在屏幕内的节点通过
        return bounds.right > 0 && 
               bounds.bottom > 0 && 
               bounds.left < screenWidth && 
               bounds.top < screenHeight
    }

    private fun findRecursive(node: SzzlViewNode, result: MutableList<SzzlViewNode>) {
        // 先检查是否在屏幕内
        if (!isNodeOnScreen(node)) {
            return
        }
        if (matchesAllFilters(node)) {
            result.add(node)
        }
        for (i in 0 until node.childCount()) {
            node.child(i)?.let { findRecursive(it, result) }
        }
    }

    private fun findOneRecursive(node: SzzlViewNode): SzzlViewNode? {
        // 先检查是否在屏幕内
        if (!isNodeOnScreen(node)) {
            return null
        }
        if (matchesAllFilters(node)) {
            return node
        }
        for (i in 0 until node.childCount()) {
            node.child(i)?.let { 
                val found = findOneRecursive(it)
                if (found != null) return found
            }
        }
        return null
    }

    companion object {
        // 获取屏幕尺寸
        private val screenWidth: Int by lazy {
            Resources.getSystem().displayMetrics.widthPixels
        }
        private val screenHeight: Int by lazy {
            Resources.getSystem().displayMetrics.heightPixels
        }
        
        @JvmStatic
        fun create(): SzzlSelector = SzzlSelector()
    }

}
