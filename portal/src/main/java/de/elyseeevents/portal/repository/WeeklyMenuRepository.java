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
                "SELECT * FROM weekly_menus WHERE week_start <= date('now') AND week_end >= date('now')",
                rowMapper);
        return list.stream().findFirst();
    }

    public WeeklyMenu save(WeeklyMenu m) {
        if (m.getId() == null) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbc.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(
                        "INSERT INTO weekly_menus (week_start, week_end, monday, tuesday, wednesday, thursday, friday, notes) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS);
                ps.setString(1, m.getWeekStart());
                ps.setString(2, m.getWeekEnd());
                ps.setString(3, m.getMonday());
                ps.setString(4, m.getTuesday());
                ps.setString(5, m.getWednesday());
                ps.setString(6, m.getThursday());
                ps.setString(7, m.getFriday());
                ps.setString(8, m.getNotes());
                return ps;
            }, keyHolder);
            m.setId(keyHolder.getKey().longValue());
        } else {
            jdbc.update("UPDATE weekly_menus SET week_start = ?, week_end = ?, monday = ?, tuesday = ?, " +
                        "wednesday = ?, thursday = ?, friday = ?, notes = ? WHERE id = ?",
                    m.getWeekStart(), m.getWeekEnd(), m.getMonday(), m.getTuesday(),
                    m.getWednesday(), m.getThursday(), m.getFriday(), m.getNotes(), m.getId());
        }
        return m;
    }

    public void markSent(Long id) {
        jdbc.update("UPDATE weekly_menus SET sent = 1, sent_at = datetime('now') WHERE id = ?", id);
    }
}
