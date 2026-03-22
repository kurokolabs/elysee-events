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
        i.setBookingId(rs.getLong("booking_id"));
        i.setCustomerId(rs.getLong("customer_id"));
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
        return i;
    };

    private final RowMapper<Invoice> rowMapperFull = (rs, rowNum) -> {
        Invoice i = rowMapper.mapRow(rs, rowNum);
        i.setCustomerName(rs.getString("customer_name"));
        i.setBookingType(rs.getString("booking_type"));
        return i;
    };

    public InvoiceRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Invoice> findAll() {
        return jdbc.query(
                "SELECT i.*, (c.first_name || ' ' || c.last_name) AS customer_name, b.booking_type " +
                "FROM invoices i JOIN customers c ON i.customer_id = c.id JOIN bookings b ON i.booking_id = b.id " +
                "ORDER BY i.created_at DESC", rowMapperFull);
    }

    public List<Invoice> findByCustomerId(Long customerId) {
        return jdbc.query(
                "SELECT i.*, (c.first_name || ' ' || c.last_name) AS customer_name, b.booking_type " +
                "FROM invoices i JOIN customers c ON i.customer_id = c.id JOIN bookings b ON i.booking_id = b.id " +
                "WHERE i.customer_id = ? ORDER BY i.created_at DESC", rowMapperFull, customerId);
    }

    public Optional<Invoice> findById(Long id) {
        List<Invoice> list = jdbc.query(
                "SELECT i.*, (c.first_name || ' ' || c.last_name) AS customer_name, b.booking_type " +
                "FROM invoices i JOIN customers c ON i.customer_id = c.id JOIN bookings b ON i.booking_id = b.id " +
                "WHERE i.id = ?", rowMapperFull, id);
        return list.stream().findFirst();
    }

    public Invoice save(Invoice inv) {
        if (inv.getId() == null) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbc.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO invoices (booking_id, customer_id, invoice_number, amount, tax_rate, tax_amount, total, status, due_date, notes) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
                ps.setLong(1, inv.getBookingId());
                ps.setLong(2, inv.getCustomerId());
                ps.setString(3, inv.getInvoiceNumber());
                ps.setDouble(4, inv.getAmount());
                ps.setDouble(5, inv.getTaxRate());
                ps.setDouble(6, inv.getTaxAmount());
                ps.setDouble(7, inv.getTotal());
                ps.setString(8, inv.getStatus());
                ps.setString(9, inv.getDueDate());
                ps.setString(10, inv.getNotes());
                return ps;
            }, keyHolder);
            inv.setId(keyHolder.getKey().longValue());
        } else {
            jdbc.update("UPDATE invoices SET status = ?, paid_date = ?, notes = ? WHERE id = ?",
                    inv.getStatus(), inv.getPaidDate(), inv.getNotes(), inv.getId());
        }
        return inv;
    }

    public String nextInvoiceNumber() {
        String year = java.time.Year.now().toString();
        String prefix = "RE-" + year + "-";
        Long maxNum = jdbc.queryForObject(
                "SELECT MAX(CAST(SUBSTR(invoice_number, " + (prefix.length() + 1) + ") AS INTEGER)) FROM invoices WHERE invoice_number LIKE ?",
                Long.class, prefix + "%");
        long next = (maxNum != null ? maxNum : 0) + 1;
        return String.format("RE-%s-%04d", year, next);
    }
}
