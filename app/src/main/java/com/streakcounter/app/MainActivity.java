package com.streakcounter.app;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
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
import android.widget.FrameLayout;
import android.widget.ImageView;
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
    private static final int BACKGROUND = Color.rgb(249, 246, 239);
    private static final int[] COLORS = {
            0xFF7359DE, 0xFFE17C55, 0xFF3C9B7F, 0xFF4779C8, 0xFFD65D83
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
        scroll.setFocusableInTouchMode(true);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        LinearLayout content = column();
        content.setFocusableInTouchMode(true);
        pad(content, 22, 22, 22, 24);
        scroll.addView(content);
        // Prevent focus restoration from leaving the brand mark clipped off-screen.
        scroll.post(() -> {
            scroll.scrollTo(0, 0);
            content.requestFocus();
        });

        // Brand and date
        LinearLayout top = row();
        ImageView mark = new ImageView(this);
        mark.setImageResource(R.drawable.ic_app);
        mark.setScaleType(ImageView.ScaleType.FIT_CENTER);
        mark.setElevation(dp(3));
        top.addView(mark, lp(46, 46));

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
            ImageView initial = new ImageView(this);
            initial.setImageResource(R.drawable.ic_app);
            initial.setScaleType(ImageView.ScaleType.FIT_CENTER);
            empty.addView(initial, lp(62, 62));
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
        add.setBackground(gradient(new int[]{0xFFE98A60, 0xFFD95D65}, 18));
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
        card.setBackground(outline(Color.WHITE, 0xFFEAE4D9, 21));
        card.setElevation(dp(1));

        LinearLayout top = row();
        View initial = new IconArt(iconFor(task.name), task.color,
                blend(task.color, Color.WHITE, .88f));
        top.addView(initial, lp(54, 54));

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
        separator.setBackgroundColor(0xFFF0ECE3);
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
                    boolean allDone = true;
                    for (Task item : tasks) {
                        if (item.scheduled(now) && !item.done(now)) {
                            allDone = false;
                            break;
                        }
                    }
                    showStreakCelebration(task, task.streak(now), allDone);
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
                    task.days[i] ? blend(task.color, Color.WHITE, .90f) : 0xFFF5F1E9,
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


    private String iconFor(String habitName) {
        String name = habitName.toLowerCase(Locale.ROOT);
        if (name.contains("workout") || name.contains("gym") || name.contains("exercise")
                || name.contains("strength") || name.contains("lift")) return "dumbbell";
        if (name.contains("read") || name.contains("book")) return "book";
        if (name.contains("water") || name.contains("drink") || name.contains("hydrat")) return "water";
        if (name.contains("meditat") || name.contains("yoga") || name.contains("breath")) return "meditate";
        if (name.contains("walk") || name.contains("run") || name.contains("jog")) return "walk";
        if (name.contains("sleep") || name.contains("rest")) return "moon";
        if (name.contains("study") || name.contains("learn") || name.contains("class")) return "study";
        if (name.contains("write") || name.contains("journal") || name.contains("diary")) return "write";
        if (name.contains("music") || name.contains("guitar") || name.contains("piano")) return "music";
        return "leaf";
    }

    /** Small original vector illustrations. Drawn locally, so the UI needs no image downloads. */
    private class IconArt extends View {
        private final String kind;
        private final int tint;
        private final int tile;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        IconArt(String kind, int tint, int tile) {
            super(MainActivity.this);
            this.kind = kind;
            this.tint = tint;
            this.tile = tile;
            setContentDescription(kind.equals("sun") ? "Daylight" : kind + " habit");
        }

        private void line(Canvas canvas, float x1, float y1, float x2, float y2) {
            canvas.drawLine(x1, y1, x2, y2, paint);
        }

        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float w = getWidth(), h = getHeight();
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(tile);
            canvas.drawRoundRect(new RectF(0, 0, w, h), dp(17), dp(17), paint);

            float unit = Math.min(w, h) * .62f;
            canvas.save();
            canvas.translate((w - unit) / 2f, (h - unit) / 2f);
            canvas.scale(unit / 100f, unit / 100f);

            paint.setColor(tint);
            paint.setStrokeWidth(5.5f);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeJoin(Paint.Join.ROUND);
            paint.setStyle(Paint.Style.STROKE);

            Path path = new Path();
            switch (kind) {
                case "sun":
                    paint.setStyle(Paint.Style.FILL);
                    canvas.drawCircle(50, 50, 15, paint);
                    paint.setStyle(Paint.Style.STROKE);
                    for (int i = 0; i < 8; i++) {
                        double angle = Math.PI * i / 4.0;
                        float x1 = 50 + (float)Math.cos(angle) * 25;
                        float y1 = 50 + (float)Math.sin(angle) * 25;
                        float x2 = 50 + (float)Math.cos(angle) * 36;
                        float y2 = 50 + (float)Math.sin(angle) * 36;
                        line(canvas, x1, y1, x2, y2);
                    }
                    break;
                case "dumbbell":
                    line(canvas, 20, 50, 80, 50);
                    canvas.drawRoundRect(new RectF(12, 34, 25, 66), 4, 4, paint);
                    canvas.drawRoundRect(new RectF(75, 34, 88, 66), 4, 4, paint);
                    line(canvas, 29, 39, 29, 61);
                    line(canvas, 71, 39, 71, 61);
                    break;
                case "book":
                    path.moveTo(50, 32); path.quadTo(30, 20, 12, 30);
                    path.lineTo(12, 73); path.quadTo(32, 64, 50, 77);
                    path.quadTo(68, 64, 88, 73); path.lineTo(88, 30);
                    path.quadTo(70, 20, 50, 32); path.close();
                    canvas.drawPath(path, paint);
                    line(canvas, 50, 33, 50, 76);
                    line(canvas, 21, 41, 38, 44);
                    line(canvas, 62, 44, 79, 41);
                    break;
                case "water":
                    path.moveTo(50, 12); path.cubicTo(42, 27, 23, 47, 23, 61);
                    path.cubicTo(23, 79, 35, 89, 50, 89);
                    path.cubicTo(65, 89, 77, 79, 77, 61);
                    path.cubicTo(77, 47, 58, 27, 50, 12);
                    canvas.drawPath(path, paint);
                    path.reset(); path.moveTo(37, 64); path.quadTo(39, 77, 51, 78);
                    canvas.drawPath(path, paint);
                    break;
                case "meditate":
                    paint.setStyle(Paint.Style.FILL);
                    canvas.drawCircle(50, 23, 9, paint);
                    paint.setStyle(Paint.Style.STROKE);
                    path.moveTo(50, 36); path.lineTo(50, 58);
                    path.moveTo(50, 43); path.quadTo(32, 43, 26, 55);
                    path.moveTo(50, 43); path.quadTo(68, 43, 74, 55);
                    path.moveTo(50, 58); path.quadTo(36, 65, 24, 77);
                    path.quadTo(50, 88, 76, 77);
                    path.quadTo(64, 65, 50, 58);
                    canvas.drawPath(path, paint);
                    break;
                case "walk":
                    paint.setStyle(Paint.Style.FILL);
                    canvas.drawCircle(60, 20, 8, paint);
                    paint.setStyle(Paint.Style.STROKE);
                    line(canvas, 56, 31, 45, 54);
                    line(canvas, 51, 40, 32, 49);
                    line(canvas, 51, 40, 70, 48);
                    line(canvas, 45, 54, 27, 76);
                    line(canvas, 45, 54, 66, 73);
                    break;
                case "moon":
                    path.moveTo(67, 19);
                    path.cubicTo(38, 21, 26, 54, 43, 74);
                    path.cubicTo(59, 93, 84, 79, 87, 59);
                    path.cubicTo(72, 68, 48, 54, 50, 36);
                    path.cubicTo(51, 28, 57, 22, 67, 19);
                    canvas.drawPath(path, paint);
                    break;
                case "study":
                    path.moveTo(9, 42); path.lineTo(50, 22); path.lineTo(91, 42);
                    path.lineTo(50, 62); path.close(); canvas.drawPath(path, paint);
                    line(canvas, 25, 51, 25, 70);
                    path.reset(); path.moveTo(25, 70); path.quadTo(50, 85, 75, 70);
                    canvas.drawPath(path, paint);
                    line(canvas, 91, 42, 91, 67);
                    paint.setStyle(Paint.Style.FILL); canvas.drawCircle(91, 72, 4, paint);
                    break;
                case "write":
                    path.moveTo(27, 72); path.lineTo(32, 54); path.lineTo(69, 17);
                    path.quadTo(75, 12, 81, 19); path.lineTo(84, 22);
                    path.quadTo(89, 28, 82, 34); path.lineTo(45, 70);
                    path.close(); canvas.drawPath(path, paint);
                    line(canvas, 33, 55, 44, 66);
                    line(canvas, 22, 79, 49, 79);
                    break;
                case "music":
                    line(canvas, 63, 20, 63, 67);
                    line(canvas, 63, 20, 83, 15);
                    line(canvas, 83, 15, 83, 58);
                    paint.setStyle(Paint.Style.FILL);
                    canvas.drawCircle(49, 69, 10, paint);
                    canvas.drawCircle(69, 60, 10, paint);
                    break;
                default: // Leaf
                    path.moveTo(18, 77);
                    path.cubicTo(16, 37, 43, 13, 83, 20);
                    path.cubicTo(88, 57, 62, 84, 18, 77);
                    canvas.drawPath(path, paint);
                    line(canvas, 24, 75, 69, 31);
                    line(canvas, 44, 56, 42, 37);
                    line(canvas, 52, 49, 72, 51);
                    break;
            }
            canvas.restore();
        }
    }


    private void showStreakCelebration(Task task, int streak, boolean allDoneToday) {
        Dialog dialog = new Dialog(this);
        LinearLayout screen = column();
        screen.setGravity(Gravity.CENTER);
        pad(screen, 28, 30, 28, 28);
        screen.setBackgroundColor(0xFF101C1E);

        TextView eyebrow = text("DAYLIGHT  /  STREAK UNLOCKED", 10, 0xFF829496, true);
        eyebrow.setGravity(Gravity.CENTER);
        eyebrow.setLetterSpacing(.17f);
        screen.addView(eyebrow);

        space(screen, 24);
        FlameCelebrationView fire = new FlameCelebrationView();
        screen.addView(fire, lp(190, 178));

        space(screen, 2);
        TextView count = text(String.valueOf(Math.max(0, streak - 1)), 48, 0xFFFF684F, true);
        count.setGravity(Gravity.CENTER);
        count.setIncludeFontPadding(false);
        screen.addView(count);

        TextView unit = text("DAY STREAK", 13, 0xFFFF7657, true);
        unit.setGravity(Gravity.CENTER);
        unit.setLetterSpacing(.1f);
        screen.addView(unit);

        space(screen, 29);
        LinearLayout week = row();
        week.setGravity(Gravity.CENTER);
        LocalDate today = LocalDate.now();
        LocalDate monday = today.with(java.time.temporal.TemporalAdjusters.previousOrSame(
                java.time.DayOfWeek.MONDAY));
        String[] labels = {"M", "T", "W", "T", "F", "S", "S"};
        for (int i = 0; i < 7; i++) {
            LocalDate date = monday.plusDays(i);
            LinearLayout dayCell = column();
            dayCell.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams cellParams = new LinearLayout.LayoutParams(0, -2, 1);
            week.addView(dayCell, cellParams);

            TextView dayLabel = text(labels[i], 9,
                    date.equals(today) ? 0xFFFF7657 : 0xFF718184, true);
            dayLabel.setGravity(Gravity.CENTER);
            dayCell.addView(dayLabel);

            space(dayCell, 7);
            boolean scheduled = task.scheduled(date);
            boolean completed = task.done(date);
            TextView mark = text(completed ? "✓" : scheduled ? "·" : "–",
                    15, completed ? 0xFF142021 : 0xFF65777A, true);
            mark.setGravity(Gravity.CENTER);
            mark.setBackground(shape(completed ? 0xFFFF805C :
                    (date.equals(today) ? 0xFF263638 : 0xFF1A292B), 10));
            dayCell.addView(mark, lp(30, 30));
        }
        screen.addView(week);

        space(screen, 19);
        TextView message = text(
                allDoneToday ? "You reached a perfect day!" :
                        task.name + " is another promise kept.",
                13, 0xFFE0E8E5, true);
        message.setGravity(Gravity.CENTER);
        screen.addView(message);

        space(screen, 5);
        TextView sub = text(
                allDoneToday ? "Every habit checked. Keep the fire alive." :
                        "One small win today. Come back tomorrow.",
                11, 0xFF8DA0A0, false);
        sub.setGravity(Gravity.CENTER);
        screen.addView(sub);

        View flexible = new View(this);
        screen.addView(flexible, new LinearLayout.LayoutParams(1, 0, 1));

        TextView button = text("LET'S KEEP GOING", 12, 0xFF142021, true);
        button.setGravity(Gravity.CENTER);
        button.setLetterSpacing(.08f);
        button.setBackground(gradient(new int[]{0xFFFF9A55, 0xFFFF604F}, 13));
        screen.addView(button, lp(-1, 49));
        button.setOnClickListener(v -> dialog.dismiss());

        dialog.setContentView(screen);
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setLayout(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT);
            dialog.getWindow().setStatusBarColor(0xFF101C1E);
            dialog.getWindow().setNavigationBarColor(0xFF101C1E);
            dialog.getWindow().setDimAmount(0f);
        }
        fire.startBurst();
        ValueAnimator number = ValueAnimator.ofInt(Math.max(0, streak - 1), streak);
        number.setStartDelay(520);
        number.setDuration(520);
        number.setInterpolator(new DecelerateInterpolator());
        number.addUpdateListener(anim -> count.setText(
                String.valueOf((Integer) anim.getAnimatedValue())));
        number.start();
        count.setScaleX(.72f);
        count.setScaleY(.72f);
        count.animate().scaleX(1f).scaleY(1f).setStartDelay(520).setDuration(520).start();
    }

    /** Full-screen flame celebration, drawn locally with a short burst of sparks. */
    private class FlameCelebrationView extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private float progress;
        private ValueAnimator animator;

        FlameCelebrationView() {
            super(MainActivity.this);
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }

        void startBurst() {
            animator = ValueAnimator.ofFloat(0f, 1f);
            animator.setDuration(1750);
            animator.setInterpolator(new DecelerateInterpolator());
            animator.addUpdateListener(a -> {
                progress = (Float) a.getAnimatedValue();
                invalidate();
            });
            animator.start();
        }

        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float cx = getWidth() / 2f;
            float cy = getHeight() * .78f;
            float appear = Math.min(1f, progress * 2.3f);
            float pulse = 1f + (float)Math.sin(progress * Math.PI * 5) * .035f;
            float alpha = Math.max(.18f, 1f - Math.max(0f, progress - .35f) * .7f);

            canvas.save();
            canvas.translate(cx, cy);
            canvas.scale((.58f + appear * .42f) * pulse,
                    (.58f + appear * .42f) * pulse);

            p.setStyle(Paint.Style.FILL);
            p.setColor(Color.argb((int)(alpha * 45), 255, 75, 48));
            canvas.drawCircle(0, -45, 70, p);

            p.setColor(0xFFEF263D);
            Path red = new Path();
            red.moveTo(0, -119);
            red.cubicTo(-23, -91, -61, -69, -54, -28);
            red.cubicTo(-50, 10, -24, 28, 0, 28);
            red.cubicTo(36, 28, 59, 0, 53, -36);
            red.cubicTo(49, -68, 15, -91, 0, -119);
            canvas.drawPath(red, p);

            p.setColor(0xFFFF713C);
            Path orange = new Path();
            orange.moveTo(0, -102);
            orange.cubicTo(-13, -76, -43, -53, -38, -22);
            orange.cubicTo(-34, 8, -14, 20, 1, 20);
            orange.cubicTo(28, 18, 42, -5, 35, -32);
            orange.cubicTo(31, -56, 9, -78, 0, -102);
            canvas.drawPath(orange, p);

            p.setColor(0xFFFFC34F);
            Path gold = new Path();
            gold.moveTo(0, -78);
            gold.cubicTo(-8, -57, -25, -38, -21, -15);
            gold.cubicTo(-18, 7, -7, 13, 1, 13);
            gold.cubicTo(19, 10, 25, -8, 17, -29);
            gold.cubicTo(13, -48, 4, -63, 0, -78);
            canvas.drawPath(gold, p);

            p.setColor(0xFFFFF4D8);
            canvas.drawOval(new RectF(-11, -36, 11, 5), p);
            p.setColor(0xFFFFFFFF);
            canvas.drawOval(new RectF(-7, -29, 7, -2), p);
            canvas.restore();

            float sparks = progress < .65f ? progress / .65f : 1f;
            float fade = progress < .65f ? 1f : 1f - (progress - .65f) / .35f;
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeCap(Paint.Cap.ROUND);
            for (int i = 0; i < 16; i++) {
                double angle = Math.PI * 2 * i / 16.0 - Math.PI / 2;
                float distance = dp(22 + sparks * (i % 3 == 0 ? 76 : 54));
                float sx = cx + (float)Math.cos(angle) * distance;
                float sy = cy - dp(43) + (float)Math.sin(angle) * distance;
                int color = i % 3 == 0 ? 0xFFFFD36B :
                        i % 3 == 1 ? 0xFFFF684F : 0xFFFFA15C;
                p.setColor(Color.argb((int)(fade * 255), Color.red(color),
                        Color.green(color), Color.blue(color)));
                p.setStrokeWidth(dp(i % 3 == 0 ? 3 : 2));
                float length = dp(i % 2 == 0 ? 8 : 5);
                canvas.drawLine(sx, sy, sx + (float)Math.cos(angle) * length,
                        sy + (float)Math.sin(angle) * length, p);
            }
            p.setStyle(Paint.Style.FILL);
        }

        @Override protected void onDetachedFromWindow() {
            if (animator != null) animator.cancel();
            super.onDetachedFromWindow();
        }
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
