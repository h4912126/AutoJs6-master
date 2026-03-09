package org.autojs.autojs.runtime.api.augment.voiceassistant

import android.app.assist.AssistStructure
import android.os.Build
import android.view.accessibility.AccessibilityNodeInfo
import androidx.annotation.RequiresApi
import org.autojs.autojs.annotation.RhinoFunctionBody
import org.autojs.autojs.annotation.RhinoRuntimeFunctionInterface
import org.autojs.autojs.core.voiceinteraction.VoiceAssistantTool
import org.autojs.autojs.core.voiceinteraction.VoiceInteractionServiceUsher
import org.autojs.autojs.core.voiceinteraction.VoiceInteractionSessionUsher
import org.autojs.autojs.extension.AnyExtensions.isJsNullish
import org.autojs.autojs.extension.ArrayExtensions.toNativeArray
import org.autojs.autojs.extension.FlexibleArray
import org.autojs.autojs.runtime.ScriptRuntime
import org.autojs.autojs.runtime.api.augment.Augmentable
import org.autojs.autojs.runtime.api.augment.Invokable
import org.autojs.autojs.util.RhinoUtils.UNDEFINED
import org.mozilla.javascript.Context
import org.mozilla.javascript.NativeArray
import java.util.function.Supplier

/**
 * Created by SuperMonster003 on Mar 5, 2026.
 * Digital Assistant (数字助理) API for AutoJs6.
 * Similar to auto() but uses VoiceInteractionService instead of AccessibilityService.
 * 
 * Usage in scripts:
 * - szzl() - Open settings if service not running
 * - szzl.isRunning() - Check if service is running
 * - szzl.exists() - Check if service exists (is default assistant)
 * - szzl.launchSettings() - Open digital assistant settings
 * - szzl.getStructure() - Get current AssistStructure
 * - szzl.getWindowNodes() - Get all window nodes
 * - szzl.getViewNodes(windowIndex) - Get all view nodes from a window
 * - szzl.getRoots() - Get root SzzlViewNodes for all windows
 * - szzl.root - Get root SzzlViewNode for first window
 * - szzl.roots - Get root SzzlViewNodes for all windows
 * - szzlSelector() - Create a new SzzlSelector
 * - szzlText("text") - Shorthand for szzlSelector().text("text")
 * - szzlId("id") - Shorthand for szzlSelector().id("id")
 * - szzlDesc("desc") - Shorthand for szzlSelector().desc("desc")
 * - szzlClassName("className") - Shorthand for szzlSelector().className("className")
 */
@Suppress("unused", "UNUSED_PARAMETER")
class Szzl(private val scriptRuntime: ScriptRuntime) : Augmentable(scriptRuntime), Invokable {

    override val selfAssignmentFunctions = listOf(
        ::isRunning.name,
        ::exists.name,
        ::hasSession.name,
        ::isSessionShowing.name,
        ::launchSettings.name,
        ::wakeUp.name,
        ::wakeUpAndWait.name,
        ::dismiss.name,
        ::capture.name,
        ::getStructure.name,
        ::getWindowNodes.name,
        ::getViewNodes.name,
        ::getRoots.name,
        ::requestAssist.name,
        ::clearStructure.name,
        ::debugInfo.name,
        ::dumpNodes.name,
        ::printNodes.name,
    )
    
    override val globalAssignmentFunctions = listOf(
        ::szzlSelector.name,
        ::szzlText.name,
        ::szzlTextContains.name,
        ::szzlTextStartsWith.name,
        ::szzlTextEndsWith.name,
        ::szzlTextMatches.name,
        ::szzlId.name,
        ::szzlIdContains.name,
        ::szzlIdStartsWith.name,
        ::szzlIdEndsWith.name,
        ::szzlIdMatches.name,
        ::szzlDesc.name,
        ::szzlDescContains.name,
        ::szzlDescStartsWith.name,
        ::szzlDescEndsWith.name,
        ::szzlDescMatches.name,
        ::szzlClassName.name,
        ::szzlClassNameContains.name,
        ::szzlClassNameStartsWith.name,
        ::szzlClassNameEndsWith.name,
        ::szzlClassNameMatches.name,
        ::szzlPackageName.name,
        ::szzlPackageNameContains.name,
        ::szzlClickable.name,
        ::szzlLongClickable.name,
        ::szzlCheckable.name,
        ::szzlChecked.name,
        ::szzlFocusable.name,
        ::szzlFocused.name,
        ::szzlSelected.name,
        ::szzlEnabled.name,
        ::szzlScrollable.name,
        ::szzlEditable.name,
        ::szzlVisibleToUser.name,
        ::szzlDepth.name,
        ::szzlMinDepth.name,
        ::szzlMaxDepth.name,
        ::szzlBoundsInside.name,
        ::szzlBoundsContains.name,
    )

