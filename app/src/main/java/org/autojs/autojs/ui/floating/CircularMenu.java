package org.autojs.autojs.ui.floating;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.os.RemoteException;
import android.text.TextUtils;
import android.view.ContextThemeWrapper;
import android.view.View;
import androidx.annotation.NonNull;
import com.afollestad.materialdialogs.MaterialDialog;
import com.makeramen.roundedimageview.RoundedImageView;
import org.autojs.autojs.AutoJs;
import org.autojs.autojs.app.AppLevelThemeDialogBuilder;
import org.autojs.autojs.app.CircularMenuOperationDialogBuilder;
import org.autojs.autojs.app.DialogUtils;
import org.autojs.autojs.core.accessibility.AccessibilityTool;
import org.autojs.autojs.core.accessibility.Capture;
import org.autojs.autojs.core.accessibility.LayoutInspector;
import org.autojs.autojs.core.activity.ActivityInfoProvider;
import org.autojs.autojs.core.pref.Language;
import org.autojs.autojs.core.pref.Pref;
import org.autojs.autojs.core.record.GlobalActionRecorder;
import org.autojs.autojs.core.record.Recorder;
import org.autojs.autojs.model.explorer.ExplorerDirPage;
import org.autojs.autojs.model.explorer.Explorers;
import org.autojs.autojs.model.script.Scripts;
import org.autojs.autojs.runtime.api.WrappedShizuku;
import org.autojs.autojs.runtime.api.augment.shell.Shell;
import org.autojs.autojs.tool.Func1;
import org.autojs.autojs.ui.enhancedfloaty.FloatyService;
import org.autojs.autojs.ui.enhancedfloaty.FloatyWindow;
import org.autojs.autojs.ui.explorer.ExplorerView;
import org.autojs.autojs.ui.floating.layoutinspector.LayoutBoundsFloatyWindow;
import org.autojs.autojs.ui.floating.layoutinspector.LayoutHierarchyFloatyWindow;
import org.autojs.autojs.ui.floating.layoutinspector.AssistantLayoutBoundsFloatyWindow;
import org.autojs.autojs.ui.floating.layoutinspector.AssistantLayoutHierarchyFloatyWindow;
import org.autojs.autojs.core.accessibility.AssistCapture;
import org.autojs.autojs.core.accessibility.AssistNodeInfo;
import org.autojs.autojs.core.voiceinteraction.VoiceAssistantTool;
import org.autojs.autojs.core.voiceinteraction.VoiceInteractionSessionUsher;
import org.autojs.autojs.ui.main.MainActivity;
import org.autojs.autojs.util.ClipboardUtils;
import org.autojs.autojs.util.RootUtils;
import org.autojs.autojs.util.ShellUtils;
import org.autojs.autojs.util.ViewUtils;
import org.autojs.autojs.util.WorkingDirectoryUtils;
import org.autojs.autojs6.R;
import org.autojs.autojs6.databinding.CircularActionMenuBinding;
import org.greenrobot.eventbus.EventBus;
import org.jdeferred.Deferred;
import org.jdeferred.impl.DeferredObject;
import org.jetbrains.annotations.NotNull;

import java.text.MessageFormat;

/**
 * Created by Stardust on Oct 18, 2017.
 */
public class CircularMenu implements Recorder.OnStateChangedListener, LayoutInspector.CaptureAvailableListener {

    public record StateChangeEvent(int currentState, int previousState) {
        /* Empty record body. */
    }

    public static final int STATE_CLOSED = -1;
    public static final int STATE_NORMAL = 0;
    public static final int STATE_RECORDING = 1;

    CircularMenuWindow mWindow;

    private int mState;
    private RoundedImageView mActionViewIcon;
    private Context mContext;
    private final GlobalActionRecorder mRecorder;
    private CircularActionMenuBinding binding;
    private MaterialDialog mSettingsDialog;
    private MaterialDialog mLayoutInspectDialog;
    private String mRunningPackage;
    private String mRunningActivity;
    private Deferred<Capture, Void, Void> mCaptureDeferred;
    private final AccessibilityTool mA11yTool;

