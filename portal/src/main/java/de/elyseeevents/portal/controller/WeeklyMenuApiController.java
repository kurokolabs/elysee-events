package de.elyseeevents.portal.controller;

import de.elyseeevents.portal.model.WeeklyMenu;
import de.elyseeevents.portal.repository.WeeklyMenuRepository;
import de.elyseeevents.portal.util.BavarianHolidayUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/weekly-menu")
public class WeeklyMenuApiController {

    private final WeeklyMenuRepository weeklyMenuRepository;
    private final BavarianHolidayUtil holidayUtil;

    public WeeklyMenuApiController(WeeklyMenuRepository weeklyMenuRepository, BavarianHolidayUtil holidayUtil) {
        this.weeklyMenuRepository = weeklyMenuRepository;
        this.holidayUtil = holidayUtil;
    }

    @GetMapping("/current")
    public ResponseEntity<?> getCurrentMenu() {
        Optional<WeeklyMenu> menuOpt = weeklyMenuRepository.findCurrent();
        if (menuOpt.isEmpty()) {
            return ResponseEntity.ok(Map.of("available", false));
        }

        WeeklyMenu menu = menuOpt.get();
        Map<String, String> holidays = Map.of();
        try {
            LocalDate monday = LocalDate.parse(menu.getWeekStart());
            holidays = holidayUtil.getHolidaysForWeek(monday, monday.plusDays(4));
        } catch (Exception ignored) {}

        String[] dayKeys = {"monday", "tuesday", "wednesday", "thursday", "friday"};
        String[] dayNames = {"Montag", "Dienstag", "Mittwoch", "Donnerstag", "Freitag"};
        String[] meats = {menu.getMondayMeat(), menu.getTuesdayMeat(), menu.getWednesdayMeat(), menu.getThursdayMeat(), menu.getFridayMeat()};
        String[] vegs = {menu.getMondayVeg(), menu.getTuesdayVeg(), menu.getWednesdayVeg(), menu.getThursdayVeg(), menu.getFridayVeg()};
        String[] meatPrices = {menu.getMondayMeatPrice(), menu.getTuesdayMeatPrice(), menu.getWednesdayMeatPrice(), menu.getThursdayMeatPrice(), menu.getFridayMeatPrice()};
        String[] vegPrices = {menu.getMondayVegPrice(), menu.getTuesdayVegPrice(), menu.getWednesdayVegPrice(), menu.getThursdayVegPrice(), menu.getFridayVegPrice()};

        LinkedHashMap<String, Object>[] days = new LinkedHashMap[5];
        for (int i = 0; i < 5; i++) {
            LinkedHashMap<String, Object> entry = new LinkedHashMap<>();
            entry.put("day", dayNames[i]);
            String holiday = holidays.get(dayKeys[i]);
            if (holiday != null) {
                entry.put("holiday", holiday);
                entry.put("meat", "");
                entry.put("vegetarian", "");
                entry.put("meatPrice", "");
                entry.put("vegPrice", "");
            } else {
                entry.put("holiday", null);
                entry.put("meat", meats[i] != null ? meats[i] : "");
                entry.put("vegetarian", vegs[i] != null ? vegs[i] : "");
                entry.put("meatPrice", meatPrices[i] != null ? meatPrices[i] : "");
                entry.put("vegPrice", vegPrices[i] != null ? vegPrices[i] : "");
            }
            days[i] = entry;
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("available", true);
        result.put("weekStart", menu.getWeekStart());
        result.put("weekEnd", menu.getWeekEnd());
        result.put("days", days);
        result.put("notes", menu.getNotes());
        return ResponseEntity.ok(result);
    }
}