    override val selfAssignmentGetters = listOf<Pair<String, Supplier<Any?>>>(
        "service" to Supplier {
            voiceAssistantTool.let { 
                if (it.serviceExists()) VoiceInteractionServiceUsher.getInstance() else null 
            }
        },
        "session" to Supplier {
            VoiceInteractionSessionUsher.getInstance()
        },
        "structure" to Supplier {
            VoiceInteractionSessionUsher.currentAssistStructure
        },
        "windowCount" to Supplier {
            VoiceInteractionSessionUsher.currentAssistStructure?.windowNodeCount ?: 0
        },
        "root" to Supplier {
            getRootInternal(0)
        },
        "roots" to Supplier {
            getRootsInternal().toNativeArray()
        },
    )

    override fun invoke(vararg args: Any?): Any = ensureArgumentsAtMost(args, 1) {
        when {
            it.isEmpty() -> {
                if (!voiceAssistantTool.serviceExists()) {
                    voiceAssistantTool.launchSettings()
                }
            }
            it.size == 1 -> {
                val (o) = it
                when {
                    o.isJsNullish() -> {
                        if (!voiceAssistantTool.serviceExists()) {
                            voiceAssistantTool.launchSettings()
                        }
                    }
                    o is Boolean -> {
                        if (o && !voiceAssistantTool.serviceExists()) {
                            voiceAssistantTool.launchSettings()
                        }
                    }
                    else -> {}
                }
            }
        }
        return@ensureArgumentsAtMost UNDEFINED
    }

