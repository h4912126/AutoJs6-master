package org.autojs.autojs.core.voiceinteraction;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.view.Window;
import android.view.WindowManager;


public class AssistActivity extends Activity {

    private final Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 窗口设置：全透明且不影响触摸焦点
        Window window = getWindow();
        window.setBackgroundDrawableResource(android.R.color.transparent);

        window.clearFlags(WindowManager.LayoutParams.FLAG_TOUCHABLE_WHEN_WAKING);
        window.addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

        // 无布局，无界面，不调用 setContentView()

        // 延迟1秒关闭，保证唤醒体验
        handler.postDelayed(this::finish, 100);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, 0);  // 关闭无动画，避免闪烁
    }
}