    private final View.OnClickListener mCollapseWindowAndInspectLayoutBoundsListener = v -> {
        mWindow.collapse();
        inspectLayout(capture -> new LayoutBoundsFloatyWindow(capture, mContext));
    };
    private final View.OnClickListener mCollapseWindowAndInspectLayoutHierarchyListener = v -> {
        mWindow.collapse();
        inspectLayout(capture -> new LayoutHierarchyFloatyWindow(capture, mContext));
    };
    private final View.OnClickListener mCollapseWindowAndInspectLayoutBoundsByDigitalAssistantListener = v -> {
        mWindow.collapse();
        inspectLayoutByDigitalAssistant(true);
    };
    private final View.OnClickListener mCollapseWindowAndInspectLayoutHierarchyByDigitalAssistantListener = v -> {
        mWindow.collapse();
        inspectLayoutByDigitalAssistant(false);
    };

    public CircularMenu(Context context) {
        // mContext = new ContextThemeWrapper(context, R.style.AppTheme);
        mContext = context;
        initFloaty();
        setupWindowListeners();
        mRecorder = GlobalActionRecorder.getSingleton(context);
        mRecorder.addOnStateChangedListener(this);
        AutoJs.getInstance().getLayoutInspector().addCaptureAvailableListener(this);
        mA11yTool = new AccessibilityTool(mContext);
    }

    private void setupWindowListeners() {
        mWindow.setOnActionViewClickListener(v -> {
            if (isRecording()) {
                stopRecord();
            } else if (mWindow.isExpanded()) {
                mWindow.collapse();
            } else {
                mCaptureDeferred = new DeferredObject<>();
                AutoJs.getInstance().getLayoutInspector().captureCurrentWindow();
                mWindow.expand();
            }
        });
    }