    companion object : FlexibleArray() {

        val voiceAssistantTool by lazy { VoiceAssistantTool(globalContext) }

        @JvmStatic
        @RhinoRuntimeFunctionInterface
        fun isRunning(scriptRuntime: ScriptRuntime, args: Array<out Any?>) = ensureArgumentsIsEmpty(args) {
            voiceAssistantTool.isServiceRunning()
        }

        @JvmStatic
        @RhinoRuntimeFunctionInterface
        fun exists(scriptRuntime: ScriptRuntime, args: Array<out Any?>) = ensureArgumentsIsEmpty(args) {
            voiceAssistantTool.serviceExists()
        }
        
        @JvmStatic
        @RhinoRuntimeFunctionInterface
        fun hasSession(scriptRuntime: ScriptRuntime, args: Array<out Any?>) = ensureArgumentsIsEmpty(args) {
            VoiceInteractionSessionUsher.hasInstance()
        }
        
        @JvmStatic
        @RhinoRuntimeFunctionInterface
        fun isSessionShowing(scriptRuntime: ScriptRuntime, args: Array<out Any?>) = ensureArgumentsIsEmpty(args) {
            VoiceInteractionSessionUsher.isSessionShowing
        }

        @JvmStatic
        @RhinoRuntimeFunctionInterface
        fun launchSettings(scriptRuntime: ScriptRuntime, args: Array<out Any?>) = ensureArgumentsIsEmpty(args) {
            voiceAssistantTool.launchSettings()
        }
        
        /**
         * 唤醒数字助理
         * @return 是否成功发送唤醒请求
         */
        @JvmStatic
        @RhinoRuntimeFunctionInterface
        fun wakeUp(scriptRuntime: ScriptRuntime, args: Array<out Any?>): Boolean = ensureArgumentsIsEmpty(args) {
            voiceAssistantTool.wakeUp()
        }
        
        /**
         * 唤醒数字助理并等待 AssistStructure
         * @param timeout 超时时间（毫秒），默认 5000ms
         * @return 是否成功获取到 AssistStructure
         */
        @JvmStatic
        @RhinoRuntimeFunctionInterface
        fun wakeUpAndWait(scriptRuntime: ScriptRuntime, args: Array<out Any?>): Boolean = ensureArgumentsAtMost(args, 1) {
            val (timeout) = it
            val timeoutMs = when {
                timeout.isJsNullish() -> 5000L
                timeout is Number -> timeout.toLong()
                else -> 5000L
            }
            voiceAssistantTool.wakeUpAndWait(timeoutMs)
        }
        
        /**
         * 关闭数字助理界面
         */
        @JvmStatic
        @RhinoRuntimeFunctionInterface
        fun dismiss(scriptRuntime: ScriptRuntime, args: Array<out Any?>) = ensureArgumentsIsEmpty(args) {
            voiceAssistantTool.dismiss()
        }
        
        /**
         * 捕获当前屏幕控件
         * 自动唤醒数字助理，获取控件后自动隐藏
         * 
         * 注意：此方法会：
         * 1. 唤醒数字助理（但不显示 UI）
         * 2. 获取目标页面的 AssistStructure
         * 3. 自动隐藏助理 Session
         * 
         * @param timeout 超时时间（毫秒），默认 5000ms
         * @return 根节点 SzzlViewNode，失败返回 null
         */
        @JvmStatic
        @RhinoRuntimeFunctionInterface
        fun capture(scriptRuntime: ScriptRuntime, args: Array<out Any?>): SzzlViewNode? = ensureArgumentsAtMost(args, 1) {
            val (timeout) = it
            val timeoutMs = when {
                timeout.isJsNullish() -> 5000L
                timeout is Number -> timeout.toLong()
                else -> 5000L
            }
            
            // 唤醒并等待（autoHide=true 会在获取后自动隐藏 Session）
            if (!voiceAssistantTool.wakeUpAndWait(timeoutMs, true)) {
                return@ensureArgumentsAtMost null
            }
            
            // 获取根节点（排除助理自己的窗口）
            getRootInternal(0)
        }

        @JvmStatic
        @RhinoRuntimeFunctionInterface
        fun getStructure(scriptRuntime: ScriptRuntime, args: Array<out Any?>): AssistStructure? = ensureArgumentsIsEmpty(args) {
            VoiceInteractionSessionUsher.currentAssistStructure
        }

        @JvmStatic
        @RhinoRuntimeFunctionInterface
        fun getWindowNodes(scriptRuntime: ScriptRuntime, args: Array<out Any?>): NativeArray = ensureArgumentsIsEmpty(args) {
            val structure = VoiceInteractionSessionUsher.currentAssistStructure
            if (structure == null) {
                return@ensureArgumentsIsEmpty NativeArray(0)
            }
            val windowNodes = mutableListOf<AssistStructure.WindowNode>()
            for (i in 0 until structure.windowNodeCount) {
                windowNodes.add(structure.getWindowNodeAt(i))
            }
            windowNodes.toNativeArray()
        }

        @JvmStatic
        @RhinoRuntimeFunctionInterface
        fun getViewNodes(scriptRuntime: ScriptRuntime, args: Array<out Any?>): NativeArray = ensureArgumentsAtMost(args, 1) {
            val structure = VoiceInteractionSessionUsher.currentAssistStructure
            if (structure == null) {
                return@ensureArgumentsAtMost NativeArray(0)
            }
            
            val (windowIndex) = it
            val index = when {
                windowIndex.isJsNullish() -> 0
                windowIndex is Number -> windowIndex.toInt()
                else -> 0
            }
            
            if (index < 0 || index >= structure.windowNodeCount) {
                return@ensureArgumentsAtMost NativeArray(0)
            }
            
            val windowNode = structure.getWindowNodeAt(index)
            val rootViewNode = windowNode.rootViewNode
            if (rootViewNode == null) {
                return@ensureArgumentsAtMost NativeArray(0)
            }
            
            val viewNodes = mutableListOf<SzzlViewNode>()
            // 创建 SzzlViewNode 以正确计算屏幕坐标
            val rootSzzlNode = SzzlViewNode(rootViewNode, 0, 0, null)
            collectViewNodes(rootSzzlNode, viewNodes, 0)
            viewNodes.toNativeArray()
        }
        
        @JvmStatic
        @RhinoRuntimeFunctionInterface
        fun getRoots(scriptRuntime: ScriptRuntime, args: Array<out Any?>): NativeArray = ensureArgumentsIsEmpty(args) {
            getRootsInternal().toNativeArray()
        }
        
        /**
         * Request AssistStructure from the current activity.
         * This will trigger the digital assistant to capture the current screen.
         * @param timeout Optional timeout in milliseconds (default 5000ms)
         * @return true if AssistStructure was received, false otherwise
         */
        @JvmStatic
        @RhinoRuntimeFunctionInterface
        fun requestAssist(scriptRuntime: ScriptRuntime, args: Array<out Any?>): Boolean = ensureArgumentsAtMost(args, 1) {
            val session = VoiceInteractionSessionUsher.getInstance()
            if (session == null) {
                return@ensureArgumentsAtMost false
            }
            
            if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M) {
                return@ensureArgumentsAtMost false
            }
            
            val (timeout) = it
            val timeoutMs = when {
                timeout.isJsNullish() -> 5000L
                timeout is Number -> timeout.toLong()
                else -> 5000L
            }
            
            session.requestAssistAndWait(timeoutMs)
        }
        
        /**
         * Clear the cached AssistStructure.
         */
        @JvmStatic
        @RhinoRuntimeFunctionInterface
        fun clearStructure(scriptRuntime: ScriptRuntime, args: Array<out Any?>) = ensureArgumentsIsEmpty(args) {
            VoiceInteractionSessionUsher.clearAssistStructure()
        }
        
