package com.streakcounter.app;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** A task's schedule is fixed at creation so that past streaks cannot be rewritten. */
final class Task {
    String id, name;
    LocalDate created;
    int color;
    boolean[] days = new boolean[7]; // Monday through Sunday
    Set<LocalDate> completions = new HashSet<>();

    static Task create(String name, int color, boolean[] days) {
        Task t = new Task();
        t.id = UUID.randomUUID().toString();
        t.name = name;
        t.color = color;
        t.days = days.clone();
        t.created = LocalDate.now();
        return t;
    }

    boolean scheduled(LocalDate date) { return days[date.getDayOfWeek().getValue() - 1]; }
    boolean done(LocalDate date) { return completions.contains(date); }
    int daysPerWeek() { int n = 0; for (boolean d : days) if (d) n++; return n; }

    // Today remains available until midnight. Only a missed *past scheduled* day breaks a streak.
    int streak(LocalDate today) {
        int count = 0;
        LocalDate date = today;
        if (!done(date)) date = date.minusDays(1);
        while (!date.isBefore(created)) {
            if (scheduled(date)) {
                if (!done(date)) break;
                count++;
            }
            date = date.minusDays(1);
        }
        return count;
    }

    JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("id", id); o.put("name", name); o.put("created", created.toString()); o.put("color", color);
        JSONArray schedule = new JSONArray(); for (boolean day : days) schedule.put(day);
        JSONArray dates = new JSONArray(); for (LocalDate date : completions) dates.put(date.toString());
        o.put("days", schedule); o.put("completions", dates);
        return o;
    }
    static Task fromJson(JSONObject o) throws JSONException {
        Task t = new Task();
        t.id = o.getString("id"); t.name = o.getString("name");
        t.created = LocalDate.parse(o.getString("created")); t.color = o.getInt("color");
        JSONArray schedule = o.getJSONArray("days");
        for (int i = 0; i < 7; i++) t.days[i] = schedule.getBoolean(i);
        JSONArray dates = o.getJSONArray("completions");
        for (int i = 0; i < dates.length(); i++) t.completions.add(LocalDate.parse(dates.getString(i)));
        return t;
    }
}