    private void setupBindingListeners() {
        binding.scriptList.setOnClickListener(v -> {
            mWindow.collapse();
            ExplorerView explorerView = new ExplorerView(mContext);
            explorerView.setExplorer(Explorers.workspace(), ExplorerDirPage.createRoot(WorkingDirectoryUtils.getPath()));
            explorerView.setDirectorySpanSize(2);
            final MaterialDialog dialog = new AppLevelThemeDialogBuilder(mContext)
                    .title(mContext.getString(R.string.text_run_script))
                    .customView(explorerView, false)
                    .positiveText(R.string.dialog_button_dismiss)
                    .positiveColorRes(R.color.dialog_button_default)
                    .cancelable(false)
                    .build();
            explorerView.setOnItemOperateListener(item -> dialog.dismiss());
            explorerView.setOnItemClickListener((view, item) -> {
                if (item.isExecutable()) {
                    Scripts.run(mContext, item.toScriptFile());
                } else {
                    new MaterialDialog.Builder(mContext)
                            .title(mContext.getString(R.string.error_failed_to_run_script))
                            .content(mContext.getString(R.string.text_file_with_abs_path_is_not_an_executable_script, item.toScriptFile().getAbsolutePath()))
                            .positiveText(R.string.dialog_button_dismiss)
                            .positiveColorRes(R.color.dialog_button_failure)
                            .show();
                }
            });
            explorerView.setOnProjectToolbarOperateListener(toolbar -> dialog.dismiss());
            explorerView.setOnProjectToolbarClickListener(toolbar -> toolbar.findViewById(R.id.project_run).performClick());
            explorerView.setProjectToolbarRunnableOnly(true);

            DialogUtils.adaptToExplorer(dialog, explorerView);
            DialogUtils.showDialog(dialog);
        });
        binding.record.setOnClickListener(v -> {
            mWindow.collapse();
            if (!RootUtils.isRootAvailable()) {
                DialogUtils.showDialog(new AppLevelThemeDialogBuilder(mContext)
                        .title(mContext.getString(R.string.text_no_root_access))
                        .content(mContext.getString(R.string.no_root_access_for_record))
                        .positiveText(R.string.dialog_button_quit)
                        .positiveColorRes(R.color.dialog_button_failure)
                        .build());
            } else {
                mRecorder.start();
            }
        });
        binding.layoutInspect.setOnClickListener(mCollapseWindowAndInspectLayoutBoundsListener);
        binding.layoutInspect.setOnLongClickListener(v -> {
            mWindow.collapse();
            mLayoutInspectDialog = new CircularMenuOperationDialogBuilder(mContext)
                    .item(R.drawable.ic_circular_menu_bounds, mContext.getString(R.string.text_inspect_layout_bounds), mCollapseWindowAndInspectLayoutBoundsListener)
                    .item(R.drawable.ic_circular_menu_hierarchy, mContext.getString(R.string.text_inspect_layout_hierarchy), mCollapseWindowAndInspectLayoutHierarchyListener)
                    .item(R.drawable.ic_assistant_black_48dp, mContext.getString(R.string.text_inspect_layout_bounds_digital_assistant), mCollapseWindowAndInspectLayoutBoundsByDigitalAssistantListener)
                    .item(R.drawable.ic_assistant_black_48dp, mContext.getString(R.string.text_inspect_layout_hierarchy_digital_assistant), mCollapseWindowAndInspectLayoutHierarchyByDigitalAssistantListener)
                    .title(mContext.getString(R.string.text_inspect_layout))
                    .build();
            DialogUtils.showDialog(mLayoutInspectDialog);
            return true;
        });
        binding.stopAllScripts.setOnClickListener(v -> {
            mWindow.collapse();
            if (AutoJs.getInstance().getScriptEngineService().stopAllAndToast() <= 0) {
                ViewUtils.showToast(mContext, mContext.getString(R.string.text_no_scripts_to_stop_running));
            }
        });
        binding.actionMenuMore.setOnClickListener(v -> {
            mWindow.collapse();

            if (mSettingsDialog != null && mSettingsDialog.isShowing()) {
                mSettingsDialog.dismiss();
            }

            applyComponentInformation();

            // noinspection CodeBlock2Expr
            mSettingsDialog = new CircularMenuOperationDialogBuilder(mContext)
                    .item(R.drawable.ic_accessibility_black_48dp, mContext.getString(R.string.text_manage_a11y_service), onCircularMenuItemClick(v1 -> {
                        mA11yTool.launchSettings();
                    }))
                    .item(R.drawable.ic_text_fields_black_48dp, mContext.getString(R.string.text_latest_package) + ":\n" + getRunningPackage(), onCircularMenuItemClick(v1 -> {
                        if (!TextUtils.isEmpty(mRunningPackage)) {
                            ClipboardUtils.setClip(mContext, mRunningPackage);
                            ViewUtils.showToast(mContext, getTextAlreadyCopied(R.string.text_latest_package));
                        }
                    }))
                    .item(R.drawable.ic_text_fields_black_48dp, mContext.getString(R.string.text_latest_activity) + ":\n" + getRunningActivity(), onCircularMenuItemClick(v1 -> {
                        if (!TextUtils.isEmpty(mRunningActivity)) {
                            ClipboardUtils.setClip(mContext, mRunningActivity);
                            ViewUtils.showToast(mContext, getTextAlreadyCopied(R.string.text_latest_activity));
                        }
                    }))
                    .item(R.drawable.ic_home_black_48dp, mContext.getString(R.string.text_open_main_activity), onCircularMenuItemClick(v1 -> {
                        MainActivity.launch(mContext);
                    }))
                    .item(R.drawable.ic_control_point_black_48dp, mContext.getString(R.string.text_pointer_location), onCircularMenuItemClick(v1 -> {
                        if (!ShellUtils.togglePointerLocation(mContext)) {
                            ViewUtils.showToast(mContext, mContext.getString(R.string.text_pointer_location_toggle_failed_with_hint), true);
                        }
                    }))
                    .item(R.drawable.ic_close_white_48dp, mContext.getString(R.string.text_close_floating_button), onCircularMenuItemClick(v1 -> {
                        closeAndSaveState(false);
                    }))
                    .title(mContext.getString(R.string.text_more))
                    .build();

            DialogUtils.showDialog(mSettingsDialog);
        });
    }

