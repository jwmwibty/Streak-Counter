package com.streakcounter.app;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputFilter;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.animation.ValueAnimator;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int INK = Color.rgb(31, 35, 48);
    private static final int MUTED = Color.rgb(126, 130, 145);
    private static final int PURPLE = Color.rgb(111, 86, 222);
    private static final int BACKGROUND = Color.rgb(247, 247, 251);
    private static final int[] COLORS = {
            0xFF7359DE, 0xFFE78A66, 0xFF56A58E, 0xFF648BD6, 0xFFD9789A
    };
    private static final String[] DAYS = {"M", "T", "W", "T", "F", "S", "S"};
    private static final String[] DAY_NAMES = {
            "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"
    };

    private List<Task> tasks = new ArrayList<>();
    private LinearLayout root;

    private int dp(float value) {
        return (int) (getResources().getDisplayMetrics().density * value + .5f);
    }

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        getWindow().setStatusBarColor(BACKGROUND);
        getWindow().setNavigationBarColor(BACKGROUND);
        if (Build.VERSION.SDK_INT >= 26) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        }
        ReminderReceiver.schedule(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        tasks = TaskStore.load(this);
        render();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        render();
    }

    private GradientDrawable shape(int color, float radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radius));
        return drawable;
    }

    private GradientDrawable gradient(int[] colors, float radius) {
        GradientDrawable drawable = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR, colors);
        drawable.setCornerRadius(dp(radius));
        return drawable;
    }

    private GradientDrawable outline(int color, int stroke, float radius) {
        GradientDrawable drawable = shape(color, radius);
        drawable.setStroke(dp(1), stroke);
        return drawable;
    }

    private RippleDrawable ripple(int color, int rippleColor, float radius) {
        return new RippleDrawable(
                ColorStateList.valueOf(rippleColor), shape(color, radius), null);
    }

    private TextView text(String value, float size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setFontFeatureSettings("kern");
        view.setIncludeFontPadding(false);
        if (bold) view.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        return view;
    }

    private LinearLayout column() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        return layout;
    }

    private LinearLayout row() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setGravity(Gravity.CENTER_VERTICAL);
        return layout;
    }

    private LinearLayout.LayoutParams lp(int width, int height) {
        return new LinearLayout.LayoutParams(
                width < 0 ? width : dp(width), height < 0 ? height : dp(height));
    }

    private void pad(View view, int left, int top, int right, int bottom) {
        view.setPadding(dp(left), dp(top), dp(right), dp(bottom));
    }

    private void space(LinearLayout parent, int height) {
        parent.addView(new View(this), lp(1, height));
    }

    private TextView capsule(String value, int background, int foreground) {
        TextView view = text(value, 10, foreground, true);
        view.setGravity(Gravity.CENTER);
        view.setLetterSpacing(.07f);
        pad(view, 11, 7, 11, 7);
        view.setBackground(shape(background, 30));
        return view;
    }

    private void animateEntrance(View view, int order) {
        view.setAlpha(0f);
        view.setTranslationY(dp(14));
        view.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(order * 48L)
                .setDuration(420)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }

    private void addPressFeedback(View view) {
        view.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                v.animate().scaleX(.975f).scaleY(.975f).setDuration(90).start();
            } else if (event.getAction() == MotionEvent.ACTION_UP
                    || event.getAction() == MotionEvent.ACTION_CANCEL) {
                v.animate().scaleX(1f).scaleY(1f).setDuration(140).start();
            }
            return false;
        });
    }

    private void save() {
        TaskStore.save(this, tasks);
        render();
    }

    private void render() {
        LocalDate today = LocalDate.now();
        int due = 0;
        int done = 0;
        int best = 0;

        for (Task task : tasks) {
            if (task.scheduled(today)) {
                due++;
                if (task.done(today)) done++;
            }
            best = Math.max(best, task.streak(today));
        }

        root = column();
        root.setBackgroundColor(BACKGROUND);
        setContentView(root);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        scroll.setVerticalScrollBarEnabled(false);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        LinearLayout content = column();
        pad(content, 22, 22, 22, 24);
        scroll.addView(content);

        // Brand and date
        LinearLayout top = row();
        TextView mark = text("D", 19, Color.WHITE, true);
        mark.setGravity(Gravity.CENTER);
        mark.setBackground(gradient(new int[]{0xFF8069EF, 0xFF5F48C7}, 14));
        mark.setElevation(dp(3));
        top.addView(mark, lp(42, 42));

        TextView brand = text("daylight", 21, INK, true);
        LinearLayout.LayoutParams brandParams = lp(-2, -2);
        brandParams.leftMargin = dp(11);
        top.addView(brand, brandParams);

        View spacer = new View(this);
        top.addView(spacer, new LinearLayout.LayoutParams(0, 1, 1));

        TextView date = capsule(
                today.format(DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())
                        .withLocale(Locale.getDefault())).toUpperCase(Locale.getDefault()),
                0xFFECECF3, 0xFF6E7080);
        top.addView(date);
        content.addView(top);
        animateEntrance(top, 0);

        space(content, 30);
        TextView eyebrow = text(
                today.format(DateTimeFormatter.ofPattern("EEEE  /  MMMM d", Locale.getDefault()))
                        .toUpperCase(Locale.getDefault()),
                10, PURPLE, true);
        eyebrow.setLetterSpacing(.13f);
        content.addView(eyebrow);

        space(content, 10);
        TextView headline = text("Make today\ncount.", 35, INK, true);
        headline.setLineSpacing(dp(1), 1f);
        content.addView(headline);

        space(content, 8);
        content.addView(text(
                "Small habits. A steadier, stronger you.",
                14, MUTED, false));

        space(content, 25);

        // Daily progress card
        LinearLayout summary = column();
        pad(summary, 20, 20, 20, 19);
        summary.setBackground(gradient(
                new int[]{0xFF292A48, 0xFF48406F, 0xFF6654A4}, 25));
        summary.setElevation(dp(4));

        LinearLayout summaryHeader = row();
        TextView summaryLabel = text("DAILY OVERVIEW", 10, 0xFFD8D3F1, true);
        summaryLabel.setLetterSpacing(.14f);
        summaryHeader.addView(summaryLabel, new LinearLayout.LayoutParams(0, -2, 1));
        summaryHeader.addView(capsule(
                "YOUR PACE", 0xFF5D5486, 0xFFF2EFFF));
        summary.addView(summaryHeader);

        space(summary, 18);
        LinearLayout figures = row();
        TextView ratio = text(done + "/" + due, 39, Color.WHITE, true);
        figures.addView(ratio);

        LinearLayout figureCopy = column();
        LinearLayout.LayoutParams copyParams = new LinearLayout.LayoutParams(0, -2, 1);
        copyParams.leftMargin = dp(12);
        figureCopy.setGravity(Gravity.CENTER_VERTICAL);
        figures.addView(figureCopy, copyParams);
        figureCopy.addView(text("habits completed", 13, 0xFFE1DDF1, true));
        space(figureCopy, 4);
        figureCopy.addView(text(
                due == 0 ? "A clear space for today" :
                        (done == due ? "You showed up for yourself" : "Keep your momentum going"),
                11, 0xFFC5BFDE, false));
        summary.addView(figures);

        space(summary, 17);
        ProgressBar progress = new ProgressBar(
                this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(100);
        progress.setProgress(0);
        progress.setProgressTintList(ColorStateList.valueOf(0xFFD3C5FF));
        progress.setProgressBackgroundTintList(ColorStateList.valueOf(0xFF777096));
        summary.addView(progress, lp(-1, 7));

        int progressValue = due == 0 ? 0 : Math.min(100, done * 100 / due);
        progress.post(() -> {
            ValueAnimator animator = ValueAnimator.ofInt(0, progressValue);
            animator.setDuration(760);
            animator.setInterpolator(new DecelerateInterpolator());
            animator.addUpdateListener(a ->
                    progress.setProgress((Integer) a.getAnimatedValue()));
            animator.start();
        });

        space(summary, 16);
        LinearLayout stats = row();
        addSummaryStat(stats, "COMPLETED", done + " of " + due);
        View divider = new View(this);
        divider.setBackgroundColor(0x557D77A8);
        LinearLayout.LayoutParams dividerParams = lp(1, 32);
        dividerParams.leftMargin = dp(13);
        dividerParams.rightMargin = dp(13);
        stats.addView(divider, dividerParams);
        addSummaryStat(stats, "BEST CURRENT STREAK", best + (best == 1 ? " day" : " days"));
        summary.addView(stats);

        content.addView(summary);
        animateEntrance(summary, 1);
        space(content, 29);

        // Habit section heading
        LinearLayout sectionHeading = row();
        TextView habitsTitle = text("Your habits", 22, INK, true);
        sectionHeading.addView(habitsTitle, new LinearLayout.LayoutParams(0, -2, 1));
        sectionHeading.addView(capsule(
                tasks.size() + (tasks.size() == 1 ? " HABIT" : " HABITS"),
                0xFFECE9FB, PURPLE));
        content.addView(sectionHeading);
        space(content, 6);
        content.addView(text(
                "Show up for the things you care about.",
                13, MUTED, false));
        space(content, 17);

        if (tasks.isEmpty()) {
            LinearLayout empty = column();
            empty.setGravity(Gravity.CENTER);
            pad(empty, 22, 29, 22, 29);
            empty.setBackground(outline(Color.WHITE, 0xFFE9E9F0, 23));
            TextView initial = text("D", 22, PURPLE, true);
            initial.setGravity(Gravity.CENTER);
            initial.setBackground(shape(0xFFF0EDFF, 19));
            empty.addView(initial, lp(58, 58));
            space(empty, 15);
            TextView emptyTitle = text("Start with one small thing", 17, INK, true);
            emptyTitle.setGravity(Gravity.CENTER);
            empty.addView(emptyTitle);
            space(empty, 7);
            TextView emptyBody = text(
                    "Add a habit you want to make part of your everyday life.",
                    13, MUTED, false);
            emptyBody.setGravity(Gravity.CENTER);
            emptyBody.setLineSpacing(dp(3), 1f);
            empty.addView(emptyBody);
            content.addView(empty);
            animateEntrance(empty, 2);
        } else {
            int index = 2;
            for (Task task : tasks) {
                View card = taskCard(task, today);
                content.addView(card);
                animateEntrance(card, index++);
                space(content, 12);
            }
        }

        if (!notificationsEnabled()) {
            space(content, 7);
            LinearLayout notice = row();
            pad(notice, 15, 14, 15, 14);
            notice.setBackground(shape(0xFFEDEAFF, 16));
            LinearLayout noticeCopy = column();
            noticeCopy.addView(text("A gentle reminder", 13, PURPLE, true));
            space(noticeCopy, 4);
            noticeCopy.addView(text(
                    "Enable notifications for your evening check-in.",
                    11, 0xFF77718F, false));
            notice.addView(noticeCopy, new LinearLayout.LayoutParams(0, -2, 1));
            TextView enable = text("Enable", 12, PURPLE, true);
            enable.setGravity(Gravity.CENTER);
            notice.addView(enable);
            notice.setOnClickListener(v -> enableNotifications());
            addPressFeedback(notice);
            content.addView(notice);
        } else {
            space(content, 5);
            TextView reminder = text(
                    "EVENING REMINDERS ARE ON  ·  AROUND 8 PM",
                    9, MUTED, true);
            reminder.setGravity(Gravity.CENTER);
            reminder.setLetterSpacing(.08f);
            content.addView(reminder);
        }

        space(content, 4);
        TextView footerNote = text("Consistency over intensity.", 11, 0xFFA2A3B0, false);
        footerNote.setGravity(Gravity.CENTER);
        content.addView(footerNote);

        // Persistent add button
        LinearLayout footer = column();
        pad(footer, 22, 10, 22, 16);
        footer.setBackgroundColor(BACKGROUND);

        TextView add = text("+   Add a habit", 16, Color.WHITE, true);
        add.setGravity(Gravity.CENTER);
        add.setBackground(gradient(new int[]{0xFF8068EF, 0xFF624BCF}, 18));
        add.setElevation(dp(5));
        add.setOnClickListener(v -> showAddDialog());
        addPressFeedback(add);
        footer.addView(add, lp(-1, 56));
        root.addView(footer);
    }

    private void addSummaryStat(LinearLayout parent, String label, String value) {
        LinearLayout stat = column();
        stat.addView(text(label, 9, 0xFFBEB7D9, true));
        space(stat, 5);
        stat.addView(text(value, 14, Color.WHITE, true));
        parent.addView(stat, new LinearLayout.LayoutParams(0, -2, 1));
    }

    private View taskCard(Task task, LocalDate today) {
        boolean scheduled = task.scheduled(today);
        boolean done = task.done(today);

        LinearLayout card = column();
        pad(card, 16, 16, 16, 15);
        card.setBackground(outline(Color.WHITE, 0xFFE9E9F0, 21));
        card.setElevation(dp(1));

        LinearLayout top = row();
        TextView initial = text(
                task.name.substring(0, 1).toUpperCase(Locale.getDefault()),
                18, task.color, true);
        initial.setGravity(Gravity.CENTER);
        initial.setBackground(shape(blend(task.color, Color.WHITE, .88f), 15));
        top.addView(initial, lp(48, 48));

        LinearLayout info = column();
        LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(0, -2, 1);
        infoParams.leftMargin = dp(12);
        top.addView(info, infoParams);

        TextView name = text(task.name, 16, INK, true);
        name.setMaxLines(2);
        info.addView(name);
        space(info, 5);
        info.addView(text(
                task.daysPerWeek() + (task.daysPerWeek() == 1 ? " day" : " days")
                        + " per week  ·  "
                        + (scheduled ? (done ? "Completed today" : "Scheduled today") : "Rest day"),
                11, MUTED, false));

        TextView more = text("•••", 16, MUTED, true);
        more.setGravity(Gravity.CENTER);
        more.setContentDescription("Options for " + task.name);
        top.addView(more, lp(38, 40));
        more.setOnClickListener(v -> confirmDelete(task));
        card.addView(top);

        space(card, 17);
        View separator = new View(this);
        separator.setBackgroundColor(0xFFEFEFF4);
        card.addView(separator, lp(-1, 1));
        space(card, 13);

        LinearLayout lower = row();
        LinearLayout streakInfo = column();
        lower.addView(streakInfo, new LinearLayout.LayoutParams(0, -2, 1));

        TextView streakCount = text(
                String.valueOf(task.streak(today)),
                23, task.color, true);
        streakInfo.addView(streakCount);
        space(streakInfo, 3);
        streakInfo.addView(text("DAY STREAK", 9, MUTED, true));

        TextView action = text(
                done ? "Completed" : scheduled ? "Check in" : "Rest day",
                13,
                done ? 0xFF43836C : scheduled ? Color.WHITE : MUTED,
                true);
        action.setGravity(Gravity.CENTER);
        pad(action, 17, 0, 17, 0);
        action.setBackground(ripple(
                done ? 0xFFE7F4EC : scheduled ? task.color : 0xFFF1F1F4,
                done ? 0xFFD4EBDD : 0x44726AD0, 14));
        action.setContentDescription(
                done ? task.name + " completed today" :
                        scheduled ? "Check in " + task.name : task.name + " rest day");
        if (scheduled && !done) {
            action.setOnClickListener(v -> {
                LocalDate now = LocalDate.now();
                if (task.scheduled(now) && task.completions.add(now)) {
                    save();
                    Toast.makeText(this, "Habit checked in.", Toast.LENGTH_SHORT).show();
                } else {
                    render();
                }
            });
            addPressFeedback(action);
        }
        lower.addView(action, lp(-2, 43));
        card.addView(lower);

        space(card, 16);
        LinearLayout schedule = row();
        TextView scheduleLabel = text("WEEKLY PLAN", 9, MUTED, true);
        scheduleLabel.setLetterSpacing(.08f);
        schedule.addView(scheduleLabel, new LinearLayout.LayoutParams(0, -2, 1));

        for (int i = 0; i < 7; i++) {
            TextView day = text(DAYS[i], 10,
                    task.days[i] ? task.color : 0xFFB5B6C0, true);
            day.setGravity(Gravity.CENTER);
            day.setBackground(shape(
                    task.days[i] ? blend(task.color, Color.WHITE, .90f) : 0xFFF4F4F7,
                    9));
            day.setContentDescription(
                    DAY_NAMES[i] + (task.days[i] ? " scheduled" : " rest day"));
            LinearLayout.LayoutParams dayParams = lp(25, 25);
            dayParams.leftMargin = dp(4);
            schedule.addView(day, dayParams);
        }

        card.addView(schedule);
        addPressFeedback(card);
        return card;
    }

    private int blend(int first, int second, float amount) {
        return Color.rgb(
                (int) (Color.red(first) * (1 - amount) + Color.red(second) * amount),
                (int) (Color.green(first) * (1 - amount) + Color.green(second) * amount),
                (int) (Color.blue(first) * (1 - amount) + Color.blue(second) * amount));
    }

    private void confirmDelete(Task task) {
        new AlertDialog.Builder(this)
                .setTitle("Delete this habit?")
                .setMessage("Your " + task.name
                        + " streak and check-ins will be permanently removed.")
                .setNegativeButton("Keep habit", null)
                .setPositiveButton("Delete", (dialog, which) -> {
                    tasks.remove(task);
                    save();
                })
                .show();
    }

    private void showAddDialog() {
        LinearLayout form = column();
        pad(form, 22, 8, 22, 4);

        form.addView(text("Make a little space for what matters.", 13, MUTED, false));
        space(form, 19);

        TextView nameLabel = text("HABIT NAME", 10, INK, true);
        nameLabel.setLetterSpacing(.1f);
        form.addView(nameLabel);
        space(form, 7);

        EditText name = new EditText(this);
        name.setSingleLine(true);
        name.setTextSize(15);
        name.setHint("e.g. Workout, read, meditate");
        name.setFilters(new InputFilter[]{new InputFilter.LengthFilter(50)});
        pad(name, 14, 5, 14, 5);
        name.setBackground(outline(0xFFF8F8FB, 0xFFE7E7EF, 13));
        form.addView(name, lp(-1, 49));

        space(form, 20);
        TextView scheduleLabel = text("CHOOSE YOUR DAYS", 10, INK, true);
        scheduleLabel.setLetterSpacing(.1f);
        form.addView(scheduleLabel);
        space(form, 5);
        form.addView(text(
                "Only selected days count. The others are rest days.",
                12, MUTED, false));
        space(form, 13);

        boolean[] selected = {true, true, true, true, true, false, false};
        LinearLayout daysRow = row();

        for (int i = 0; i < 7; i++) {
            final int index = i;
            TextView day = text(DAYS[i], 13, Color.WHITE, true);
            day.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams params =
                    new LinearLayout.LayoutParams(0, dp(40), 1);
            if (i > 0) params.leftMargin = dp(5);
            daysRow.addView(day, params);

            Runnable update = () -> {
                day.setTextColor(selected[index] ? Color.WHITE : MUTED);
                day.setBackground(shape(
                        selected[index] ? PURPLE : 0xFFF0F0F4, 12));
                day.setContentDescription(
                        DAY_NAMES[index] + (selected[index] ? ", selected" : ", rest day"));
            };
            update.run();
            day.setOnClickListener(v -> {
                selected[index] = !selected[index];
                update.run();
            });
        }

        form.addView(daysRow);
        space(form, 8);
        form.addView(text(
                "Pick between 1 and 7 days each week.",
                11, MUTED, false));

        space(form, 20);
        TextView colorLabel = text("PICK A COLOR", 10, INK, true);
        colorLabel.setLetterSpacing(.1f);
        form.addView(colorLabel);
        space(form, 11);

        LinearLayout colorRow = row();
        final int[] colorChoice = {0};
        List<View> swatches = new ArrayList<>();

        for (int i = 0; i < COLORS.length; i++) {
            final int index = i;
            View swatch = new View(this);
            GradientDrawable selectedDrawable = shape(COLORS[i], 24);
            if (i == 0) selectedDrawable.setStroke(dp(3), 0xFF292A48);
            swatch.setBackground(selectedDrawable);
            swatch.setContentDescription("Color " + (i + 1));
            LinearLayout.LayoutParams params = lp(37, 37);
            if (i > 0) params.leftMargin = dp(13);
            colorRow.addView(swatch, params);
            swatches.add(swatch);

            swatch.setOnClickListener(v -> {
                colorChoice[0] = index;
                for (int j = 0; j < swatches.size(); j++) {
                    GradientDrawable background = shape(COLORS[j], 24);
                    if (j == index) background.setStroke(dp(3), 0xFF292A48);
                    swatches.get(j).setBackground(background);
                }
            });
        }

        form.addView(colorRow);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.addView(form);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Create a habit")
                .setView(scroll)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Create habit", null)
                .create();

        dialog.setOnShowListener(ignored -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(PURPLE);
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(MUTED);
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String value = name.getText().toString().trim();
                if (value.isEmpty()) {
                    name.setError("Give your habit a name");
                    return;
                }

                boolean any = false;
                for (boolean day : selected) any |= day;
                if (!any) {
                    Toast.makeText(this, "Choose at least one day.",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                tasks.add(Task.create(value, COLORS[colorChoice[0]], selected));
                save();
                dialog.dismiss();
            });
        });
        dialog.show();
    }

    private boolean notificationsEnabled() {
        return ((NotificationManager) getSystemService(NOTIFICATION_SERVICE))
                .areNotificationsEnabled();
    }

    private void enableNotifications() {
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
                && !getPreferences(0).getBoolean("asked_notifications", false)) {
            getPreferences(0).edit().putBoolean("asked_notifications", true).apply();
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
        } else {
            Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
            try {
                startActivity(intent);
            } catch (Exception error) {
                startActivity(new Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:" + getPackageName())));
            }
        }
    }
}
