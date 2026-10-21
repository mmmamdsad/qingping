package com.example.qingping;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.ComponentName;
import android.content.Context;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.Locale;

public class AdDetectionService extends AccessibilityService {
    private long lastSavedAt = 0L;
    private String lastSignature = "";

    // 这是启发式文本线索，不代表一定是广告。
    private static final String[] KEYWORDS = {
        "广告", "跳过", "跳过广告", "立即领取", "立即下载",
        "打开应用", "继续观看", "限时优惠", "去看看", "点击领取",
        "sponsored", "skip ad", "install now", "open app"
    };

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                | AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        info.flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS;
        info.notificationTimeout = 500;
        setServiceInfo(info);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) return;
        String packageName = event.getPackageName() == null
                ? "未知应用" : event.getPackageName().toString();

        // 避免把“清屏”自身界面里的说明文字当成其他应用的线索。
        if (getPackageName().equals(packageName)) return;

        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return;

        StringBuilder text = new StringBuilder();
        collectText(root, text, 0);
        root.recycle();

        String content = text.toString().trim();
        if (content.isEmpty()) return;

        String lower = content.toLowerCase(Locale.ROOT);
        String found = null;
        for (String keyword : KEYWORDS) {
            if (lower.contains(keyword.toLowerCase(Locale.ROOT))) {
                found = keyword;
                break;
            }
        }
        if (found == null) return;

        long now = System.currentTimeMillis();
        String signature = packageName + "|" + found + "|" + content.hashCode();
        // 限制重复事件造成的刷屏：相同线索 8 秒内只记录一次。
        if (signature.equals(lastSignature) && now - lastSavedAt < 8000) return;

        lastSignature = signature;
        lastSavedAt = now;
        String snippet = content.length() > 180 ? content.substring(0, 180) : content;
        LogStore.add(this, packageName,
                "发现疑似广告相关文字线索（关键词：" + found + "）；片段：" + snippet,
                "DETECTED");
    }

    private void collectText(AccessibilityNodeInfo node, StringBuilder out, int depth) {
        if (node == null || depth > 12 || out.length() > 4000) return;
        CharSequence text = node.getText();
        CharSequence desc = node.getContentDescription();
        if (!TextUtils.isEmpty(text)) out.append(text).append(" ");
        if (!TextUtils.isEmpty(desc)) out.append(desc).append(" ");
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                collectText(child, out, depth + 1);
                child.recycle();
            }
        }
    }

    @Override
    public void onInterrupt() {
        // 系统中断服务时不执行任何自动点击或关闭操作。
    }

    public static boolean isEnabled(Context context) {
        String enabled = Settings.Secure.getString(
                context.getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (enabled == null) return false;
        ComponentName expected = new ComponentName(context, AdDetectionService.class);
        TextUtils.SimpleStringSplitter splitter = new TextUtils.SimpleStringSplitter(':');
        splitter.setString(enabled);
        while (splitter.hasNext()) {
            ComponentName component = ComponentName.unflattenFromString(splitter.next());
            if (expected.equals(component)) return true;
        }
        return false;
    }
}
