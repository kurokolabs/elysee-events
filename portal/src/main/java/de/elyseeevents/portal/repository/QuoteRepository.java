package de.elyseeevents.portal.repository;

import de.elyseeevents.portal.model.Quote;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Types;
import java.util.List;
import java.util.Optional;

@Repository
public class QuoteRepository {

    private final JdbcTemplate jdbc;

    private final RowMapper<Quote> rowMapper = (rs, rowNum) -> {
        Quote q = new Quote();
        q.setId(rs.getLong("id"));
        q.setCustomerId(rs.getLong("customer_id"));
        q.setBookingId(rs.getObject("booking_id") != null ? rs.getLong("booking_id") : null);
        q.setQuoteNumber(rs.getString("quote_number"));
        q.setAmount(rs.getDouble("amount"));
        q.setTaxRate(rs.getDouble("tax_rate"));
        q.setTaxAmount(rs.getDouble("tax_amount"));
        q.setTotal(rs.getDouble("total"));
        q.setStatus(rs.getString("status"));
        q.setValidUntil(rs.getString("valid_until"));
        q.setNotes(rs.getString("notes"));
        q.setCreatedAt(rs.getString("created_at"));
        return q;
    };

    private final RowMapper<Quote> rowMapperFull = (rs, rowNum) -> {
        Quote q = rowMapper.mapRow(rs, rowNum);
        q.setCustomerName(rs.getString("customer_name"));
        return q;
    };

    public QuoteRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Quote> findAll() {
        return jdbc.query(
                "SELECT q.*, (c.first_name || ' ' || c.last_name) AS customer_name " +
                "FROM quotes q JOIN customers c ON q.customer_id = c.id " +
                "ORDER BY q.created_at DESC", rowMapperFull);
    }

    public List<Quote> findByCustomerId(Long customerId) {
        return jdbc.query(
                "SELECT q.*, (c.first_name || ' ' || c.last_name) AS customer_name " +
                "FROM quotes q JOIN customers c ON q.customer_id = c.id " +
                "WHERE q.customer_id = ? ORDER BY q.created_at DESC", rowMapperFull, customerId);
    }

    public Optional<Quote> findById(Long id) {
        List<Quote> list = jdbc.query(
                "SELECT q.*, (c.first_name || ' ' || c.last_name) AS customer_name " +
                "FROM quotes q JOIN customers c ON q.customer_id = c.id " +
                "WHERE q.id = ?", rowMapperFull, id);
        return list.stream().findFirst();
    }

    public Quote save(Quote q) {
        if (q.getId() == null) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbc.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO quotes (customer_id, booking_id, quote_number, amount, tax_rate, tax_amount, total, status, valid_until, notes) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
                ps.setLong(1, q.getCustomerId());
                if (q.getBookingId() != null) { ps.setLong(2, q.getBookingId()); } else { ps.setNull(2, Types.BIGINT); }
                ps.setString(3, q.getQuoteNumber());
                ps.setDouble(4, q.getAmount());
                ps.setDouble(5, q.getTaxRate());
                ps.setDouble(6, q.getTaxAmount());
                ps.setDouble(7, q.getTotal());
                ps.setString(8, q.getStatus());
                ps.setString(9, q.getValidUntil());
                ps.setString(10, q.getNotes());
                return ps;
            }, keyHolder);
            q.setId(keyHolder.getKey().longValue());
        } else {
            jdbc.update("UPDATE quotes SET status = ?, valid_until = ?, notes = ? WHERE id = ?",
                    q.getStatus(), q.getValidUntil(), q.getNotes(), q.getId());
        }
        return q;
    }

    public String nextQuoteNumber() {
        String year = java.time.Year.now().toString();
        String prefix = "AN-" + year + "-";
        Long maxNum = jdbc.queryForObject(
                "SELECT MAX(CAST(SUBSTR(quote_number, " + (prefix.length() + 1) + ") AS INTEGER)) FROM quotes WHERE quote_number LIKE ?",
                Long.class, prefix + "%");
        long next = (maxNum != null ? maxNum : 0) + 1;
        return String.format("AN-%s-%04d", year, next);
    }
}
