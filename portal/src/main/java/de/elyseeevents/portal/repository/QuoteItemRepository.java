package de.elyseeevents.portal.repository;

import de.elyseeevents.portal.model.QuoteItem;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class QuoteItemRepository {

    private final JdbcTemplate jdbc;

    private final RowMapper<QuoteItem> rowMapper = (rs, rowNum) -> {
        QuoteItem i = new QuoteItem();
        i.setId(rs.getLong("id"));
        i.setQuoteId(rs.getLong("quote_id"));
        i.setDescription(rs.getString("description"));
        i.setQuantity(rs.getDouble("quantity"));
        i.setUnitPrice(rs.getDouble("unit_price"));
        i.setTotal(rs.getDouble("total"));
        try { i.setTaxType(rs.getString("tax_type")); } catch (Exception e) { i.setTaxType("GETRAENKE"); }
        return i;
    };

    public QuoteItemRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<QuoteItem> findByQuoteId(Long quoteId) {
        return jdbc.query("SELECT * FROM quote_items WHERE quote_id = ? ORDER BY id", rowMapper, quoteId);
    }

    public void save(QuoteItem item) {
        jdbc.update("INSERT INTO quote_items (quote_id, description, quantity, unit_price, total, tax_type) VALUES (?, ?, ?, ?, ?, ?)",
                item.getQuoteId(), item.getDescription(), item.getQuantity(), item.getUnitPrice(), item.getTotal(), item.getTaxType());
    }

    public void deleteByQuoteId(Long quoteId) {
        jdbc.update("DELETE FROM quote_items WHERE quote_id = ?", quoteId);
    }
}
