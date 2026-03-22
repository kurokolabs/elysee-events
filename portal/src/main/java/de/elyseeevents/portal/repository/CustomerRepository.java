package de.elyseeevents.portal.repository;

import de.elyseeevents.portal.model.Customer;
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
public class CustomerRepository {

    private final JdbcTemplate jdbc;

    private final RowMapper<Customer> rowMapper = (rs, rowNum) -> {
        Customer c = new Customer();
        c.setId(rs.getLong("id"));
        c.setUserId(rs.getLong("user_id"));
        c.setFirstName(rs.getString("first_name"));
        c.setLastName(rs.getString("last_name"));
        c.setCompany(rs.getString("company"));
        c.setPhone(rs.getString("phone"));
        c.setAddress(rs.getString("address"));
        c.setPostalCode(rs.getString("postal_code"));
        c.setCity(rs.getString("city"));
        c.setNotes(rs.getString("notes"));
        c.setCreatedAt(rs.getString("created_at"));
        c.setUpdatedAt(rs.getString("updated_at"));
        return c;
    };

    private final RowMapper<Customer> rowMapperWithEmail = (rs, rowNum) -> {
        Customer c = rowMapper.mapRow(rs, rowNum);
        c.setEmail(rs.getString("email"));
        return c;
    };

    public CustomerRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Customer> findAll() {
        return jdbc.query(
                "SELECT c.*, u.email FROM customers c JOIN users u ON c.user_id = u.id ORDER BY c.last_name, c.first_name",
                rowMapperWithEmail);
    }

    public Optional<Customer> findById(Long id) {
        List<Customer> list = jdbc.query(
                "SELECT c.*, u.email FROM customers c JOIN users u ON c.user_id = u.id WHERE c.id = ?",
                rowMapperWithEmail, id);
        return list.stream().findFirst();
    }

    public Optional<Customer> findByUserId(Long userId) {
        List<Customer> list = jdbc.query(
                "SELECT c.*, u.email FROM customers c JOIN users u ON c.user_id = u.id WHERE c.user_id = ?",
                rowMapperWithEmail, userId);
        return list.stream().findFirst();
    }

    public List<Customer> search(String query) {
        String like = "%" + query.toLowerCase() + "%";
        return jdbc.query(
                "SELECT c.*, u.email FROM customers c JOIN users u ON c.user_id = u.id " +
                "WHERE LOWER(c.first_name) LIKE ? OR LOWER(c.last_name) LIKE ? OR LOWER(c.company) LIKE ? OR LOWER(c.city) LIKE ? OR LOWER(u.email) LIKE ? " +
                "ORDER BY c.last_name",
                rowMapperWithEmail, like, like, like, like, like);
    }

    public Customer save(Customer c) {
        if (c.getId() == null) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbc.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO customers (user_id, first_name, last_name, company, phone, address, postal_code, city, notes) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
                ps.setLong(1, c.getUserId());
                ps.setString(2, c.getFirstName());
                ps.setString(3, c.getLastName());
                ps.setString(4, c.getCompany());
                ps.setString(5, c.getPhone());
                ps.setString(6, c.getAddress());
                ps.setString(7, c.getPostalCode());
                ps.setString(8, c.getCity());
                ps.setString(9, c.getNotes());
                return ps;
            }, keyHolder);
            c.setId(keyHolder.getKey().longValue());
        } else {
            jdbc.update("UPDATE customers SET first_name = ?, last_name = ?, company = ?, phone = ?, address = ?, postal_code = ?, city = ?, notes = ?, updated_at = datetime('now') WHERE id = ?",
                    c.getFirstName(), c.getLastName(), c.getCompany(), c.getPhone(),
                    c.getAddress(), c.getPostalCode(), c.getCity(), c.getNotes(), c.getId());
        }
        return c;
    }

    public long count() {
        Long count = jdbc.queryForObject("SELECT COUNT(*) FROM customers", Long.class);
        return count != null ? count : 0;
    }
}
