package de.elyseeevents.portal.repository;

import de.elyseeevents.portal.model.NewsletterSubscriber;
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
public class NewsletterRepository {

    private final JdbcTemplate jdbc;

    private final RowMapper<NewsletterSubscriber> rowMapper = (rs, rowNum) -> {
        NewsletterSubscriber s = new NewsletterSubscriber();
        s.setId(rs.getLong("id"));
        s.setEmail(rs.getString("email"));
        s.setName(rs.getString("name"));
        s.setActive(rs.getInt("active") == 1);
        s.setToken(rs.getString("token"));
        s.setCreatedAt(rs.getString("created_at"));
        return s;
    };

    public NewsletterRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<NewsletterSubscriber> findAll() {
        return jdbc.query("SELECT * FROM newsletter_subscribers ORDER BY created_at DESC", rowMapper);
    }

    public List<NewsletterSubscriber> findActive() {
        return jdbc.query("SELECT * FROM newsletter_subscribers WHERE active = 1 ORDER BY created_at DESC", rowMapper);
    }

    public Optional<NewsletterSubscriber> findByEmail(String email) {
        List<NewsletterSubscriber> list = jdbc.query(
                "SELECT * FROM newsletter_subscribers WHERE email = ?", rowMapper, email);
        return list.stream().findFirst();
    }

    public Optional<NewsletterSubscriber> findById(long id) {
        List<NewsletterSubscriber> list = jdbc.query(
                "SELECT * FROM newsletter_subscribers WHERE id = ?", rowMapper, id);
        return list.stream().findFirst();
    }

    public NewsletterSubscriber save(NewsletterSubscriber s) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO newsletter_subscribers (email, name, active, token) VALUES (?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, s.getEmail());
            ps.setString(2, s.getName());
            ps.setInt(3, s.isActive() ? 1 : 0);
            ps.setString(4, s.getToken());
            return ps;
        }, keyHolder);
        s.setId(keyHolder.getKey().longValue());
        return s;
    }

    public void reactivate(Long id, String name, String newToken) {
        jdbc.update("UPDATE newsletter_subscribers SET active = 1, name = ?, token = ? WHERE id = ?",
                name, newToken, id);
    }

    public void unsubscribeById(long id) {
        jdbc.update("UPDATE newsletter_subscribers SET active = 0 WHERE id = ?", id);
    }

    public long count() {
        Long c = jdbc.queryForObject("SELECT COUNT(*) FROM newsletter_subscribers", Long.class);
        return c != null ? c : 0;
    }

    public long countActive() {
        Long c = jdbc.queryForObject("SELECT COUNT(*) FROM newsletter_subscribers WHERE active = 1", Long.class);
        return c != null ? c : 0;
    }
}
