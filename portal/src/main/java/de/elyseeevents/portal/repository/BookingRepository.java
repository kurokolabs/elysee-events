package de.elyseeevents.portal.repository;

import de.elyseeevents.portal.model.Booking;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Types;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class BookingRepository {

    private final JdbcTemplate jdbc;

    private final RowMapper<Booking> rowMapper = (rs, rowNum) -> {
        Booking b = new Booking();
        b.setId(rs.getLong("id"));
        b.setCustomerId(rs.getLong("customer_id"));
        b.setBookingType(rs.getString("booking_type"));
        b.setStatus(rs.getString("status"));
        b.setEventDate(rs.getString("event_date"));
        b.setEventTimeSlot(rs.getString("event_time_slot"));
        b.setGuestCount(rs.getObject("guest_count") != null ? rs.getInt("guest_count") : null);
        b.setBudget(rs.getObject("budget") != null ? rs.getDouble("budget") : null);
        b.setMenuSelection(rs.getString("menu_selection"));
        b.setSpecialRequests(rs.getString("special_requests"));
        b.setAdminNotes(rs.getString("admin_notes"));
        b.setDeliveryAddress(rs.getString("delivery_address"));
        b.setCateringPackage(rs.getString("catering_package"));
        b.setFoodOption(rs.getString("food_option"));
        b.setFoodSubOption(rs.getString("food_sub_option"));
        b.setCuisineStyle(rs.getString("cuisine_style"));
        b.setCreatedAt(rs.getString("created_at"));
        b.setUpdatedAt(rs.getString("updated_at"));
        return b;
    };

    private final RowMapper<Booking> rowMapperWithCustomer = (rs, rowNum) -> {
        Booking b = rowMapper.mapRow(rs, rowNum);
        b.setCustomerName(rs.getString("customer_name"));
        b.setCustomerCompany(rs.getString("customer_company"));
        return b;
    };

    public BookingRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Booking> findAll() {
        return jdbc.query(
                "SELECT b.*, (c.first_name || ' ' || c.last_name) AS customer_name, c.company AS customer_company " +
                "FROM bookings b JOIN customers c ON b.customer_id = c.id ORDER BY b.created_at DESC",
                rowMapperWithCustomer);
    }

    public List<Booking> findByFilters(String type, String status, String dateFrom, String dateTo) {
        StringBuilder sql = new StringBuilder(
                "SELECT b.*, (c.first_name || ' ' || c.last_name) AS customer_name, c.company AS customer_company " +
                "FROM bookings b JOIN customers c ON b.customer_id = c.id WHERE 1=1");
        java.util.List<Object> params = new java.util.ArrayList<>();

        if (type != null && !type.isEmpty()) {
            sql.append(" AND b.booking_type = ?");
            params.add(type);
        }
        if (status != null && !status.isEmpty()) {
            sql.append(" AND b.status = ?");
            params.add(status);
        }
        if (dateFrom != null && !dateFrom.isEmpty()) {
            sql.append(" AND b.event_date >= ?");
            params.add(dateFrom);
        }
        if (dateTo != null && !dateTo.isEmpty()) {
            sql.append(" AND b.event_date <= ?");
            params.add(dateTo);
        }
        sql.append(" ORDER BY b.created_at DESC");
        return jdbc.query(sql.toString(), rowMapperWithCustomer, params.toArray());
    }

    public List<Booking> findByCustomerId(Long customerId) {
        return jdbc.query("SELECT b.*, (c.first_name || ' ' || c.last_name) AS customer_name, c.company AS customer_company " +
                "FROM bookings b JOIN customers c ON b.customer_id = c.id WHERE b.customer_id = ? ORDER BY b.created_at DESC",
                rowMapperWithCustomer, customerId);
    }

    public Optional<Booking> findById(Long id) {
        List<Booking> list = jdbc.query(
                "SELECT b.*, (c.first_name || ' ' || c.last_name) AS customer_name, c.company AS customer_company " +
                "FROM bookings b JOIN customers c ON b.customer_id = c.id WHERE b.id = ?",
                rowMapperWithCustomer, id);
        return list.stream().findFirst();
    }

    public Booking save(Booking b) {
        if (b.getId() == null) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbc.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO bookings (customer_id, booking_type, status, event_date, event_time_slot, guest_count, budget, menu_selection, special_requests, admin_notes, delivery_address, catering_package, food_option, food_sub_option, cuisine_style) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
                ps.setLong(1, b.getCustomerId());
                ps.setString(2, b.getBookingType());
                ps.setString(3, b.getStatus());
                ps.setString(4, b.getEventDate());
                ps.setString(5, b.getEventTimeSlot());
                if (b.getGuestCount() != null) { ps.setInt(6, b.getGuestCount()); } else { ps.setNull(6, Types.INTEGER); }
                if (b.getBudget() != null) { ps.setDouble(7, b.getBudget()); } else { ps.setNull(7, Types.DOUBLE); }
                ps.setString(8, b.getMenuSelection());
                ps.setString(9, b.getSpecialRequests());
                ps.setString(10, b.getAdminNotes());
                ps.setString(11, b.getDeliveryAddress());
                ps.setString(12, b.getCateringPackage());
                ps.setString(13, b.getFoodOption());
                ps.setString(14, b.getFoodSubOption());
                ps.setString(15, b.getCuisineStyle());
                return ps;
            }, keyHolder);
            b.setId(keyHolder.getKey().longValue());
        } else {
            jdbc.update("UPDATE bookings SET customer_id = ?, booking_type = ?, status = ?, event_date = ?, event_time_slot = ?, guest_count = ?, budget = ?, menu_selection = ?, special_requests = ?, admin_notes = ?, delivery_address = ?, catering_package = ?, food_option = ?, food_sub_option = ?, cuisine_style = ?, updated_at = datetime('now') WHERE id = ?",
                    b.getCustomerId(), b.getBookingType(), b.getStatus(),
                    b.getEventDate(), b.getEventTimeSlot(), b.getGuestCount(), b.getBudget(),
                    b.getMenuSelection(), b.getSpecialRequests(), b.getAdminNotes(),
                    b.getDeliveryAddress(), b.getCateringPackage(), b.getFoodOption(),
                    b.getFoodSubOption(), b.getCuisineStyle(), b.getId());
        }
        return b;
    }

    public void setCreatedAt(Long id, String createdAt) {
        jdbc.update("UPDATE bookings SET created_at = ? WHERE id = ?", createdAt, id);
    }

    public void updateStatus(Long id, String status) {
        jdbc.update("UPDATE bookings SET status = ?, updated_at = datetime('now') WHERE id = ?", status, id);
    }

    public long count() {
        Long c = jdbc.queryForObject("SELECT COUNT(*) FROM bookings", Long.class);
        return c != null ? c : 0;
    }

    public long countByStatus(String status) {
        Long c = jdbc.queryForObject("SELECT COUNT(*) FROM bookings WHERE status = ?", Long.class, status);
        return c != null ? c : 0;
    }

    public long countThisMonth() {
        Long c = jdbc.queryForObject("SELECT COUNT(*) FROM bookings WHERE strftime('%Y-%m', created_at) = strftime('%Y-%m', 'now')", Long.class);
        return c != null ? c : 0;
    }

    public Double totalBudget() {
        Double sum = jdbc.queryForObject("SELECT COALESCE(SUM(budget), 0) FROM bookings WHERE status != 'STORNIERT'", Double.class);
        return sum != null ? sum : 0.0;
    }

    public List<java.util.Map<String, Object>> monthlyRevenue(int months) {
        return jdbc.queryForList(
                "WITH RECURSIVE m(d) AS (" +
                "  SELECT date('now','start of month','-' || (? - 1) || ' months') " +
                "  UNION ALL SELECT date(d, '+1 month') FROM m WHERE d < date('now','start of month')" +
                ") SELECT strftime('%Y-%m', m.d) AS month, COALESCE(SUM(b.budget), 0) AS revenue " +
                "FROM m LEFT JOIN bookings b ON strftime('%Y-%m', b.created_at) = strftime('%Y-%m', m.d) " +
                "AND b.status != 'STORNIERT' GROUP BY strftime('%Y-%m', m.d) ORDER BY month", months);
    }

    public List<Booking> findRecent(int limit) {
        return jdbc.query(
                "SELECT b.*, (c.first_name || ' ' || c.last_name) AS customer_name, c.company AS customer_company " +
                "FROM bookings b JOIN customers c ON b.customer_id = c.id ORDER BY b.created_at DESC LIMIT ?",
                rowMapperWithCustomer, limit);
    }

    public List<Map<String, Object>> availabilityData(int year, int month) {
        String monthStr = String.format("%04d-%02d", year, month);
        return jdbc.queryForList(
                "SELECT event_date, event_time_slot, booking_type " +
                "FROM bookings WHERE strftime('%Y-%m', event_date) = ? " +
                "AND status NOT IN ('STORNIERT') ORDER BY event_date",
                monthStr);
    }

    public List<Map<String, Object>> calendarData(int year, int month) {
        String monthStr = String.format("%04d-%02d", year, month);
        return jdbc.queryForList(
                "SELECT b.id, b.event_date, b.booking_type, b.status, " +
                "(c.first_name || ' ' || c.last_name) AS customer_name " +
                "FROM bookings b JOIN customers c ON b.customer_id = c.id " +
                "WHERE strftime('%Y-%m', b.event_date) = ? ORDER BY b.event_date",
                monthStr);
    }
}