        /**
         * Get debug information about the digital assistant service state.
         */
        @JvmStatic
        @RhinoRuntimeFunctionInterface
        fun debugInfo(scriptRuntime: ScriptRuntime, args: Array<out Any?>): String = ensureArgumentsIsEmpty(args) {
            val sb = StringBuilder()
            sb.appendLine("=== 数字助理调试信息 ===")
            sb.appendLine("服务是否存在 (serviceExists): ${voiceAssistantTool.serviceExists()}")
            sb.appendLine("服务是否运行 (isServiceRunning): ${voiceAssistantTool.isServiceRunning()}")
            sb.appendLine("服务实例 (VoiceInteractionServiceUsher): ${VoiceInteractionServiceUsher.getInstance()}")
            sb.appendLine("会话实例 (VoiceInteractionSessionUsher): ${VoiceInteractionSessionUsher.getInstance()}")
            sb.appendLine("会话是否显示 (isSessionShowing): ${VoiceInteractionSessionUsher.isSessionShowing}")
            
            val structure = VoiceInteractionSessionUsher.currentAssistStructure
            sb.appendLine("AssistStructure: $structure")
            if (structure != null) {
                sb.appendLine("窗口数量 (windowNodeCount): ${structure.windowNodeCount}")
                for (i in 0 until structure.windowNodeCount) {
                    val windowNode = structure.getWindowNodeAt(i)
                    sb.appendLine("  窗口 $i: title=${windowNode.title}")
                    sb.appendLine("    rootViewNode: ${windowNode.rootViewNode}")
                    windowNode.rootViewNode?.let { root ->
                        sb.appendLine("    rootViewNode.className: ${root.className}")
                        sb.appendLine("    rootViewNode.childCount: ${root.childCount}")
                    }
                }
            }

            sb.toString()
        }
        
        /**
         * 打印所有控件树到控制台
         * @param maxDepth 最大深度，默认 -1 表示无限制
         * @param refresh 是否先刷新 AssistStructure，默认 true
         */
        @JvmStatic
        @RhinoRuntimeFunctionInterface
        fun dumpNodes(scriptRuntime: ScriptRuntime, args: Array<out Any?>): String = ensureArgumentsAtMost(args, 2) {
            val (maxDepthArg, refreshArg) = it
            val maxDepth = when {
                maxDepthArg.isJsNullish() -> -1
                maxDepthArg is Number -> maxDepthArg.toInt()
                else -> -1
            }
            val refresh = when {
                refreshArg.isJsNullish() -> true
                refreshArg is Boolean -> refreshArg
                else -> true
            }
            
            // 如果需要刷新，先获取最新的 AssistStructure
            if (refresh) {
                /*voiceAssistantTool.wakeUpAndWait(3000, true)*/
                voiceAssistantTool.wakeUp()
            }
            
            val structure = VoiceInteractionSessionUsher.currentAssistStructure
            if (structure == null) {
                return@ensureArgumentsAtMost "AssistStructure 为空，请先唤醒数字助理"
            }
            
            val sb = StringBuilder()
            sb.appendLine("=== 控件树 ===")
            sb.appendLine("窗口数量: ${structure.windowNodeCount}")
            sb.appendLine()
            
            for (i in 0 until structure.windowNodeCount) {
                val windowNode = structure.getWindowNodeAt(i)
                val rootViewNode = windowNode.rootViewNode
                
                sb.appendLine("【窗口 $i】${windowNode.title}")
                if (rootViewNode != null) {
                    // 创建 SzzlViewNode 以正确计算屏幕坐标
                    val rootSzzlNode = SzzlViewNode(rootViewNode, 0, 0, null)
                    dumpNodeRecursive(rootSzzlNode, sb, 0, maxDepth)
                } else {
                    sb.appendLine("  (无根节点)")
                }
                sb.appendLine()
            }
            
            sb.toString()
        }
        
        /**
         * 打印所有控件树到控制台（同 dumpNodes，但会自动输出到 console）
         */
        @JvmStatic
        @RhinoRuntimeFunctionInterface
        fun printNodes(scriptRuntime: ScriptRuntime, args: Array<out Any?>) = ensureArgumentsAtMost(args, 2) {
            val result = dumpNodes(scriptRuntime, args)
            println(result)
        }
        
        private fun dumpNodeRecursive(szzlNode: SzzlViewNode, sb: StringBuilder, depth: Int, maxDepth: Int) {
            if (maxDepth >= 0 && depth > maxDepth) return
            
            val indent = "  ".repeat(depth)
            val className = szzlNode.className()?.split(".")?.lastOrNull() ?: "Unknown"
            val text = szzlNode.text().take(30)
            val desc = szzlNode.desc()?.take(20) ?: ""
            val id = szzlNode.id() ?: ""
            // 使用屏幕坐标
            val bounds = szzlNode.bounds()
            val boundsStr = "Rect(${bounds.left}, ${bounds.top} - ${bounds.right}, ${bounds.bottom})"
            
            val info = buildString {
                append(className)
                if (text.isNotEmpty()) append(" text=\"$text\"")
                if (desc.isNotEmpty()) append(" desc=\"$desc\"")
                if (id.isNotEmpty()) append(" id=\"$id\"")
                if (szzlNode.clickable()) append(" [可点击]")
                if (szzlNode.longClickable()) append(" [可长按]")
                append(" $boundsStr")
            }
            
            sb.appendLine("$indent$info")
            
            for (i in 0 until szzlNode.childCount()) {
                val child = szzlNode.child(i)
                if (child != null) {
                    dumpNodeRecursive(child, sb, depth + 1, maxDepth)
                }
            }
        }

