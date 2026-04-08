package com.minhasfinancas.app.util;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

public final class DateUtils {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter HUMAN = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MMMM 'de' yyyy", new Locale("pt", "BR"));

    private DateUtils() {}

    public static String todayIso() {
        return LocalDate.now().format(ISO);
    }

    public static String todayDisplay() {
        return LocalDate.now().format(HUMAN);
    }

    public static String monthStartIso() {
        return monthStartIso(LocalDate.now());
    }

    public static String monthEndIso() {
        return monthEndIso(LocalDate.now());
    }

    public static String monthStartIso(LocalDate date) {
        return date.withDayOfMonth(1).format(ISO);
    }

    public static String monthEndIso(LocalDate date) {
        LocalDate value = date.withDayOfMonth(date.lengthOfMonth());
        return value.format(ISO);
    }

    public static LocalDate parseIsoOrToday(String isoDate) {
        if (isoDate == null || isoDate.trim().isEmpty()) {
            return LocalDate.now();
        }
        try {
            return LocalDate.parse(isoDate, ISO);
        } catch (DateTimeParseException ex) {
            return LocalDate.now();
        }
    }

    public static LocalDate parseDisplayOrIsoOrToday(String value) {
        if (value == null || value.trim().isEmpty()) {
            return LocalDate.now();
        }
        try {
            return LocalDate.parse(value, HUMAN);
        } catch (DateTimeParseException ex) {
            try {
                return LocalDate.parse(value, ISO);
            } catch (DateTimeParseException ignored) {
                return LocalDate.now();
            }
        }
    }

    public static String displayToIso(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "";
        }
        try {
            return LocalDate.parse(value, HUMAN).format(ISO);
        } catch (DateTimeParseException ex) {
            try {
                return LocalDate.parse(value, ISO).format(ISO);
            } catch (DateTimeParseException ignored) {
                return value;
            }
        }
    }

    public static String isoToDisplay(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "";
        }
        try {
            return LocalDate.parse(value, ISO).format(HUMAN);
        } catch (DateTimeParseException ex) {
            try {
                return LocalDate.parse(value, HUMAN).format(HUMAN);
            } catch (DateTimeParseException ignored) {
                return value;
            }
        }
    }

    public static String formatIsoToHuman(String isoDate) {
        if (isoDate == null || isoDate.trim().isEmpty()) {
            return "-";
        }
        try {
            return LocalDate.parse(isoDate, ISO).format(HUMAN);
        } catch (DateTimeParseException ex) {
            return isoDate;
        }
    }

    public static String formatMonthYear(String isoDate) {
        try {
            return capitalize(LocalDate.parse(isoDate, ISO).format(MONTH));
        } catch (DateTimeParseException ex) {
            return isoDate;
        }
    }

    public static String formatMonthYear(LocalDate date) {
        return capitalize(date.format(MONTH));
    }

    public static String currency(double value) {
        NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
        return format.format(value);
    }

    private static String capitalize(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        return value.substring(0, 1).toUpperCase(new Locale("pt", "BR")) + value.substring(1);
    }
}
