package de.elyseeevents.portal.util;

import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class BavarianHolidayUtil {

    public Map<LocalDate, String> getHolidaysForYear(int year) {
        Map<LocalDate, String> holidays = new LinkedHashMap<>();

        // Feste Feiertage
        holidays.put(LocalDate.of(year, 1, 1), "Neujahr");
        holidays.put(LocalDate.of(year, 1, 6), "Heilige Drei Könige");
        holidays.put(LocalDate.of(year, 5, 1), "Tag der Arbeit");
        holidays.put(LocalDate.of(year, 8, 8), "Friedensfest Augsburg");
        holidays.put(LocalDate.of(year, 8, 15), "Mariä Himmelfahrt");
        holidays.put(LocalDate.of(year, 10, 3), "Tag der Deutschen Einheit");
        holidays.put(LocalDate.of(year, 11, 1), "Allerheiligen");
        holidays.put(LocalDate.of(year, 12, 25), "1. Weihnachtstag");
        holidays.put(LocalDate.of(year, 12, 26), "2. Weihnachtstag");

        // Bewegliche Feiertage (basierend auf Ostern)
        LocalDate easter = computeEaster(year);
        holidays.put(easter.minusDays(2), "Karfreitag");
        holidays.put(easter.plusDays(1), "Ostermontag");
        holidays.put(easter.plusDays(39), "Christi Himmelfahrt");
        holidays.put(easter.plusDays(50), "Pfingstmontag");
        holidays.put(easter.plusDays(60), "Fronleichnam");

        return holidays;
    }

    public Map<String, String> getHolidaysForWeek(LocalDate monday, LocalDate friday) {
        Map<String, String> result = new LinkedHashMap<>();
        String[] dayKeys = {"monday", "tuesday", "wednesday", "thursday", "friday"};

        // Feiertage für alle relevanten Jahre laden (Jahreswechsel-Wochen)
        Map<LocalDate, String> allHolidays = new LinkedHashMap<>();
        allHolidays.putAll(getHolidaysForYear(monday.getYear()));
        if (friday.getYear() != monday.getYear()) {
            allHolidays.putAll(getHolidaysForYear(friday.getYear()));
        }

        for (int i = 0; i < 5; i++) {
            LocalDate day = monday.plusDays(i);
            String holidayName = allHolidays.get(day);
            if (holidayName != null) {
                result.put(dayKeys[i], holidayName);
            }
        }
        return result;
    }

    public String getHolidayName(LocalDate date) {
        Map<LocalDate, String> holidays = getHolidaysForYear(date.getYear());
        return holidays.get(date);
    }

    public boolean isHoliday(LocalDate date) {
        return getHolidayName(date) != null;
    }

    public LocalDate getNextMonday() {
        LocalDate today = LocalDate.now(java.time.ZoneId.of("Europe/Berlin"));
        if (today.getDayOfWeek() == DayOfWeek.MONDAY) {
            return today.plusWeeks(1);
        }
        return today.with(java.time.temporal.TemporalAdjusters.next(DayOfWeek.MONDAY));
    }

    /**
     * Gauss'sche Osterformel -- berechnet Ostersonntag für ein gegebenes Jahr.
     */
    private LocalDate computeEaster(int year) {
        int a = year % 19;
        int b = year / 100;
        int c = year % 100;
        int d = b / 4;
        int e = b % 4;
        int f = (b + 8) / 25;
        int g = (b - f + 1) / 3;
        int h = (19 * a + b - d - g + 15) % 30;
        int i = c / 4;
        int k = c % 4;
        int l = (32 + 2 * e + 2 * i - h - k) % 7;
        int m = (a + 11 * h + 22 * l) / 451;
        int month = (h + l - 7 * m + 114) / 31;
        int day = ((h + l - 7 * m + 114) % 31) + 1;
        return LocalDate.of(year, month, day);
    }
}
