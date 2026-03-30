package de.elyseeevents.portal.controller;

import de.elyseeevents.portal.model.WeeklyMenu;
import de.elyseeevents.portal.repository.WeeklyMenuRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/weekly-menu")
public class WeeklyMenuApiController {

    private final WeeklyMenuRepository weeklyMenuRepository;

    public WeeklyMenuApiController(WeeklyMenuRepository weeklyMenuRepository) {
        this.weeklyMenuRepository = weeklyMenuRepository;
    }

    @GetMapping("/current")
    public ResponseEntity<?> getCurrentMenu() {
        Optional<WeeklyMenu> menuOpt = weeklyMenuRepository.findCurrent();
        if (menuOpt.isEmpty()) {
            return ResponseEntity.ok(Map.of("available", false));
        }

        WeeklyMenu menu = menuOpt.get();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("available", true);
        result.put("weekStart", menu.getWeekStart());
        result.put("weekEnd", menu.getWeekEnd());
        result.put("days", new LinkedHashMap[] {
            dayEntry("Montag", menu.getMondayMeat(), menu.getMondayVeg(), menu.getMonday(), menu.getMondayMeatPrice(), menu.getMondayVegPrice()),
            dayEntry("Dienstag", menu.getTuesdayMeat(), menu.getTuesdayVeg(), menu.getTuesday(), menu.getTuesdayMeatPrice(), menu.getTuesdayVegPrice()),
            dayEntry("Mittwoch", menu.getWednesdayMeat(), menu.getWednesdayVeg(), menu.getWednesday(), menu.getWednesdayMeatPrice(), menu.getWednesdayVegPrice()),
            dayEntry("Donnerstag", menu.getThursdayMeat(), menu.getThursdayVeg(), menu.getThursday(), menu.getThursdayMeatPrice(), menu.getThursdayVegPrice()),
            dayEntry("Freitag", menu.getFridayMeat(), menu.getFridayVeg(), menu.getFriday(), menu.getFridayMeatPrice(), menu.getFridayVegPrice())
        });
        result.put("notes", menu.getNotes());
        return ResponseEntity.ok(result);
    }

    private LinkedHashMap<String, String> dayEntry(String day, String meat, String veg, String legacy,
                                                     String meatPrice, String vegPrice) {
        LinkedHashMap<String, String> entry = new LinkedHashMap<>();
        entry.put("day", day);
        entry.put("meat", meat != null ? meat : (legacy != null ? legacy : ""));
        entry.put("vegetarian", veg != null ? veg : "");
        entry.put("meatPrice", meatPrice != null ? meatPrice : "");
        entry.put("vegPrice", vegPrice != null ? vegPrice : "");
        return entry;
    }
}
