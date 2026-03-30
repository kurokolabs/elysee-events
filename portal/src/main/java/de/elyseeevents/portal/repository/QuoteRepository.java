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
        long cid = rs.getLong("customer_id"); q.setCustomerId(rs.wasNull() ? null : cid);
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
        try { q.setServicePeriodFrom(rs.getString("service_period_from")); } catch (Exception ignored) {}
        try { q.setServicePeriodTo(rs.getString("service_period_to")); } catch (Exception ignored) {}
        try { q.setIntroText(rs.getString("intro_text")); } catch (Exception ignored) {}
        try { q.setTaxAmount7(rs.getDouble("tax_amount_7")); } catch (Exception ignored) {}
        try { q.setTaxAmount19(rs.getDouble("tax_amount_19")); } catch (Exception ignored) {}
        try { q.setRecipientName(rs.getString("recipient_name")); } catch (Exception ignored) {}
        try { q.setRecipientCompany(rs.getString("recipient_company")); } catch (Exception ignored) {}
        try { q.setRecipientAddress(rs.getString("recipient_address")); } catch (Exception ignored) {}
        try { q.setRecipientPostalCode(rs.getString("recipient_postal_code")); } catch (Exception ignored) {}
        try { q.setRecipientCity(rs.getString("recipient_city")); } catch (Exception ignored) {}
        try { q.setRecipientEmail(rs.getString("recipient_email")); } catch (Exception ignored) {}
        return q;
    };

    private final RowMapper<Quote> rowMapperFull = (rs, rowNum) -> {
        Quote q = rowMapper.mapRow(rs, rowNum);
        try { q.setCustomerName(rs.getString("customer_name")); } catch (Exception ignored) {}
        return q;
    };

    public QuoteRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Quote> findAll() {
        return jdbc.query(
                "SELECT q.*, COALESCE(CONCAT(c.first_name, ' ', c.last_name), q.recipient_name) AS customer_name " +
                "FROM quotes q LEFT JOIN customers c ON q.customer_id = c.id " +
                "ORDER BY q.created_at DESC", rowMapperFull);
    }

    public List<Quote> search(String query) {
        if (query == null || query.isBlank()) return findAll();
        String escaped = query.toLowerCase().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        String like = "%" + escaped + "%";
        return jdbc.query(
                "SELECT q.*, COALESCE(CONCAT(c.first_name, ' ', c.last_name), q.recipient_name) AS customer_name " +
                "FROM quotes q LEFT JOIN customers c ON q.customer_id = c.id " +
                "WHERE LOWER(q.quote_number) LIKE ? OR LOWER(COALESCE(CONCAT(c.first_name, ' ', c.last_name), q.recipient_name)) LIKE ? " +
                "OR LOWER(q.recipient_company) LIKE ? OR LOWER(q.status) LIKE ? " +
                "ORDER BY q.created_at DESC", rowMapperFull, like, like, like, like);
    }

    public List<Quote> findByCustomerId(Long customerId) {
        return jdbc.query(
                "SELECT q.*, CONCAT(c.first_name, ' ', c.last_name) AS customer_name " +
                "FROM quotes q LEFT JOIN customers c ON q.customer_id = c.id " +
                "WHERE q.customer_id = ? ORDER BY q.created_at DESC", rowMapperFull, customerId);
    }

    public Optional<Quote> findById(Long id) {
        List<Quote> list = jdbc.query(
                "SELECT q.*, COALESCE(CONCAT(c.first_name, ' ', c.last_name), q.recipient_name) AS customer_name " +
                "FROM quotes q LEFT JOIN customers c ON q.customer_id = c.id " +
                "WHERE q.id = ?", rowMapperFull, id);
        return list.stream().findFirst();
    }

    public Quote save(Quote q) {
        if (q.getId() == null) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbc.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO quotes (customer_id, booking_id, quote_number, amount, tax_rate, tax_amount, total, status, valid_until, notes, service_period_from, service_period_to, intro_text, tax_amount_7, tax_amount_19, recipient_name, recipient_company, recipient_address, recipient_postal_code, recipient_city, recipient_email) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
                if (q.getCustomerId() != null) ps.setLong(1, q.getCustomerId()); else ps.setNull(1, Types.BIGINT);
                if (q.getBookingId() != null) ps.setLong(2, q.getBookingId()); else ps.setNull(2, Types.BIGINT);
                ps.setString(3, q.getQuoteNumber());
                ps.setDouble(4, q.getAmount());
                ps.setDouble(5, q.getTaxRate());
                ps.setDouble(6, q.getTaxAmount());
                ps.setDouble(7, q.getTotal());
                ps.setString(8, q.getStatus());
                ps.setString(9, q.getValidUntil());
                ps.setString(10, q.getNotes());
                ps.setString(11, q.getServicePeriodFrom());
                ps.setString(12, q.getServicePeriodTo());
                ps.setString(13, q.getIntroText());
                ps.setDouble(14, q.getTaxAmount7());
                ps.setDouble(15, q.getTaxAmount19());
                ps.setString(16, q.getRecipientName());
                ps.setString(17, q.getRecipientCompany());
                ps.setString(18, q.getRecipientAddress());
                ps.setString(19, q.getRecipientPostalCode());
                ps.setString(20, q.getRecipientCity());
                ps.setString(21, q.getRecipientEmail());
                return ps;
            }, keyHolder);
            q.setId(keyHolder.getKey().longValue());
        } else {
            jdbc.update("UPDATE quotes SET status = ?, valid_until = ?, notes = ? WHERE id = ?",
                    q.getStatus(), q.getValidUntil(), q.getNotes(), q.getId());
        }
        return q;
    }

    public synchronized String nextQuoteNumber() {
        String year = java.time.Year.now().toString();
        String prefix = "AN-" + year + "-";
        Long maxNum = jdbc.queryForObject(
                "SELECT MAX(CAST(SUBSTRING(quote_number, ?) AS UNSIGNED)) FROM quotes WHERE quote_number LIKE ?",
                Long.class, prefix.length() + 1, prefix + "%");
        long next = (maxNum != null ? maxNum : 0) + 1;
        return String.format("AN-%s-%04d", year, next);
    }
}