        private fun collectViewNodes(szzlNode: SzzlViewNode, list: MutableList<SzzlViewNode>, depth: Int) {
            list.add(szzlNode)
            for (i in 0 until szzlNode.childCount()) {
                val child = szzlNode.child(i)
                if (child != null) {
                    collectViewNodes(child, list, depth + 1)
                }
            }
        }
        
        private val OUR_PACKAGE_NAME by lazy { 
            globalContext.packageName 
        }
        
        /**
         * 获取指定窗口的根节点
         * @param windowIndex 窗口索引，-1 表示获取第一个非助理窗口
         */
        private fun getRootInternal(windowIndex: Int): SzzlViewNode? {
            val structure = VoiceInteractionSessionUsher.currentAssistStructure ?: return null
            
            // 如果 windowIndex 为 -1 或 0，尝试获取第一个非助理窗口
            if (windowIndex <= 0) {
                return getFirstNonAssistantRoot(structure)
            }
            
            if (windowIndex >= structure.windowNodeCount) return null
            val windowNode = structure.getWindowNodeAt(windowIndex)
            val rootViewNode = windowNode.rootViewNode ?: return null
            return SzzlViewNode(rootViewNode, 0, 0, null)
        }
        
        /**
         * 获取第一个非助理应用的根节点
         * 过滤掉 AutoJs6 自己的窗口
         */
        private fun getFirstNonAssistantRoot(structure: AssistStructure): SzzlViewNode? {
            for (i in 0 until structure.windowNodeCount) {
                val windowNode = structure.getWindowNodeAt(i)
                val rootViewNode = windowNode.rootViewNode ?: continue
                
                // 检查是否是我们自己的窗口
                val windowTitle = windowNode.title?.toString() ?: ""
                val rootPackage = rootViewNode.idPackage ?: ""
                
                // 如果窗口标题或包名包含我们的包名，跳过
                if (windowTitle.contains(OUR_PACKAGE_NAME) || rootPackage.contains(OUR_PACKAGE_NAME)) {
                    android.util.Log.d("Szzl", "Skipping our window: title=$windowTitle, package=$rootPackage")
                    continue
                }
                
                // 如果窗口标题包含 "Assistant" 或 "助理"，跳过
                if (windowTitle.contains("Assistant", ignoreCase = true) || 
                    windowTitle.contains("助理") ||
                    windowTitle.contains("助手")) {
                    android.util.Log.d("Szzl", "Skipping assistant window: title=$windowTitle")
                    continue
                }
                
                android.util.Log.d("Szzl", "Found target window: title=$windowTitle, package=$rootPackage")
                return SzzlViewNode(rootViewNode, 0, 0, null)
            }
            
            // 如果没有找到非助理窗口，返回第一个窗口
            if (structure.windowNodeCount > 0) {
                val windowNode = structure.getWindowNodeAt(0)
                val rootViewNode = windowNode.rootViewNode
                if (rootViewNode != null) {
                    return SzzlViewNode(rootViewNode, 0, 0, null)
                }
            }
            
            return null
        }
        
        private fun getRootsInternal(): List<SzzlViewNode> {
            val structure = VoiceInteractionSessionUsher.currentAssistStructure ?: return emptyList()
            val roots = mutableListOf<SzzlViewNode>()
            for (i in 0 until structure.windowNodeCount) {
                val windowNode = structure.getWindowNodeAt(i)
                val rootViewNode = windowNode.rootViewNode ?: continue
                roots.add(SzzlViewNode(rootViewNode, 0, i, null))
            }
            return roots
        }
        
        /**
         * 获取所有非助理应用的根节点
         */
        private fun getNonAssistantRootsInternal(): List<SzzlViewNode> {
            val structure = VoiceInteractionSessionUsher.currentAssistStructure ?: return emptyList()
            val roots = mutableListOf<SzzlViewNode>()
            
            for (i in 0 until structure.windowNodeCount) {
                val windowNode = structure.getWindowNodeAt(i)
                val rootViewNode = windowNode.rootViewNode ?: continue
                
                val windowTitle = windowNode.title?.toString() ?: ""
                val rootPackage = rootViewNode.idPackage ?: ""
                
                // 过滤掉我们自己的窗口和助理窗口
                if (windowTitle.contains(OUR_PACKAGE_NAME) || 
                    rootPackage.contains(OUR_PACKAGE_NAME) ||
                    windowTitle.contains("Assistant", ignoreCase = true) ||
                    windowTitle.contains("助理") ||
                    windowTitle.contains("助手")) {
                    continue
                }
                
                roots.add(SzzlViewNode(rootViewNode, 0, i, null))
            }
            
            return roots
        }
        