    private void applyComponentInformation() {
        if (WrappedShizuku.isOperational() && WrappedShizuku.service != null) {
            try {
                mRunningPackage = WrappedShizuku.service.currentPackage();
                mRunningActivity = WrappedShizuku.service.currentActivity();
                return;
            } catch (RemoteException ignored) {

            }
        }
        if (RootUtils.isRootAvailable()) {
            mRunningPackage = Shell.currentPackageRhino();
            mRunningActivity = Shell.currentActivityRhino();
            return;
        }
        ActivityInfoProvider infoProvider = AutoJs.getInstance().getInfoProvider();
        mRunningPackage = infoProvider.getLatestPackageByUsageStatsIfGranted();
        mRunningActivity = infoProvider.getLatestActivity();
    }

    @NonNull
    private View.OnClickListener onCircularMenuItemClick(View.OnClickListener listener) {
        return v -> {
            dismissSettingsDialog();
            listener.onClick(v);
        };
    }

    public boolean isRecording() {
        return mState == STATE_RECORDING;
    }

    private void initFloaty() {
        mWindow = new CircularMenuWindow(new CircularMenuFloaty() {
            @Override
            public CircularActionView inflateActionView(FloatyService service, CircularMenuWindow window) {
                CircularActionView actionView = (CircularActionView) View.inflate(service, R.layout.circular_action_view, null);
                mActionViewIcon = actionView.findViewById(R.id.icon);
                return actionView;
            }

            @Override
            public CircularActionMenu inflateMenuItems(FloatyService service, CircularMenuWindow window) {
                CircularActionMenu menu = (CircularActionMenu) View.inflate(new ContextThemeWrapper(service, R.style.AppTheme), R.layout.circular_action_menu, null);
                binding = CircularActionMenuBinding.bind(menu);
                setupBindingListeners();
                return menu;
            }
        });
        mWindow.setKeepToSideHiddenWidthRadio(0.25f);
        FloatyService.addWindow(mWindow);
    }

    private void setState(int state) {
        int previousState = mState;

        mState = state;
        mActionViewIcon.setImageResource(isRecording()
                ? R.drawable.ic_ali_record
                : R.drawable.autojs6_material);
        mActionViewIcon.setBackgroundTintList(ColorStateList.valueOf(mContext.getColor(isRecording()
                ? R.color.circular_menu_icon_red
                : R.color.circular_menu_icon_white)));
        int padding = (int) mContext.getResources().getDimension(isRecording()
                ? R.dimen.padding_circular_menu_recording
                : R.dimen.padding_circular_menu_normal);
        mActionViewIcon.setPadding(padding, padding, padding, padding);

        EventBus.getDefault().post(new StateChangeEvent(mState, previousState));
    }

    public void stopRecord() {
        mRecorder.stop();
    }

    private void inspectLayout(Func1<Capture, FloatyWindow> windowCreator) {
        if (mLayoutInspectDialog != null) {
            mLayoutInspectDialog.dismiss();
            mLayoutInspectDialog = null;
        }
        if (!mA11yTool.isServiceRunning()) {
            if (!mA11yTool.startService(false)) {
                ViewUtils.showToast(mContext, mContext.getString(R.string.error_no_accessibility_permission_to_capture));
                mA11yTool.launchSettings();
            }
        } else {
            mCaptureDeferred.promise().then(capture -> {
                mActionViewIcon.post(() -> FloatyService.addWindow(windowCreator.call(capture)));
            });
        }
    }

