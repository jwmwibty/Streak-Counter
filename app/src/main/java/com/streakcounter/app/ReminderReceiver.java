package com.streakcounter.app;

import android.Manifest;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

public class ReminderReceiver extends BroadcastReceiver {
    private static final String CHANNEL = "streak_reminders";
    static void schedule(Context context) {
        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime next = now.withHour(20).withMinute(0).withSecond(0).withNano(0);
        if (!next.isAfter(now)) next = next.plusDays(1);
        Intent intent = new Intent(context, ReminderReceiver.class);
        PendingIntent pending = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        // Inexact alarms avoid requesting special exact-alarm access. Delivery can be delayed by Android.
        alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.toInstant().toEpochMilli(), pending);
    }
    @Override public void onReceive(Context context, Intent intent) {
        schedule(context);
        LocalDate today = LocalDate.now();
        List<String> atRisk = new ArrayList<>();
        for (Task task : TaskStore.load(context)) {
            if (task.scheduled(today) && !task.done(today) && task.streak(today) > 0) atRisk.add(task.name);
        }
        if (atRisk.isEmpty()) return;
        if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return;
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.createNotificationChannel(new NotificationChannel(CHANNEL, "Streak reminders", NotificationManager.IMPORTANCE_DEFAULT));
        Intent open = new Intent(context, MainActivity.class);
        PendingIntent content = PendingIntent.getActivity(context, 0, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        String message = atRisk.size() == 1 ? "Your " + atRisk.get(0) + " streak is waiting for you." : atRisk.size() + " streaks need a check-in today: " + String.join(", ", atRisk);
        Notification notification = new Notification.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification).setContentTitle("Don't lose your streak ✨")
            .setContentText(message).setStyle(new Notification.BigTextStyle().bigText(message))
            .setContentIntent(content).setAutoCancel(true).build();
        manager.notify(100, notification);
    }
}