        // Global selector functions
        
        @JvmStatic
        @RhinoRuntimeFunctionInterface
        fun szzlSelector(scriptRuntime: ScriptRuntime, args: Array<out Any?>): SzzlSelector = ensureArgumentsIsEmpty(args) {
            SzzlSelector.create()
        }
        
        @JvmStatic
        @RhinoRuntimeFunctionInterface
        fun szzlText(scriptRuntime: ScriptRuntime, args: Array<out Any?>): SzzlSelector = ensureArgumentsOnlyOne(args) {
            SzzlSelector.create().text(Context.toString(it))
        }
        
        @JvmStatic
        @RhinoRuntimeFunctionInterface
        fun szzlTextContains(scriptRuntime: ScriptRuntime, args: Array<out Any?>): SzzlSelector = ensureArgumentsOnlyOne(args) {
            SzzlSelector.create().textContains(Context.toString(it))
        }
        
        @JvmStatic
        @RhinoRuntimeFunctionInterface
        fun szzlTextStartsWith(scriptRuntime: ScriptRuntime, args: Array<out Any?>): SzzlSelector = ensureArgumentsOnlyOne(args) {
            SzzlSelector.create().textStartsWith(Context.toString(it))
        }
        
        @JvmStatic
        @RhinoRuntimeFunctionInterface
        fun szzlTextEndsWith(scriptRuntime: ScriptRuntime, args: Array<out Any?>): SzzlSelector = ensureArgumentsOnlyOne(args) {
            SzzlSelector.create().textEndsWith(Context.toString(it))
        }
        
        @JvmStatic
        @RhinoRuntimeFunctionInterface
        fun szzlTextMatches(scriptRuntime: ScriptRuntime, args: Array<out Any?>): SzzlSelector = ensureArgumentsOnlyOne(args) {
            SzzlSelector.create().textMatches(Context.toString(it))
        }
        
        @JvmStatic
        @RhinoRuntimeFunctionInterface
        fun szzlId(scriptRuntime: ScriptRuntime, args: Array<out Any?>): SzzlSelector = ensureArgumentsOnlyOne(args) {
            SzzlSelector.create().id(Context.toString(it))
        }
        
        @JvmStatic
        @RhinoRuntimeFunctionInterface
        fun szzlIdContains(scriptRuntime: ScriptRuntime, args: Array<out Any?>): SzzlSelector = ensureArgumentsOnlyOne(args) {
            SzzlSelector.create().idContains(Context.toString(it))
        }
        
        @JvmStatic
        @RhinoRuntimeFunctionInterface
        fun szzlIdStartsWith(scriptRuntime: ScriptRuntime, args: Array<out Any?>): SzzlSelector = ensureArgumentsOnlyOne(args) {
            SzzlSelector.create().idStartsWith(Context.toString(it))
        }
        
        @JvmStatic
        @RhinoRuntimeFunctionInterface
        fun szzlIdEndsWith(scriptRuntime: ScriptRuntime, args: Array<out Any?>): SzzlSelector = ensureArgumentsOnlyOne(args) {
            SzzlSelector.create().idEndsWith(Context.toString(it))
        }
        
        @JvmStatic
        @RhinoRuntimeFunctionInterface
        fun szzlIdMatches(scriptRuntime: ScriptRuntime, args: Array<out Any?>): SzzlSelector = ensureArgumentsOnlyOne(args) {
            SzzlSelector.create().idMatches(Context.toString(it))
        }
        
        @JvmStatic
        @RhinoRuntimeFunctionInterface
        fun szzlDesc(scriptRuntime: ScriptRuntime, args: Array<out Any?>): SzzlSelector = ensureArgumentsOnlyOne(args) {
            SzzlSelector.create().desc(Context.toString(it))
        }
        
        @JvmStatic
        @RhinoRuntimeFunctionInterface
        fun szzlDescContains(scriptRuntime: ScriptRuntime, args: Array<out Any?>): SzzlSelector = ensureArgumentsOnlyOne(args) {
            SzzlSelector.create().descContains(Context.toString(it))
        }
        
        @JvmStatic
        @RhinoRuntimeFunctionInterface
        fun szzlDescStartsWith(scriptRuntime: ScriptRuntime, args: Array<out Any?>): SzzlSelector = ensureArgumentsOnlyOne(args) {
            SzzlSelector.create().descStartsWith(Context.toString(it))
        }
        
        @JvmStatic
        @RhinoRuntimeFunctionInterface
        fun szzlDescEndsWith(scriptRuntime: ScriptRuntime, args: Array<out Any?>): SzzlSelector = ensureArgumentsOnlyOne(args) {
            SzzlSelector.create().descEndsWith(Context.toString(it))
        }
        