    /**
     * 使用数字助理检查布局
     * @param showBounds true 显示布局范围分析，false 显示布局层次分析
     */
    private void inspectLayoutByDigitalAssistant(boolean showBounds) {
        if (mLayoutInspectDialog != null) {
            mLayoutInspectDialog.dismiss();
            mLayoutInspectDialog = null;
        }
        
        VoiceAssistantTool voiceAssistantTool = new VoiceAssistantTool(mContext);
        
        if (!voiceAssistantTool.serviceExists()) {
            ViewUtils.showToast(mContext, mContext.getString(R.string.error_no_digital_assistant_permission_to_capture));
            voiceAssistantTool.launchSettings();
            return;
        }
        
        ViewUtils.showToast(mContext, mContext.getString(R.string.text_capturing_layout));
        
        new Thread(() -> {
            boolean success = voiceAssistantTool.wakeUpAndWait(5000, true);
            mActionViewIcon.post(() -> {
                if (success) {
                    android.app.assist.AssistStructure structure = VoiceInteractionSessionUsher.currentAssistStructure;
                    if (structure != null && structure.getWindowNodeCount() > 0) {
                        showDigitalAssistantLayoutWindow(structure, showBounds);
                    } else {
                        ViewUtils.showToast(mContext, mContext.getString(R.string.error_failed_to_capture_layout_by_digital_assistant));
                    }
                } else {
                    ViewUtils.showToast(mContext, mContext.getString(R.string.error_failed_to_capture_layout_by_digital_assistant));
                }
            });
        }).start();
    }

    private void showDigitalAssistantLayoutWindow(android.app.assist.AssistStructure structure, boolean showBounds) {
        
        // 从 AssistStructure 构建 AssistCapture，过滤不可见的窗口
        java.util.List<org.autojs.autojs.core.accessibility.AssistWindowInfo> windows =
            AssistNodeInfo.captureFromStructure(structure);

        if (windows.isEmpty()) {
            // 如果过滤后没有窗口，尝试使用原始方法
            windows = AssistNodeInfo.captureFromStructure(structure);
        }
        
        if (windows.isEmpty()) {
            ViewUtils.showToast(mContext, mContext.getString(R.string.error_failed_to_capture_layout_by_digital_assistant));
            return;
        }

        // 选择最合适的窗口（最大的非系统窗口）
        AssistNodeInfo rootNode = windows.get(0).getRoot();
        /*AssistNodeInfo rootNode = findBestRootNode(windows);
        if (rootNode == null) {
            // 如果找不到合适的，使用第一个窗口
            rootNode = windows.get(0).getRoot();
        }*/
        
        if (rootNode == null) {
            ViewUtils.showToast(mContext, mContext.getString(R.string.error_failed_to_capture_layout_by_digital_assistant));
            return;
        }
        
        AssistCapture capture = new AssistCapture(windows, rootNode);
        
        if (showBounds) {
            FloatyService.addWindow(new AssistantLayoutBoundsFloatyWindow(capture, mContext, false));
        } else {
            FloatyService.addWindow(new AssistantLayoutHierarchyFloatyWindow(capture, mContext, false));
        }
    }
    
