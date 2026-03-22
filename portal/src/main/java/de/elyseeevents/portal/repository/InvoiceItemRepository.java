package de.elyseeevents.portal.repository;

import de.elyseeevents.portal.model.InvoiceItem;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class InvoiceItemRepository {

    private final JdbcTemplate jdbc;

    private final RowMapper<InvoiceItem> rowMapper = (rs, rowNum) -> {
        InvoiceItem i = new InvoiceItem();
        i.setId(rs.getLong("id"));
        i.setInvoiceId(rs.getLong("invoice_id"));
        i.setDescription(rs.getString("description"));
        i.setQuantity(rs.getDouble("quantity"));
        i.setUnitPrice(rs.getDouble("unit_price"));
        i.setTotal(rs.getDouble("total"));
        return i;
    };

    public InvoiceItemRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<InvoiceItem> findByInvoiceId(Long invoiceId) {
        return jdbc.query("SELECT * FROM invoice_items WHERE invoice_id = ? ORDER BY id", rowMapper, invoiceId);
    }

    public void save(InvoiceItem item) {
        jdbc.update("INSERT INTO invoice_items (invoice_id, description, quantity, unit_price, total) VALUES (?, ?, ?, ?, ?)",
                item.getInvoiceId(), item.getDescription(), item.getQuantity(), item.getUnitPrice(), item.getTotal());
    }

    public void deleteByInvoiceId(Long invoiceId) {
        jdbc.update("DELETE FROM invoice_items WHERE invoice_id = ?", invoiceId);
    }
}
