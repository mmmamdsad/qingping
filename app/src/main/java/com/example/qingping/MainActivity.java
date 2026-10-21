package com.example.qingping;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.graphics.Typeface;

import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends Activity {
    private TextView statusText;
    private TextView statsText;
    private TextView logText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildUi();
        refresh();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (statusText != null) refresh();
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(24), dp(20), dp(24));
        root.setBackgroundColor(0xFFF5F6F8);
        scroll.addView(root);

        TextView title = new TextView(this);
        title.setText("清屏");
        title.setTextSize(30);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(0xFF172033);
        root.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("广告线索检测 · 本地统计 · Android 16");
        subtitle.setTextSize(14);
        subtitle.setTextColor(0xFF596579);
        addGap(root, 6);
        root.addView(subtitle);

        statusText = section("服务状态：读取中", root);
        statsText = section("统计：读取中", root);
        logText = section("最近记录", root);

        Button enable = button("开启/管理无障碍服务", root);
        enable.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));

        Button simulate = button("添加一条【模拟】测试记录", root);
        simulate.setOnClickListener(v -> {
            LogStore.add(this, "演示数据", "模拟广告线索（不是真实拦截）", "SIMULATED");
            refresh();
        });

        Button refresh = button("刷新统计", root);
        refresh.setOnClickListener(v -> refresh());

        Button clear = button("清空本地记录", root);
        clear.setOnClickListener(v -> {
            LogStore.clear(this);
            refresh();
        });

        TextView note = new TextView(this);
        note.setText("\n说明：本应用当前检测的是界面文本线索，不会真正移除广告、阻止网络请求或自动点击关闭按钮。所有记录仅保存在本机。");
        note.setTextSize(13);
        note.setTextColor(0xFF7A3440);
        root.addView(note);

        setContentView(scroll);
    }

    private TextView section(String text, LinearLayout root) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(16);
        tv.setTextColor(0xFF172033);
        tv.setPadding(dp(14), dp(14), dp(14), dp(14));
        tv.setBackgroundColor(0xFFFFFFFF);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(14);
        root.addView(tv, lp);
        return tv;
    }

    private Button button(String text, LinearLayout root) {
        Button b = new Button(this);
        b.setText(text);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(8);
        root.addView(b, lp);
        return b;
    }

    private void refresh() {
        boolean enabled = AdDetectionService.isEnabled(this);
        statusText.setText("服务状态：" + (enabled ? "系统设置中已开启" : "尚未开启") +
                "\n\n点“开启/管理无障碍服务”，找到“清屏界面线索检测”并手动开启。");
        JSONArray all = LogStore.getAll(this);
        int real = 0, simulated = 0;
        for (int i = 0; i < all.length(); i++) {
            JSONObject o = all.optJSONObject(i);
            if (o == null) continue;
            if ("SIMULATED".equals(o.optString("kind"))) simulated++;
            else real++;
        }
        statsText.setText("本地统计\n\n检测线索记录：" + real +
                "\n模拟测试记录：" + simulated +
                "\n总记录数：" + all.length() +
                "\n\n注意：检测线索记录不等于成功拦截次数。");

        StringBuilder sb = new StringBuilder("最近记录\n\n");
        int shown = 0;
        for (int i = all.length() - 1; i >= 0 && shown < 20; i--) {
            JSONObject o = all.optJSONObject(i);
            if (o == null) continue;
            sb.append(o.optString("time", "")).append("\n")
              .append("应用：").append(o.optString("app", "未知应用")).append("\n")
              .append("类型：").append("SIMULATED".equals(o.optString("kind")) ? "模拟" : "检测线索").append("\n")
              .append(o.optString("detail", "")).append("\n\n");
            shown++;
        }
        if (shown == 0) sb.append("暂无记录。可以先添加一条模拟记录来测试界面。");
        logText.setText(sb.toString());
    }

    private void addGap(LinearLayout root, int dp) {
        View v = new View(this);
        root.addView(v, new LinearLayout.LayoutParams(1, dp(dp)));
    }
    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }
}
