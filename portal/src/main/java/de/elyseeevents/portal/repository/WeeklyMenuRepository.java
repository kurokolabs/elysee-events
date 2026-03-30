package de.elyseeevents.portal.repository;

import de.elyseeevents.portal.model.WeeklyMenu;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

@Repository
public class WeeklyMenuRepository {

    private final JdbcTemplate jdbc;

    private final RowMapper<WeeklyMenu> rowMapper = (rs, rowNum) -> {
        WeeklyMenu m = new WeeklyMenu();
        m.setId(rs.getLong("id"));
        m.setWeekStart(rs.getString("week_start"));
        m.setWeekEnd(rs.getString("week_end"));
        m.setMonday(rs.getString("monday"));
        m.setTuesday(rs.getString("tuesday"));
        m.setWednesday(rs.getString("wednesday"));
        m.setThursday(rs.getString("thursday"));
        m.setFriday(rs.getString("friday"));
        m.setMondayMeat(rs.getString("monday_meat"));
        m.setMondayVeg(rs.getString("monday_veg"));
        m.setTuesdayMeat(rs.getString("tuesday_meat"));
        m.setTuesdayVeg(rs.getString("tuesday_veg"));
        m.setWednesdayMeat(rs.getString("wednesday_meat"));
        m.setWednesdayVeg(rs.getString("wednesday_veg"));
        m.setThursdayMeat(rs.getString("thursday_meat"));
        m.setThursdayVeg(rs.getString("thursday_veg"));
        m.setFridayMeat(rs.getString("friday_meat"));
        m.setFridayVeg(rs.getString("friday_veg"));
        m.setMondayMeatPrice(rs.getString("monday_meat_price"));
        m.setMondayVegPrice(rs.getString("monday_veg_price"));
        m.setTuesdayMeatPrice(rs.getString("tuesday_meat_price"));
        m.setTuesdayVegPrice(rs.getString("tuesday_veg_price"));
        m.setWednesdayMeatPrice(rs.getString("wednesday_meat_price"));
        m.setWednesdayVegPrice(rs.getString("wednesday_veg_price"));
        m.setThursdayMeatPrice(rs.getString("thursday_meat_price"));
        m.setThursdayVegPrice(rs.getString("thursday_veg_price"));
        m.setFridayMeatPrice(rs.getString("friday_meat_price"));
        m.setFridayVegPrice(rs.getString("friday_veg_price"));
        m.setStatus(rs.getString("status"));
        m.setNotes(rs.getString("notes"));
        m.setSent(rs.getInt("sent") == 1);
        m.setSentAt(rs.getString("sent_at"));
        m.setCreatedAt(rs.getString("created_at"));
        return m;
    };

    public WeeklyMenuRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<WeeklyMenu> findAll() {
        return jdbc.query("SELECT * FROM weekly_menus ORDER BY week_start DESC", rowMapper);
    }

    public Optional<WeeklyMenu> findById(Long id) {
        List<WeeklyMenu> list = jdbc.query("SELECT * FROM weekly_menus WHERE id = ?", rowMapper, id);
        return list.stream().findFirst();
    }

    public Optional<WeeklyMenu> findCurrent() {
        List<WeeklyMenu> list = jdbc.query(
                "SELECT * FROM weekly_menus WHERE week_start <= CURDATE() AND week_end >= CURDATE()",
                rowMapper);
        return list.stream().findFirst();
    }

