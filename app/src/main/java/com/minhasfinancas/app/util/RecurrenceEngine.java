package com.minhasfinancas.app.util;

import com.minhasfinancas.app.data.entity.RecurrenceEntity;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public final class RecurrenceEngine {

    private RecurrenceEngine() {
    }

    public static List<LocalDate> generateDates(RecurrenceEntity recurrence, LocalDate rangeStart, LocalDate rangeEnd) {
        List<LocalDate> results = new ArrayList<>();
        if (recurrence == null || recurrence.startDate == null || recurrence.startDate.trim().isEmpty()) {
            return results;
        }

        LocalDate current = LocalDate.parse(recurrence.startDate);
        LocalDate endBoundary = recurrence.endDate != null && !recurrence.endDate.trim().isEmpty()
                ? LocalDate.parse(recurrence.endDate)
                : rangeEnd;
        if (endBoundary.isAfter(rangeEnd)) {
            endBoundary = rangeEnd;
        }

        int count = 0;
        int max = recurrence.maxOccurrences != null ? recurrence.maxOccurrences : Integer.MAX_VALUE;

        while (!current.isAfter(endBoundary) && count < max && count < 500) {
            if (!current.isBefore(rangeStart) && !current.isAfter(rangeEnd)) {
                results.add(current);
            }
            current = next(current, recurrence);
            count++;
        }
        return results;
    }

    private static LocalDate next(LocalDate date, RecurrenceEntity recurrence) {
        int interval = recurrence.intervalValue <= 0 ? 1 : recurrence.intervalValue;
        String frequency = recurrence.frequency == null ? "MONTHLY" : recurrence.frequency;
        switch (frequency) {
            case "WEEKLY":
                return date.plusWeeks(interval);
            case "BIWEEKLY":
                return date.plusWeeks(2L * interval);
            case "YEARLY":
                return date.plusYears(interval);
            case "CUSTOM_DAYS":
                return date.plusDays(interval);
            case "INSTALLMENT":
                return date.plusMonths(interval);
            case "MONTHLY":
            default:
                return date.plusMonths(interval);
        }
    }
}