        @JvmStatic
        @RhinoRuntimeFunctionInterface
        fun szzlDescMatches(scriptRuntime: ScriptRuntime, args: Array<out Any?>): SzzlSelector = ensureArgumentsOnlyOne(args) {
            SzzlSelector.create().descMatches(Context.toString(it))
        }
        
        @JvmStatic
        @RhinoRuntimeFunctionInterface
        fun szzlClassName(scriptRuntime: ScriptRuntime, args: Array<out Any?>): SzzlSelector = ensureArgumentsOnlyOne(args) {
            SzzlSelector.create().className(Context.toString(it))
        }
        
        @JvmStatic
        @RhinoRuntimeFunctionInterface
        fun szzlClassNameContains(scriptRuntime: ScriptRuntime, args: Array<out Any?>): SzzlSelector = ensureArgumentsOnlyOne(args) {
            SzzlSelector.create().classNameContains(Context.toString(it))
        }
        
        @JvmStatic
        @RhinoRuntimeFunctionInterface
        fun szzlClassNameStartsWith(scriptRuntime: ScriptRuntime, args: Array<out Any?>): SzzlSelector = ensureArgumentsOnlyOne(args) {
            SzzlSelector.create().classNameStartsWith(Context.toString(it))
        }
        
        @JvmStatic
        @RhinoRuntimeFunctionInterface
        fun szzlClassNameEndsWith(scriptRuntime: ScriptRuntime, args: Array<out Any?>): SzzlSelector = ensureArgumentsOnlyOne(args) {
            SzzlSelector.create().classNameEndsWith(Context.toString(it))
        }
        
        @JvmStatic
        @RhinoRuntimeFunctionInterface
        fun szzlClassNameMatches(scriptRuntime: ScriptRuntime, args: Array<out Any?>): SzzlSelector = ensureArgumentsOnlyOne(args) {
            SzzlSelector.create().classNameMatches(Context.toString(it))
        }
        
        @JvmStatic
        @RhinoRuntimeFunctionInterface
        fun szzlPackageName(scriptRuntime: ScriptRuntime, args: Array<out Any?>): SzzlSelector = ensureArgumentsOnlyOne(args) {
            SzzlSelector.create().packageName(Context.toString(it))
        }
        
        @JvmStatic
        @RhinoRuntimeFunctionInterface
        fun szzlPackageNameContains(scriptRuntime: ScriptRuntime, args: Array<out Any?>): SzzlSelector = ensureArgumentsOnlyOne(args) {
            SzzlSelector.create().packageNameContains(Context.toString(it))
        }
        
        @JvmStatic
        @RhinoRuntimeFunctionInterface
        fun szzlClickable(scriptRuntime: ScriptRuntime, args: Array<out Any?>): SzzlSelector = ensureArgumentsAtMost(args, 1) {
            val (clickable) = it
            val value = when {
                clickable.isJsNullish() -> true
                clickable is Boolean -> clickable
                else -> true
            }
            SzzlSelector.create().clickable(value)
        }
        
        @JvmStatic
        @RhinoRuntimeFunctionInterface
        fun szzlLongClickable(scriptRuntime: ScriptRuntime, args: Array<out Any?>): SzzlSelector = ensureArgumentsAtMost(args, 1) {
            val (longClickable) = it
            val value = when {
                longClickable.isJsNullish() -> true
                longClickable is Boolean -> longClickable
                else -> true
            }
            SzzlSelector.create().longClickable(value)
        }
        
        @JvmStatic
        @RhinoRuntimeFunctionInterface
        fun szzlCheckable(scriptRuntime: ScriptRuntime, args: Array<out Any?>): SzzlSelector = ensureArgumentsAtMost(args, 1) {
            val (checkable) = it
            val value = when {
                checkable.isJsNullish() -> true
                checkable is Boolean -> checkable
                else -> true
            }
            SzzlSelector.create().checkable(value)
        }
        
        @JvmStatic
        @RhinoRuntimeFunctionInterface
        fun szzlChecked(scriptRuntime: ScriptRuntime, args: Array<out Any?>): SzzlSelector = ensureArgumentsAtMost(args, 1) {
            val (checked) = it
            val value = when {
                checked.isJsNullish() -> true
                checked is Boolean -> checked
                else -> true
            }
            SzzlSelector.create().checked(value)
        }
        
        @JvmStatic
        @RhinoRuntimeFunctionInterface
        fun szzlFocusable(scriptRuntime: ScriptRuntime, args: Array<out Any?>): SzzlSelector = ensureArgumentsAtMost(args, 1) {
            val (focusable) = it
            val value = when {
                focusable.isJsNullish() -> true
                focusable is Boolean -> focusable
                else -> true
            }
            SzzlSelector.create().focusable(value)
        }
        
