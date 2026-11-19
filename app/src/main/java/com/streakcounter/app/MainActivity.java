package com.streakcounter.app;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputFilter;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int INK = Color.rgb(39, 37, 52), MUTED = Color.rgb(135, 132, 145), PURPLE = Color.rgb(113, 88, 217), BG = Color.rgb(247, 246, 242);
    private static final int[] COLORS = {0xFF745DDA, 0xFFEF9C75, 0xFF70AD9A, 0xFF7D9ED9, 0xFFE3889F};
    private static final String[] DAYS = {"M", "T", "W", "T", "F", "S", "S"};
    private static final String[] DAY_NAMES = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
    private List<Task> tasks = new ArrayList<>();
    private LinearLayout root;
    private int dp(float value) { return (int) (getResources().getDisplayMetrics().density * value + .5f); }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        ReminderReceiver.schedule(this);
    }
    @Override protected void onResume() { super.onResume(); tasks = TaskStore.load(this); render(); }
    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results); render();
    }

    private GradientDrawable shape(int color, float radius) {
        GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius)); return d;
    }
    private GradientDrawable outline(int color, int stroke, float radius) {
        GradientDrawable d = shape(color, radius); d.setStroke(dp(1), stroke); return d;
    }
    private TextView text(String value, float size, int color, boolean bold) {
        TextView t = new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(color);
        t.setFontFeatureSettings("kern"); t.setIncludeFontPadding(true);
        if (bold) t.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        return t;
    }
    private LinearLayout column() { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); return l; }
    private LinearLayout row() { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.HORIZONTAL); l.setGravity(Gravity.CENTER_VERTICAL); return l; }
    private LinearLayout.LayoutParams lp(int w, int h) { return new LinearLayout.LayoutParams(w < 0 ? w : dp(w), h < 0 ? h : dp(h)); }
    private void pad(View v, int l, int t, int r, int b) { v.setPadding(dp(l), dp(t), dp(r), dp(b)); }
    private void space(LinearLayout parent, int height) { parent.addView(new View(this), lp(1, height)); }
    private TextView pill(String s, int background, int foreground) {
        TextView t = text(s, 11, foreground, true); t.setGravity(Gravity.CENTER); pad(t, 10, 6, 10, 6); t.setBackground(shape(background, 20)); return t;
    }
    private void save() { TaskStore.save(this, tasks); render(); }

    private void render() {
        LocalDate today = LocalDate.now();
        int due = 0, done = 0, best = 0;
        for (Task task : tasks) {
            if (task.scheduled(today)) { due++; if (task.done(today)) done++; }
            best = Math.max(best, task.streak(today));
        }
        root = column(); root.setBackgroundColor(BG); setContentView(root);
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true); scroll.setClipToPadding(false);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        LinearLayout content = column(); pad(content, 22, 22, 22, 20); scroll.addView(content);

        LinearLayout top = row();
        TextView brandIcon = text("✦", 20, Color.WHITE, true); brandIcon.setGravity(Gravity.CENTER); brandIcon.setBackground(shape(PURPLE, 13)); top.addView(brandIcon, lp(42, 42));
        TextView brand = text("daylight", 22, INK, true); LinearLayout.LayoutParams brandP = lp(-2, -2); brandP.leftMargin = dp(10); top.addView(brand, brandP);
        View spacer = new View(this); top.addView(spacer, new LinearLayout.LayoutParams(0, 1, 1));
        TextView dateBadge = pill(today.format(DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())).toUpperCase(Locale.getDefault()), 0xFFEBE9E4, 0xFF6C6975); top.addView(dateBadge);
        content.addView(top); space(content, 30);
        TextView eyebrow = text(today.format(DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.getDefault())).toUpperCase(Locale.getDefault()), 11, PURPLE, true); eyebrow.setLetterSpacing(.12f); content.addView(eyebrow);
        space(content, 7);
        TextView title = text("Little steps.\nBig momentum.", 32, INK, true); title.setLineSpacing(dp(1), 1f); content.addView(title);
        space(content, 7); content.addView(text("Show up for yourself, one day at a time.", 14, MUTED, false));
        space(content, 24);

        LinearLayout summary = column(); pad(summary, 20, 20, 20, 20);
        GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{0xFF312B51, 0xFF554679, 0xFF7258A0}); gradient.setCornerRadius(dp(24)); summary.setBackground(gradient);
        LinearLayout summaryTop = row(); TextView label = text("TODAY'S PROGRESS", 10, 0xFFD2C8E9, true); label.setLetterSpacing(.12f); summaryTop.addView(label, new LinearLayout.LayoutParams(0, -2, 1)); summaryTop.addView(pill("✦  YOUR RHYTHM", 0xFF665682, 0xFFF3EBFF)); summary.addView(summaryTop);
        space(summary, 14);
        LinearLayout figures = row();
        TextView ratio = text(done + "/" + due, 36, Color.WHITE, true); figures.addView(ratio);
        TextView complete = text("  tasks completed", 13, 0xFFD7CFE7, false); figures.addView(complete, new LinearLayout.LayoutParams(0, -2, 1));
        TextView flame = text("✦  " + best, 17, 0xFFFFD39A, true); figures.addView(flame); summary.addView(figures);
        space(summary, 12);
        android.widget.ProgressBar bar = new android.widget.ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        bar.setMax(100); bar.setProgress(due == 0 ? 0 : done * 100 / due);
        GradientDrawable track = shape(0xFF75698E, 20), fill = shape(0xFFD0BCFF, 20);
        android.graphics.drawable.LayerDrawable layers = new android.graphics.drawable.LayerDrawable(new android.graphics.drawable.Drawable[]{track, new android.graphics.drawable.ClipDrawable(fill, Gravity.LEFT, android.graphics.drawable.ClipDrawable.HORIZONTAL)});
        layers.setId(0, android.R.id.background); layers.setId(1, android.R.id.progress); bar.setProgressDrawable(layers); summary.addView(bar, lp(-1, 7));
        space(summary, 12); summary.addView(text(due == 0 ? "A little breathing room today. Enjoy it." : done == due ? "All done! You showed up for yourself today." : "Every check-in is a win. You've got this.", 12, 0xFFE3DBEF, false));
        content.addView(summary); space(content, 27);

        LinearLayout heading = row(); TextView h = text("Your habits", 21, INK, true); heading.addView(h, new LinearLayout.LayoutParams(0, -2, 1)); heading.addView(pill(tasks.size() + (tasks.size() == 1 ? " HABIT" : " HABITS"), 0xFFEDEAF6, PURPLE)); content.addView(heading);
        space(content, 4); content.addView(text("A little consistency goes a long way.", 13, MUTED, false)); space(content, 15);
        if (tasks.isEmpty()) {
            LinearLayout empty = column(); empty.setGravity(Gravity.CENTER); pad(empty, 22, 27, 22, 27); empty.setBackground(outline(Color.WHITE, 0xFFECEAE6, 22));
            TextView icon = text("✦", 26, PURPLE, true); icon.setGravity(Gravity.CENTER); icon.setBackground(shape(0xFFF0ECFF, 18)); empty.addView(icon, lp(56, 56)); space(empty, 12);
            TextView emptyTitle = text("Start something good", 16, INK, true); emptyTitle.setGravity(Gravity.CENTER); empty.addView(emptyTitle); space(empty, 5);
            TextView emptyBody = text("Create your first habit and make space for the things that matter.", 13, MUTED, false); emptyBody.setGravity(Gravity.CENTER); empty.addView(emptyBody); content.addView(empty);
        } else {
            for (Task task : tasks) { content.addView(taskCard(task, today)); space(content, 11); }
        }
        if (!notificationsEnabled()) {
            space(content, 8);
            TextView notice = text("🔔  Turn on reminders  →\nGet a nudge around 8 PM before a streak breaks.", 13, PURPLE, true); pad(notice, 16, 14, 16, 14); notice.setBackground(shape(0xFFEEEAFB, 16));
            notice.setOnClickListener(v -> enableNotifications()); content.addView(notice);
        } else {
            space(content, 8); TextView note = text("◷  Streak reminders are on for around 8 PM", 12, MUTED, false); note.setGravity(Gravity.CENTER); content.addView(note);
        }
        LinearLayout footer = column(); pad(footer, 22, 8, 22, 18); footer.setBackgroundColor(BG);
        TextView add = text("＋   Add a habit", 16, Color.WHITE, true); add.setGravity(Gravity.CENTER); add.setBackground(shape(PURPLE, 18)); add.setOnClickListener(v -> showAddDialog()); footer.addView(add, lp(-1, 56)); root.addView(footer);
    }

    private View taskCard(Task task, LocalDate today) {
        boolean scheduled = task.scheduled(today), done = task.done(today);
        LinearLayout card = column(); pad(card, 17, 16, 17, 15); card.setBackground(outline(Color.WHITE, 0xFFEDEBE7, 20));
        LinearLayout top = row();
        TextView icon = text("✦", 21, task.color, true); icon.setGravity(Gravity.CENTER); icon.setBackground(shape(blend(task.color, Color.WHITE, .88f), 15)); top.addView(icon, lp(48, 48));
        LinearLayout info = column(); LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(0, -2, 1); ip.leftMargin = dp(12); top.addView(info, ip);
        TextView name = text(task.name, 16, INK, true); name.setMaxLines(2); info.addView(name); space(info, 2);
        info.addView(text(task.daysPerWeek() + (task.daysPerWeek() == 1 ? " day" : " days") + " a week  ·  " + (scheduled ? (done ? "Done for today" : "Scheduled today") : "Rest day"), 12, MUTED, false));
        TextView more = text("⋯", 25, MUTED, true); more.setGravity(Gravity.CENTER); more.setContentDescription("Delete " + task.name); top.addView(more, lp(38, 40)); more.setOnClickListener(v -> confirmDelete(task)); card.addView(top);
        space(card, 17);
        LinearLayout lower = row();
        LinearLayout streak = column(); TextView count = text("✦  " + task.streak(today), 21, task.color, true); streak.addView(count); streak.addView(text("DAY STREAK", 9, MUTED, true)); lower.addView(streak, new LinearLayout.LayoutParams(0, -2, 1));
        TextView action = text(done ? "✓  Done today" : scheduled ? "✓  Check in" : "Rest day  ☾", 13, done ? 0xFF5A967B : scheduled ? Color.WHITE : MUTED, true); action.setGravity(Gravity.CENTER); pad(action, 16, 0, 16, 0); action.setBackground(shape(done ? 0xFFE8F5EC : scheduled ? task.color : 0xFFF4F3F1, 14));
        action.setContentDescription(done ? task.name + " completed today" : scheduled ? "Check in " + task.name : task.name + " rest day");
        if (scheduled && !done) action.setOnClickListener(v -> { LocalDate now = LocalDate.now(); if (task.scheduled(now) && task.completions.add(now)) { save(); Toast.makeText(this, "Nice work! One day at a time ✨", Toast.LENGTH_SHORT).show(); } else { render(); } });
        lower.addView(action, lp(-2, 42)); card.addView(lower); space(card, 15);
        LinearLayout schedule = row();
        TextView scheduleLabel = text("SCHEDULE", 9, MUTED, true); scheduleLabel.setLetterSpacing(.08f); schedule.addView(scheduleLabel, new LinearLayout.LayoutParams(0, -2, 1));
        for (int i = 0; i < 7; i++) { TextView day = text(DAYS[i], 10, task.days[i] ? task.color : 0xFFC8C6CA, true); day.setGravity(Gravity.CENTER); day.setBackground(shape(task.days[i] ? blend(task.color, Color.WHITE, .90f) : 0xFFF5F4F2, 20)); day.setContentDescription(DAY_NAMES[i] + (task.days[i] ? " scheduled" : " rest day")); LinearLayout.LayoutParams p = lp(25, 25); p.leftMargin = dp(4); schedule.addView(day, p); }
        card.addView(schedule); return card;
    }
    private int blend(int a, int b, float amount) { return Color.rgb((int)(Color.red(a)*(1-amount)+Color.red(b)*amount), (int)(Color.green(a)*(1-amount)+Color.green(b)*amount), (int)(Color.blue(a)*(1-amount)+Color.blue(b)*amount)); }
    private void confirmDelete(Task task) {
        new AlertDialog.Builder(this).setTitle("Delete this habit?").setMessage("Your " + task.name + " streak and check-ins will be permanently removed.").setNegativeButton("Keep it", null).setPositiveButton("Delete", (d, w) -> { tasks.remove(task); save(); }).show();
    }
    private void showAddDialog() {
        LinearLayout form = column(); pad(form, 22, 8, 22, 4);
        TextView intro = text("Make a little space for what matters.", 13, MUTED, false); form.addView(intro); space(form, 19);
        TextView nameLabel = text("HABIT NAME", 11, INK, true); nameLabel.setLetterSpacing(.08f); form.addView(nameLabel); space(form, 6);
        EditText name = new EditText(this); name.setSingleLine(true); name.setTextSize(15); name.setHint("e.g. Workout, read, meditate"); name.setFilters(new InputFilter[]{new InputFilter.LengthFilter(50)}); pad(name, 14, 5, 14, 5); name.setBackground(outline(0xFFF8F7F5, 0xFFEAE8E4, 12)); form.addView(name, lp(-1, 48));
        space(form, 20); TextView scheduleLabel = text("CHOOSE YOUR DAYS", 11, INK, true); scheduleLabel.setLetterSpacing(.08f); form.addView(scheduleLabel); space(form, 4);
        TextView hint = text("Only these days count. The others are rest days.", 12, MUTED, false); form.addView(hint); space(form, 12);
        boolean[] selected = {true, true, true, true, true, false, false};
        LinearLayout daysRow = row();
        for (int i = 0; i < 7; i++) {
            final int index = i; TextView day = text(DAYS[i], 13, Color.WHITE, true); day.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(40), 1); if (i > 0) p.leftMargin = dp(5); daysRow.addView(day, p);
            Runnable update = () -> { day.setTextColor(selected[index] ? Color.WHITE : MUTED); day.setBackground(shape(selected[index] ? PURPLE : 0xFFF0EFED, 12)); day.setContentDescription(DAY_NAMES[index] + (selected[index] ? ", selected" : ", rest day")); };
            update.run(); day.setOnClickListener(v -> { selected[index] = !selected[index]; update.run(); });
        }
        form.addView(daysRow); space(form, 7); form.addView(text("Pick 1–7 days per week. You can check in once on each selected day.", 11, MUTED, false));
        space(form, 19); TextView colorLabel = text("PICK A COLOR", 11, INK, true); colorLabel.setLetterSpacing(.08f); form.addView(colorLabel); space(form, 10);
        LinearLayout colorRow = row(); final int[] colorChoice = {0}; List<TextView> swatches = new ArrayList<>();
        for (int i = 0; i < COLORS.length; i++) {
            final int index = i; TextView swatch = text(i == 0 ? "✓" : "", 17, Color.WHITE, true); swatch.setGravity(Gravity.CENTER); swatch.setBackground(shape(COLORS[i], 24)); swatch.setContentDescription("Color " + (i + 1));
            LinearLayout.LayoutParams p = lp(38, 38); if (i > 0) p.leftMargin = dp(12); colorRow.addView(swatch, p); swatches.add(swatch);
            swatch.setOnClickListener(v -> { colorChoice[0] = index; for (int j = 0; j < swatches.size(); j++) swatches.get(j).setText(j == index ? "✓" : ""); });
        }
        form.addView(colorRow); space(form, 6);
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true); scroll.addView(form);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("Create a habit ✨").setView(scroll).setNegativeButton("Cancel", null).setPositiveButton("Create habit", null).create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String value = name.getText().toString().trim();
            if (value.isEmpty()) { name.setError("Give your habit a name"); return; }
            boolean any = false; for (boolean day : selected) any |= day;
            if (!any) { Toast.makeText(this, "Choose at least one day", Toast.LENGTH_SHORT).show(); return; }
            tasks.add(Task.create(value, COLORS[colorChoice[0]], selected)); save(); dialog.dismiss();
        }));
        dialog.show();
    }
    private boolean notificationsEnabled() {
        return ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).areNotificationsEnabled();
    }
    private void enableNotifications() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED && !getPreferences(0).getBoolean("asked_notifications", false)) {
            getPreferences(0).edit().putBoolean("asked_notifications", true).apply();
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
        } else {
            Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
            try { startActivity(intent); } catch (Exception e) { startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName()))); }
        }
    }
}