    public WeeklyMenu save(WeeklyMenu m) {
        if (m.getId() == null) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbc.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(
                        "INSERT INTO weekly_menus (week_start, week_end, monday, tuesday, wednesday, thursday, friday, " +
                        "monday_meat, monday_veg, tuesday_meat, tuesday_veg, wednesday_meat, wednesday_veg, " +
                        "thursday_meat, thursday_veg, friday_meat, friday_veg, " +
                        "monday_meat_price, monday_veg_price, tuesday_meat_price, tuesday_veg_price, " +
                        "wednesday_meat_price, wednesday_veg_price, thursday_meat_price, thursday_veg_price, " +
                        "friday_meat_price, friday_veg_price, status, notes) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS);
                int i = 1;
                ps.setString(i++, m.getWeekStart());
                ps.setString(i++, m.getWeekEnd());
                ps.setString(i++, m.getMonday());
                ps.setString(i++, m.getTuesday());
                ps.setString(i++, m.getWednesday());
                ps.setString(i++, m.getThursday());
                ps.setString(i++, m.getFriday());
                ps.setString(i++, m.getMondayMeat());
                ps.setString(i++, m.getMondayVeg());
                ps.setString(i++, m.getTuesdayMeat());
                ps.setString(i++, m.getTuesdayVeg());
                ps.setString(i++, m.getWednesdayMeat());
                ps.setString(i++, m.getWednesdayVeg());
                ps.setString(i++, m.getThursdayMeat());
                ps.setString(i++, m.getThursdayVeg());
                ps.setString(i++, m.getFridayMeat());
                ps.setString(i++, m.getFridayVeg());
                ps.setString(i++, m.getMondayMeatPrice());
                ps.setString(i++, m.getMondayVegPrice());
                ps.setString(i++, m.getTuesdayMeatPrice());
                ps.setString(i++, m.getTuesdayVegPrice());
                ps.setString(i++, m.getWednesdayMeatPrice());
                ps.setString(i++, m.getWednesdayVegPrice());
                ps.setString(i++, m.getThursdayMeatPrice());
                ps.setString(i++, m.getThursdayVegPrice());
                ps.setString(i++, m.getFridayMeatPrice());
                ps.setString(i++, m.getFridayVegPrice());
                ps.setString(i++, m.getStatus());
                ps.setString(i, m.getNotes());
                return ps;
            }, keyHolder);
            m.setId(keyHolder.getKey().longValue());
        } else {
            jdbc.update("UPDATE weekly_menus SET week_start=?, week_end=?, monday=?, tuesday=?, " +
                        "wednesday=?, thursday=?, friday=?, " +
                        "monday_meat=?, monday_veg=?, tuesday_meat=?, tuesday_veg=?, " +
                        "wednesday_meat=?, wednesday_veg=?, thursday_meat=?, thursday_veg=?, " +
                        "friday_meat=?, friday_veg=?, " +
                        "monday_meat_price=?, monday_veg_price=?, tuesday_meat_price=?, tuesday_veg_price=?, " +
                        "wednesday_meat_price=?, wednesday_veg_price=?, thursday_meat_price=?, thursday_veg_price=?, " +
                        "friday_meat_price=?, friday_veg_price=?, status=?, notes=? WHERE id=?",
                    m.getWeekStart(), m.getWeekEnd(), m.getMonday(), m.getTuesday(),
                    m.getWednesday(), m.getThursday(), m.getFriday(),
                    m.getMondayMeat(), m.getMondayVeg(), m.getTuesdayMeat(), m.getTuesdayVeg(),
                    m.getWednesdayMeat(), m.getWednesdayVeg(), m.getThursdayMeat(), m.getThursdayVeg(),
                    m.getFridayMeat(), m.getFridayVeg(),
                    m.getMondayMeatPrice(), m.getMondayVegPrice(), m.getTuesdayMeatPrice(), m.getTuesdayVegPrice(),
                    m.getWednesdayMeatPrice(), m.getWednesdayVegPrice(), m.getThursdayMeatPrice(), m.getThursdayVegPrice(),
                    m.getFridayMeatPrice(), m.getFridayVegPrice(), m.getStatus(), m.getNotes(), m.getId());
        }
        return m;
    }

    public void markSent(Long id) {
        jdbc.update("UPDATE weekly_menus SET sent = 1, sent_at = NOW(), status = 'VERSENDET' WHERE id = ?", id);
    }

    public void updateStatus(Long id, String status) {
        jdbc.update("UPDATE weekly_menus SET status = ? WHERE id = ?", status, id);
    }

    public List<WeeklyMenu> findByStatusAndWeekStart(String status, String weekStart) {
        return jdbc.query("SELECT * FROM weekly_menus WHERE status = ? AND week_start = ?",
                rowMapper, status, weekStart);
    }

    public List<String> findDistinctDishes() {
        String sql = "SELECT DISTINCT dish FROM (" +
                "SELECT monday_meat AS dish FROM weekly_menus UNION " +
                "SELECT monday_veg FROM weekly_menus UNION " +
                "SELECT tuesday_meat FROM weekly_menus UNION " +
                "SELECT tuesday_veg FROM weekly_menus UNION " +
                "SELECT wednesday_meat FROM weekly_menus UNION " +
                "SELECT wednesday_veg FROM weekly_menus UNION " +
                "SELECT thursday_meat FROM weekly_menus UNION " +
                "SELECT thursday_veg FROM weekly_menus UNION " +
                "SELECT friday_meat FROM weekly_menus UNION " +
                "SELECT friday_veg FROM weekly_menus" +
                ") AS all_dishes WHERE dish IS NOT NULL AND dish != '' ORDER BY dish";
        return jdbc.queryForList(sql, String.class);
    }
}
