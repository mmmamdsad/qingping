package com.example.qingping;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class LogStore {
    private static final String PREFS = "qingping_local_logs";
    private static final String KEY = "items";
    private static final int MAX_ITEMS = 500;

    private LogStore() {}

    public static synchronized void add(Context context, String app, String detail, String kind) {
        JSONArray items = getAll(context);
        JSONObject obj = new JSONObject();
        try {
            obj.put("time", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    .format(new Date()));
            obj.put("app", app);
            obj.put("detail", detail);
            obj.put("kind", kind);
            items.put(obj);
        } catch (Exception ignored) { }

        JSONArray trimmed = new JSONArray();
        int start = Math.max(0, items.length() - MAX_ITEMS);
        for (int i = start; i < items.length(); i++) {
            trimmed.put(items.opt(i));
        }
        prefs(context).edit().putString(KEY, trimmed.toString()).apply();
    }

    public static synchronized JSONArray getAll(Context context) {
        String raw = prefs(context).getString(KEY, "[]");
        try {
            return new JSONArray(raw);
        } catch (Exception e) {
            return new JSONArray();
        }
    }

    public static synchronized void clear(Context context) {
        prefs(context).edit().remove(KEY).apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