    /**
     * 打印 AssistStructure 的调试信息
     */
    private void debugPrintStructureInfo(android.app.assist.AssistStructure structure) {
        android.util.DisplayMetrics metrics = mContext.getResources().getDisplayMetrics();
        int screenWidth = metrics.widthPixels;
        int screenHeight = metrics.heightPixels;
        
        android.util.Log.d("CircularMenu", "=== AssistStructure Debug ===");
        android.util.Log.d("CircularMenu", "Screen: " + screenWidth + "x" + screenHeight);
        
        for (int i = 0; i < structure.getWindowNodeCount(); i++) {
            android.app.assist.AssistStructure.WindowNode windowNode = structure.getWindowNodeAt(i);
            android.app.assist.AssistStructure.ViewNode root = windowNode.getRootViewNode();
            
            android.util.Log.d("CircularMenu", "Window " + i + ": title=" + windowNode.getTitle());
            if (root != null) {
                android.util.Log.d("CircularMenu", "  bounds=[" + root.getLeft() + "," + root.getTop() + 
                    "," + (root.getLeft() + root.getWidth()) + "," + (root.getTop() + root.getHeight()) + "]");
                android.util.Log.d("CircularMenu", "  scrollX=" + root.getScrollX() + ", scrollY=" + root.getScrollY());
                android.util.Log.d("CircularMenu", "  visibility=" + root.getVisibility() + ", alpha=" + root.getAlpha());
                android.util.Log.d("CircularMenu", "  isScrollable=false");
            }
        }
    }
    
    /**
     * 从 AssistStructure 中捕获可见的窗口
     * 使用 scrollX/scrollY 和坐标来判断窗口是否在当前页面
     */
    private java.util.List<org.autojs.autojs.core.accessibility.AssistWindowInfo> captureVisibleWindows(
            android.app.assist.AssistStructure structure) {
        
        android.util.DisplayMetrics metrics = mContext.getResources().getDisplayMetrics();
        int screenWidth = metrics.widthPixels;
        int screenHeight = metrics.heightPixels;
        
        java.util.List<org.autojs.autojs.core.accessibility.AssistWindowInfo> windows = new java.util.ArrayList<>();
        
        for (int i = 0; i < structure.getWindowNodeCount(); i++) {
            android.app.assist.AssistStructure.WindowNode windowNode = structure.getWindowNodeAt(i);
            android.app.assist.AssistStructure.ViewNode rootViewNode = windowNode.getRootViewNode();
            
            if (rootViewNode == null) continue;
            
            // 检查可见性
            if (rootViewNode.getVisibility() != 0) continue;
            
            // 检查 alpha
            if (rootViewNode.getAlpha() <= 0) continue;
            
            // 获取窗口的实际位置（考虑 scroll 偏移）
            int left = rootViewNode.getLeft() - rootViewNode.getScrollX();
            int top = rootViewNode.getTop() - rootViewNode.getScrollY();
            int right = left + rootViewNode.getWidth();
            int bottom = top + rootViewNode.getHeight();
            
            android.util.Log.d("CircularMenu", "Window " + i + " adjusted bounds: [" + left + "," + top + "," + right + "," + bottom + "]");
            
            // 检查窗口是否在屏幕范围内
            if (right <= 0 || left >= screenWidth || bottom <= 0 || top >= screenHeight) {
                android.util.Log.d("CircularMenu", "  -> Skipped: out of screen");
                continue;
            }
            
            // 捕获这个窗口
            AssistNodeInfo rootNodeInfo = AssistNodeInfo.capture(rootViewNode, null);
            windows.add(new org.autojs.autojs.core.accessibility.AssistWindowInfo(
                rootNodeInfo, windowNode.getTitle(), i, 0));
            android.util.Log.d("CircularMenu", "  -> Added");
        }
        
        return windows;
    }
    
