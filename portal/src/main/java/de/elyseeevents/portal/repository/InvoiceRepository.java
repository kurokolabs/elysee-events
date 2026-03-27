package de.elyseeevents.portal.repository;

import de.elyseeevents.portal.model.Invoice;
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
public class InvoiceRepository {

    private final JdbcTemplate jdbc;

    private final RowMapper<Invoice> rowMapper = (rs, rowNum) -> {
        Invoice i = new Invoice();
        i.setId(rs.getLong("id"));
        long bid = rs.getLong("booking_id"); i.setBookingId(rs.wasNull() ? null : bid);
        long cid = rs.getLong("customer_id"); i.setCustomerId(rs.wasNull() ? null : cid);
        i.setInvoiceNumber(rs.getString("invoice_number"));
        i.setAmount(rs.getDouble("amount"));
        i.setTaxRate(rs.getDouble("tax_rate"));
        i.setTaxAmount(rs.getDouble("tax_amount"));
        i.setTotal(rs.getDouble("total"));
        i.setStatus(rs.getString("status"));
        i.setDueDate(rs.getString("due_date"));
        i.setPaidDate(rs.getString("paid_date"));
        i.setNotes(rs.getString("notes"));
        i.setCreatedAt(rs.getString("created_at"));
        try { i.setRecipientName(rs.getString("recipient_name")); } catch (Exception ignored) {}
        try { i.setRecipientCompany(rs.getString("recipient_company")); } catch (Exception ignored) {}
        try { i.setRecipientAddress(rs.getString("recipient_address")); } catch (Exception ignored) {}
        try { i.setRecipientPostalCode(rs.getString("recipient_postal_code")); } catch (Exception ignored) {}
        try { i.setRecipientCity(rs.getString("recipient_city")); } catch (Exception ignored) {}
        try { i.setRecipientEmail(rs.getString("recipient_email")); } catch (Exception ignored) {}
        try { i.setServicePeriodFrom(rs.getString("service_period_from")); } catch (Exception ignored) {}
        try { i.setServicePeriodTo(rs.getString("service_period_to")); } catch (Exception ignored) {}
        try { i.setIntroText(rs.getString("intro_text")); } catch (Exception ignored) {}
        try { i.setTaxAmount7(rs.getDouble("tax_amount_7")); } catch (Exception ignored) {}
        try { i.setTaxAmount19(rs.getDouble("tax_amount_19")); } catch (Exception ignored) {}
        return i;
    };

    private final RowMapper<Invoice> rowMapperFull = (rs, rowNum) -> {
        Invoice i = rowMapper.mapRow(rs, rowNum);
        try { i.setCustomerName(rs.getString("customer_name")); } catch (Exception ignored) {}
        try { i.setBookingType(rs.getString("booking_type")); } catch (Exception ignored) {}
        return i;
    };

    public InvoiceRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Invoice> findAll() {
        return jdbc.query(
                "SELECT i.*, COALESCE(CONCAT(c.first_name, ' ', c.last_name), i.recipient_name) AS customer_name, b.booking_type " +
                "FROM invoices i LEFT JOIN customers c ON i.customer_id = c.id LEFT JOIN bookings b ON i.booking_id = b.id " +
                "ORDER BY i.created_at DESC", rowMapperFull);
    }

    public List<Invoice> findByCustomerId(Long customerId) {
        return jdbc.query(
                "SELECT i.*, CONCAT(c.first_name, ' ', c.last_name) AS customer_name, b.booking_type " +
                "FROM invoices i LEFT JOIN customers c ON i.customer_id = c.id LEFT JOIN bookings b ON i.booking_id = b.id " +
                "WHERE i.customer_id = ? ORDER BY i.created_at DESC", rowMapperFull, customerId);
    }

    public Optional<Invoice> findById(Long id) {
        List<Invoice> list = jdbc.query(
                "SELECT i.*, COALESCE(CONCAT(c.first_name, ' ', c.last_name), i.recipient_name) AS customer_name, b.booking_type " +
                "FROM invoices i LEFT JOIN customers c ON i.customer_id = c.id LEFT JOIN bookings b ON i.booking_id = b.id " +
                "WHERE i.id = ?", rowMapperFull, id);
        return list.stream().findFirst();
    }

    public Invoice save(Invoice inv) {
        if (inv.getId() == null) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbc.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO invoices (booking_id, customer_id, invoice_number, amount, tax_rate, tax_amount, total, status, due_date, notes, service_period_from, service_period_to, intro_text, tax_amount_7, tax_amount_19, recipient_name, recipient_company, recipient_address, recipient_postal_code, recipient_city, recipient_email) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
                if (inv.getBookingId() != null) ps.setLong(1, inv.getBookingId()); else ps.setNull(1, java.sql.Types.BIGINT);
                if (inv.getCustomerId() != null) ps.setLong(2, inv.getCustomerId()); else ps.setNull(2, java.sql.Types.BIGINT);
                ps.setString(3, inv.getInvoiceNumber());
                ps.setDouble(4, inv.getAmount());
                ps.setDouble(5, inv.getTaxRate());
                ps.setDouble(6, inv.getTaxAmount());
                ps.setDouble(7, inv.getTotal());
                ps.setString(8, inv.getStatus());
                ps.setString(9, inv.getDueDate());
                ps.setString(10, inv.getNotes());
                ps.setString(11, inv.getServicePeriodFrom());
                ps.setString(12, inv.getServicePeriodTo());
                ps.setString(13, inv.getIntroText());
                ps.setDouble(14, inv.getTaxAmount7());
                ps.setDouble(15, inv.getTaxAmount19());
                ps.setString(16, inv.getRecipientName());
                ps.setString(17, inv.getRecipientCompany());
                ps.setString(18, inv.getRecipientAddress());
                ps.setString(19, inv.getRecipientPostalCode());
                ps.setString(20, inv.getRecipientCity());
                ps.setString(21, inv.getRecipientEmail());
                return ps;
            }, keyHolder);
            inv.setId(keyHolder.getKey().longValue());
        } else {
            jdbc.update("UPDATE invoices SET status = ?, paid_date = ?, notes = ? WHERE id = ?",
                    inv.getStatus(), inv.getPaidDate(), inv.getNotes(), inv.getId());
        }
        return inv;
    }

    public synchronized String nextInvoiceNumber() {
        String year = java.time.Year.now().toString();
        String prefix = "RE-" + year + "-";
        Long maxNum = jdbc.queryForObject(
                "SELECT MAX(CAST(SUBSTR(invoice_number, " + (prefix.length() + 1) + ") AS UNSIGNED)) FROM invoices WHERE invoice_number LIKE ?",
                Long.class, prefix + "%");
        long next = (maxNum != null ? maxNum : 0) + 1;
        return String.format("RE-%s-%04d", year, next);
    }
}