        @JvmStatic
        @RhinoRuntimeFunctionInterface
        fun szzlFocused(scriptRuntime: ScriptRuntime, args: Array<out Any?>): SzzlSelector = ensureArgumentsAtMost(args, 1) {
            val (focused) = it
            val value = when {
                focused.isJsNullish() -> true
                focused is Boolean -> focused
                else -> true
            }
            SzzlSelector.create().focused(value)
        }
        
        @JvmStatic
        @RhinoRuntimeFunctionInterface
        fun szzlSelected(scriptRuntime: ScriptRuntime, args: Array<out Any?>): SzzlSelector = ensureArgumentsAtMost(args, 1) {
            val (selected) = it
            val value = when {
                selected.isJsNullish() -> true
                selected is Boolean -> selected
                else -> true
            }
            SzzlSelector.create().selected(value)
        }
        
        @JvmStatic
        @RhinoRuntimeFunctionInterface
        fun szzlEnabled(scriptRuntime: ScriptRuntime, args: Array<out Any?>): SzzlSelector = ensureArgumentsAtMost(args, 1) {
            val (enabled) = it
            val value = when {
                enabled.isJsNullish() -> true
                enabled is Boolean -> enabled
                else -> true
            }
            SzzlSelector.create().enabled(value)
        }
        
        @JvmStatic
        @RhinoRuntimeFunctionInterface
        fun szzlScrollable(scriptRuntime: ScriptRuntime, args: Array<out Any?>): SzzlSelector = ensureArgumentsAtMost(args, 1) {
            val (scrollable) = it
            val value = when {
                scrollable.isJsNullish() -> true
                scrollable is Boolean -> scrollable
                else -> true
            }
            SzzlSelector.create().scrollable(value)
        }
        
        @JvmStatic
        @RhinoRuntimeFunctionInterface
        fun szzlEditable(scriptRuntime: ScriptRuntime, args: Array<out Any?>): SzzlSelector = ensureArgumentsAtMost(args, 1) {
            val (editable) = it
            val value = when {
                editable.isJsNullish() -> true
                editable is Boolean -> editable
                else -> true
            }
            SzzlSelector.create().editable(value)
        }
        
        @JvmStatic
        @RhinoRuntimeFunctionInterface
        fun szzlVisibleToUser(scriptRuntime: ScriptRuntime, args: Array<out Any?>): SzzlSelector = ensureArgumentsAtMost(args, 1) {
            val (visible) = it
            val value = when {
                visible.isJsNullish() -> true
                visible is Boolean -> visible
                else -> true
            }
            SzzlSelector.create().visibleToUser(value)
        }
        
        @JvmStatic
        @RhinoRuntimeFunctionInterface
        fun szzlDepth(scriptRuntime: ScriptRuntime, args: Array<out Any?>): SzzlSelector = ensureArgumentsOnlyOne(args) {
            val depth = when (it) {
                is Number -> it.toInt()
                else -> 0
            }
            SzzlSelector.create().depth(depth)
        }
        
        @JvmStatic
        @RhinoRuntimeFunctionInterface
        fun szzlMinDepth(scriptRuntime: ScriptRuntime, args: Array<out Any?>): SzzlSelector = ensureArgumentsOnlyOne(args) {
            val minDepth = when (it) {
                is Number -> it.toInt()
                else -> 0
            }
            SzzlSelector.create().minDepth(minDepth)
        }
        
        @JvmStatic
        @RhinoRuntimeFunctionInterface
        fun szzlMaxDepth(scriptRuntime: ScriptRuntime, args: Array<out Any?>): SzzlSelector = ensureArgumentsOnlyOne(args) {
            val maxDepth = when (it) {
                is Number -> it.toInt()
                else -> Int.MAX_VALUE
            }
            SzzlSelector.create().maxDepth(maxDepth)
        }
        
        @JvmStatic
        @RhinoRuntimeFunctionInterface
        fun szzlBoundsInside(scriptRuntime: ScriptRuntime, args: Array<out Any?>): SzzlSelector = ensureArgumentsLength(args, 4) {
            val (l, t, r, b) = it
            val left = (l as? Number)?.toInt() ?: 0
            val top = (t as? Number)?.toInt() ?: 0
            val right = (r as? Number)?.toInt() ?: 0
            val bottom = (b as? Number)?.toInt() ?: 0
            SzzlSelector.create().boundsInside(left, top, right, bottom)
        }
        
        @JvmStatic
        @RhinoRuntimeFunctionInterface
        fun szzlBoundsContains(scriptRuntime: ScriptRuntime, args: Array<out Any?>): SzzlSelector = ensureArgumentsLength(args, 4) {
            val (l, t, r, b) = it
            val left = (l as? Number)?.toInt() ?: 0
            val top = (t as? Number)?.toInt() ?: 0
            val right = (r as? Number)?.toInt() ?: 0
            val bottom = (b as? Number)?.toInt() ?: 0
            SzzlSelector.create().boundsContains(left, top, right, bottom)
        }

    }

}