    /**
     * 从多个窗口中选择最合适的根节点
     */
    private AssistNodeInfo findBestRootNode(java.util.List<org.autojs.autojs.core.accessibility.AssistWindowInfo> windows) {
        android.util.DisplayMetrics metrics = mContext.getResources().getDisplayMetrics();
        int screenWidth = metrics.widthPixels;
        int screenHeight = metrics.heightPixels;
        
        AssistNodeInfo bestNode = null;
        int maxVisibleArea = 0;
        
        for (org.autojs.autojs.core.accessibility.AssistWindowInfo window : windows) {
            AssistNodeInfo root = window.getRoot();
            if (root == null) continue;
            
            // 跳过 AutoJs6 自己的窗口
            String packageName = window.getPackageName();
            if (packageName != null && packageName.contains("autojs")) {
                continue;
            }
            
            // 跳过系统窗口
            CharSequence title = window.getTitle();
            if (title != null) {
                String titleStr = title.toString().toLowerCase();
                if (titleStr.contains("statusbar") || 
                    titleStr.contains("navigationbar") ||
                    titleStr.contains("assistant") ||
                    titleStr.contains("voice")) {
                    continue;
                }
            }
            
            // 使用 scrollX/scrollY 调整后的坐标
            int left = root.getBoundsInScreen().left - root.getScrollX();
            int top = root.getBoundsInScreen().top - root.getScrollY();
            int right = left + root.getBoundsInScreen().width();
            int bottom = top + root.getBoundsInScreen().height();
            
            // 检查是否在屏幕内
            if (right <= 0 || left >= screenWidth || bottom <= 0 || top >= screenHeight) {
                continue;
            }
            
            // 计算可见面积
            int visibleLeft = Math.max(left, 0);
            int visibleTop = Math.max(top, 0);
            int visibleRight = Math.min(right, screenWidth);
            int visibleBottom = Math.min(bottom, screenHeight);
            int visibleArea = (visibleRight - visibleLeft) * (visibleBottom - visibleTop);
            
            if (visibleArea > maxVisibleArea) {
                maxVisibleArea = visibleArea;
                bestNode = root;
            }
        }
        
        return bestNode;
    }

    public void closeAndSaveState(boolean state) {
        Pref.putBooleanSync(R.string.key_floating_menu_shown, state);
        savePosition();
        close();
    }

    public void savePosition() {
        mWindow.savePosition();
    }

    public void savePosition(@NotNull Configuration newConfig) {
        mWindow.savePosition(newConfig);
    }

    private String getRunningPackage() {
        if (!TextUtils.isEmpty(mRunningPackage)) {
            return mRunningPackage;
        }
        return getEmptyInfoHint();
    }

    private String getRunningActivity() {
        if (!TextUtils.isEmpty(mRunningActivity)) {
            return mRunningActivity;
        }
        return getEmptyInfoHint();
    }

    private String getEmptyInfoHint() {
        return MessageFormat.format("{0} ({1})",
                mContext.getString(R.string.text_null).toLowerCase(Language.getPrefLanguage().getLocale()),
                mContext.getString(R.string.text_a11y_service_may_be_needed).toLowerCase(Language.getPrefLanguage().getLocale()));
    }

    @Override
    public void onCaptureAvailable(@NotNull Capture capture, @NonNull Context context) {
        if (mCaptureDeferred != null && mCaptureDeferred.isPending()) {
            mCaptureDeferred.resolve(capture);
        }
    }

    private void dismissSettingsDialog() {
        if (mSettingsDialog != null) {
            mSettingsDialog.dismiss();
            mSettingsDialog = null;
        }
    }

    @NonNull
    private String getTextAlreadyCopied(int actionResId) {
        return MessageFormat.format("{0} ({1})",
                mContext.getString(R.string.text_already_copied_to_clip),
                mContext.getString(actionResId).toLowerCase(Language.getPrefLanguage().getLocale()));
    }

    public void close() {
        dismissSettingsDialog();
        try {
            mWindow.close();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } finally {
            EventBus.getDefault().post(new StateChangeEvent(STATE_CLOSED, mState));
            mState = STATE_CLOSED;
        }
        mRecorder.removeOnStateChangedListener(this);
        AutoJs.getInstance().getLayoutInspector().removeCaptureAvailableListener(this);
    }

    @Override
    public void onStart() {
        setState(STATE_RECORDING);
    }

    @Override
    public void onStop() {
        setState(STATE_NORMAL);
    }

    @Override
    public void onPause() {
        /* Empty body. */
    }

    @Override
    public void onResume() {
        /* Empty body. */
    }

}
