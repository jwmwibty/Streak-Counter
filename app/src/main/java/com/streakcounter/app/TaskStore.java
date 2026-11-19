package com.streakcounter.app;

import android.content.Context;
import org.json.JSONArray;
import java.util.ArrayList;
import java.util.List;

final class TaskStore {
    private static final String PREFS = "daylight_tasks";
    static List<Task> load(Context context) {
        List<Task> tasks = new ArrayList<>();
        try {
            JSONArray data = new JSONArray(context.getSharedPreferences(PREFS, 0).getString("tasks", "[]"));
            for (int i = 0; i < data.length(); i++) {
                try { tasks.add(Task.fromJson(data.getJSONObject(i))); } catch (Exception ignored) { }
            }
        } catch (Exception ignored) { }
        return tasks;
    }
    static void save(Context context, List<Task> tasks) {
        JSONArray data = new JSONArray();
        for (Task task : tasks) {
            try { data.put(task.toJson()); } catch (Exception e) { throw new IllegalStateException(e); }
        }
        context.getSharedPreferences(PREFS, 0).edit().putString("tasks", data.toString()).apply();
    }
}
