package de.elyseeevents.portal.repository;

import de.elyseeevents.portal.model.Document;
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
public class DocumentRepository {

    private final JdbcTemplate jdbc;

    private final RowMapper<Document> rowMapper = (rs, rowNum) -> {
        Document d = new Document();
        d.setId(rs.getLong("id"));
        d.setCustomerId(rs.getLong("customer_id"));
        d.setBookingId(rs.getObject("booking_id") != null ? rs.getLong("booking_id") : null);
        d.setUploadedBy(rs.getString("uploaded_by"));
        d.setFileName(rs.getString("file_name"));
        d.setFilePath(rs.getString("file_path"));
        d.setFileSize(rs.getLong("file_size"));
        d.setFileType(rs.getString("file_type"));
        d.setDescription(rs.getString("description"));
        d.setCreatedAt(rs.getString("created_at"));
        return d;
    };

    private final RowMapper<Document> rowMapperFull = (rs, rowNum) -> {
        Document d = rowMapper.mapRow(rs, rowNum);
        d.setCustomerName(rs.getString("customer_name"));
        return d;
    };

    public DocumentRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Document> findAll() {
        return jdbc.query(
                "SELECT d.*, CONCAT(c.first_name, ' ', c.last_name) AS customer_name " +
                "FROM documents d JOIN customers c ON d.customer_id = c.id " +
                "ORDER BY d.created_at DESC", rowMapperFull);
    }

    public List<Document> findByCustomerId(Long customerId) {
        return jdbc.query(
                "SELECT d.*, CONCAT(c.first_name, ' ', c.last_name) AS customer_name " +
                "FROM documents d JOIN customers c ON d.customer_id = c.id " +
                "WHERE d.customer_id = ? ORDER BY d.created_at DESC", rowMapperFull, customerId);
    }

    public List<Document> findByBookingId(Long bookingId) {
        return jdbc.query(
                "SELECT d.*, CONCAT(c.first_name, ' ', c.last_name) AS customer_name " +
                "FROM documents d JOIN customers c ON d.customer_id = c.id " +
                "WHERE d.booking_id = ? ORDER BY d.created_at DESC", rowMapperFull, bookingId);
    }

    public Optional<Document> findById(Long id) {
        List<Document> list = jdbc.query(
                "SELECT d.*, CONCAT(c.first_name, ' ', c.last_name) AS customer_name " +
                "FROM documents d JOIN customers c ON d.customer_id = c.id " +
                "WHERE d.id = ?", rowMapperFull, id);
        return list.stream().findFirst();
    }

    public Document save(Document d) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO documents (customer_id, booking_id, uploaded_by, file_name, file_path, file_size, file_type, description) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, d.getCustomerId());
            if (d.getBookingId() != null) { ps.setLong(2, d.getBookingId()); } else { ps.setNull(2, Types.BIGINT); }
            ps.setString(3, d.getUploadedBy());
            ps.setString(4, d.getFileName());
            ps.setString(5, d.getFilePath());
            ps.setLong(6, d.getFileSize());
            ps.setString(7, d.getFileType());
            ps.setString(8, d.getDescription());
            return ps;
        }, keyHolder);
        d.setId(keyHolder.getKey().longValue());
        return d;
    }

    public void deleteById(Long id) {
        jdbc.update("DELETE FROM documents WHERE id = ?", id);
    }
}
