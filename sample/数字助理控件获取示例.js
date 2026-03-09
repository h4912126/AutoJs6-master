/**
 * 数字助理 (szzl) 控件获取示例
 * Digital Assistant (szzl) UI Control Example
 * 
 * 核心功能：
 * - szzl.capture(timeout) - 自动唤醒、获取控件、隐藏助理（推荐）
 * - szzl.wakeUp() - 唤醒数字助理
 * - szzl.wakeUpAndWait(timeout) - 唤醒并等待获取控件
 * - szzl.dismiss() - 关闭数字助理
 * 
 * 特点：
 * - 助理界面不显示任何 UI（透明）
 * - 自动过滤掉助理自己的窗口，只获取目标页面控件
 * - 获取控件后自动隐藏助理
 * 
 * 使用步骤：
 * 1. 在系统设置中将 AutoJs6 设置为默认数字助理
 * 2. 运行脚本，会自动唤醒数字助理获取控件
 */

console.log("=== 数字助理控件获取示例 ===\n");

// 检查服务状态
console.log("检查服务状态...");
console.log("数字助理服务是否存在: " + szzl.exists());

// 如果服务未启用，打开设置页面
if (!szzl.exists()) {
    console.log("\n数字助理服务未启用！");
    console.log("正在打开设置页面，请将 AutoJs6 设置为默认数字助理...");
    szzl.launchSettings();
    console.log("\n设置完成后，请重新运行此脚本。");
    exit();
}

console.log("\n=== 使用 capture() 获取控件 ===");
console.log("正在自动唤醒数字助理并获取控件...");
console.log("（助理界面不会显示，会自动过滤掉助理窗口）\n");

// capture() 会自动：
// 1. 唤醒助理（不显示 UI）
// 2. 等待获取 AssistStructure
// 3. 过滤掉助理自己的窗口
// 4. 自动隐藏助理
var root = szzl.capture(5000);

if (!root) {
    console.log("获取控件失败！");
    console.log("\n可能的原因：");
    console.log("1. 数字助理未正确设置");
    console.log("2. 超时时间不够");
    console.log("3. 目标应用不支持 AssistStructure");
    exit();
}

console.log("控件获取成功！");
console.log("根节点类名: " + root.className());
console.log("子节点数量: " + root.childCount());

// 使用选择器查找控件
console.log("\n=== 使用选择器查找控件 ===");

var mySelector = szzlSelector().textContains("设置").clickable(true);
var results = mySelector.find();
console.log("找到 " + results.length + " 个包含'设置'的可点击控件");

var textResults = szzlText("确定").find();
console.log("找到 " + textResults.length + " 个文本为'确定'的控件");

var idResults = szzlIdContains("button").find();
console.log("找到 " + idResults.length + " 个ID包含'button'的控件");

// 查找单个控件
var oneResult = szzlTextContains("取消").findOne();
if (oneResult) {
    console.log("\n找到'取消'控件:");
    console.log("  文本: " + oneResult.text());
    console.log("  位置: " + oneResult.bounds());
    console.log("  可点击: " + oneResult.clickable());
}

// 链式调用
var chainResult = szzlSelector()
    .className("android.widget.Button")
    .enabled(true)
    .visibleToUser(true)
    .findOne();
    
if (chainResult) {
    console.log("\n找到按钮: " + chainResult.text());
}

// 遍历控件树（限制深度避免输出过多）
console.log("\n=== 控件树结构（前3层）===");
function traverseTree(node, depth, maxDepth) {
    if (depth > maxDepth) return;
    
    var indent = "  ".repeat(depth);
    var className = (node.className() || "").split(".").pop();
    var text = node.text() || "";
    var desc = node.desc() || "";
    var display = className;
    if (text) display += " [" + text.substring(0, 20) + (text.length > 20 ? "..." : "") + "]";
    if (desc) display += " desc:" + desc.substring(0, 20);
    
    console.log(indent + display);
    
    var children = node.children();
    for (var i = 0; i < children.length && i < 10; i++) {
        traverseTree(children[i], depth + 1, maxDepth);
    }
    if (children.length > 10) {
        console.log(indent + "  ... 还有 " + (children.length - 10) + " 个子节点");
    }
}

traverseTree(root, 0, 3);

// 使用 SzzlViewNode 的查找方法
console.log("\n=== 使用 SzzlViewNode 查找方法 ===");

var clickables = root.findClickable();
console.log("可点击控件数量: " + clickables.length);

var byText = root.findByText("确定");
console.log("包含'确定'的控件数量: " + byText.length);

var buttons = root.findByClassName("Button");
console.log("按钮数量: " + buttons.length);

console.log("\n=== 示例完成 ===");
console.log("\nAPI 说明：");
console.log("- szzl.capture(timeout) - 自动唤醒、获取、隐藏（推荐）");
console.log("- szzl.wakeUp() - 仅唤醒数字助理");
console.log("- szzl.wakeUpAndWait(timeout) - 唤醒并等待获取控件");
console.log("- szzl.dismiss() - 关闭数字助理界面");
console.log("\n特点：");
console.log("- 助理界面透明，不会遮挡目标页面");
console.log("- 自动过滤掉助理窗口，只获取目标页面控件");
