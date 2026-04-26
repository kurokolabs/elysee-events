package de.elyseeevents.portal.repository;

import de.elyseeevents.portal.model.KantineReservation;
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
public class KantineReservationRepository {

    private final JdbcTemplate jdbc;

    private final RowMapper<KantineReservation> rowMapper = (rs, rowNum) -> {
        KantineReservation r = new KantineReservation();
        r.setId(rs.getLong("id"));
        r.setCustomerId(rs.getLong("customer_id"));
        r.setName(rs.getString("name"));
        r.setSeatCount(rs.getInt("seat_count"));
        r.setReservationDate(rs.getString("reservation_date"));
        r.setStatus(rs.getString("status"));
        r.setCreatedAt(rs.getString("created_at"));
        return r;
    };

    public KantineReservationRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<KantineReservation> findAll() {
        return jdbc.query(
                "SELECT * FROM kantine_reservations ORDER BY created_at DESC",
                rowMapper);
    }

    public List<KantineReservation> findByCustomerId(Long customerId) {
        return jdbc.query(
                "SELECT * FROM kantine_reservations WHERE customer_id = ? ORDER BY created_at DESC",
                rowMapper, customerId);
    }

    public Optional<KantineReservation> findById(Long id) {
        List<KantineReservation> list = jdbc.query(
                "SELECT * FROM kantine_reservations WHERE id = ?", rowMapper, id);
        return list.stream().findFirst();
    }

    public KantineReservation save(KantineReservation r) {
        if (r.getId() == null) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbc.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(
                        "INSERT INTO kantine_reservations " +
                        "(customer_id, name, seat_count, reservation_date, status) " +
                        "VALUES (?, ?, ?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS);
                ps.setLong(1, r.getCustomerId());
                ps.setString(2, r.getName());
                ps.setInt(3, r.getSeatCount());
                ps.setString(4, r.getReservationDate());
                ps.setString(5, r.getStatus());
                return ps;
            }, keyHolder);
            r.setId(keyHolder.getKey().longValue());
        } else {
            jdbc.update(
                    "UPDATE kantine_reservations SET name=?, seat_count=?, reservation_date=?, status=? WHERE id=?",
                    r.getName(), r.getSeatCount(), r.getReservationDate(), r.getStatus(), r.getId());
        }
        return r;
    }

    public void updateStatus(Long id, String status) {
        jdbc.update("UPDATE kantine_reservations SET status = ? WHERE id = ?", status, id);
    }

    public void deleteById(Long id) {
        jdbc.update("DELETE FROM kantine_reservations WHERE id = ?", id);
    }
}
