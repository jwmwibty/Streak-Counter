package com.streakcounter.app;

import org.junit.Test;
import java.time.LocalDate;
import static org.junit.Assert.*;

public class TaskTest {
    private Task weekdays() {
        Task t = new Task();
        t.created = LocalDate.parse("2026-11-16"); // Monday
        t.days = new boolean[]{true, true, true, true, true, false, false};
        return t;
    }
    @Test public void todayHasUntilMidnight() {
        Task t = weekdays();
        t.completions.add(LocalDate.parse("2026-11-16"));
        assertEquals(1, t.streak(LocalDate.parse("2026-11-17")));
        assertEquals(0, t.streak(LocalDate.parse("2026-11-18")));
    }
    @Test public void restDaysDoNotBreakStreak() {
        Task t = weekdays();
        for (int day = 16; day <= 20; day++) t.completions.add(LocalDate.of(2026, 11, day));
        assertEquals(5, t.streak(LocalDate.parse("2026-11-22"))); // Sunday
        assertEquals(5, t.streak(LocalDate.parse("2026-11-23"))); // Monday still open
        t.completions.add(LocalDate.parse("2026-11-23"));
        assertEquals(6, t.streak(LocalDate.parse("2026-11-23")));
    }
    @Test public void missedScheduledDayResetsAndNextCheckinStartsAgain() {
        Task t = weekdays();
        t.completions.add(LocalDate.parse("2026-11-16"));
        t.completions.add(LocalDate.parse("2026-11-18"));
        assertEquals(1, t.streak(LocalDate.parse("2026-11-18")));
        assertEquals(0, t.streak(LocalDate.parse("2026-11-20")));
    }
    @Test public void duplicateCheckinsOnlyCountOnce() {
        Task t = weekdays();
        LocalDate monday = LocalDate.parse("2026-11-16");
        assertTrue(t.completions.add(monday));
        assertFalse(t.completions.add(monday));
        assertEquals(1, t.streak(monday));
    }
    @Test public void daysBeforeCreationAreNotMisses() {
        Task t = weekdays();
        assertEquals(0, t.streak(LocalDate.parse("2026-11-16")));
        t.completions.add(t.created);
        assertEquals(1, t.streak(t.created));
    }
    @Test public void sixDayScheduleStillSkipsItsRestDay() {
        Task t = weekdays(); t.days[5] = true;
        for (int day = 16; day <= 21; day++) t.completions.add(LocalDate.of(2026, 11, day));
        assertEquals(6, t.streak(LocalDate.parse("2026-11-22")));
    }
}
