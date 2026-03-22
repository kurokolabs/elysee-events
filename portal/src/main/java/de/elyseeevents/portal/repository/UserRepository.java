package de.elyseeevents.portal.repository;

import de.elyseeevents.portal.model.User;
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
public class UserRepository {

    private final JdbcTemplate jdbc;

    private final RowMapper<User> rowMapper = (rs, rowNum) -> {
        User u = new User();
        u.setId(rs.getLong("id"));
        u.setEmail(rs.getString("email"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setRole(rs.getString("role"));
        u.setActive(rs.getInt("active") == 1);
        u.setForcePwChange(rs.getInt("force_pw_change") == 1);
        u.setTwoFaEnabled(rs.getInt("two_fa_enabled") == 1);
        u.setTwoFaCode(rs.getString("two_fa_code"));
        u.setTwoFaExpires(rs.getString("two_fa_expires"));
        u.setCreatedAt(rs.getString("created_at"));
        u.setLastLogin(rs.getString("last_login"));
        return u;
    };

    public UserRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<User> findByEmail(String email) {
        List<User> users = jdbc.query("SELECT * FROM users WHERE email = ?", rowMapper, email);
        return users.stream().findFirst();
    }

    public Optional<User> findById(Long id) {
        List<User> users = jdbc.query("SELECT * FROM users WHERE id = ?", rowMapper, id);
        return users.stream().findFirst();
    }

    public User save(User user) {
        if (user.getId() == null) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbc.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO users (email, password_hash, role, active, force_pw_change, two_fa_enabled) VALUES (?, ?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
                ps.setString(1, user.getEmail());
                ps.setString(2, user.getPasswordHash());
                ps.setString(3, user.getRole());
                ps.setInt(4, user.isActive() ? 1 : 0);
                ps.setInt(5, user.isForcePwChange() ? 1 : 0);
                ps.setInt(6, user.isTwoFaEnabled() ? 1 : 0);
                return ps;
            }, keyHolder);
            user.setId(keyHolder.getKey().longValue());
        } else {
            jdbc.update("UPDATE users SET email = ?, password_hash = ?, role = ?, active = ?, force_pw_change = ?, two_fa_enabled = ? WHERE id = ?",
                    user.getEmail(), user.getPasswordHash(), user.getRole(),
                    user.isActive() ? 1 : 0, user.isForcePwChange() ? 1 : 0,
                    user.isTwoFaEnabled() ? 1 : 0, user.getId());
        }
        return user;
    }

    public void updateLastLogin(Long userId) {
        jdbc.update("UPDATE users SET last_login = datetime('now') WHERE id = ?", userId);
    }

    public void updatePassword(Long userId, String passwordHash) {
        jdbc.update("UPDATE users SET password_hash = ?, force_pw_change = 0 WHERE id = ?", passwordHash, userId);
    }

    public void storeTwoFaCode(Long userId, String code, String expiresAt) {
        jdbc.update("UPDATE users SET two_fa_code = ?, two_fa_expires = ? WHERE id = ?", code, expiresAt, userId);
    }

    public void clearTwoFaCode(Long userId) {
        jdbc.update("UPDATE users SET two_fa_code = NULL, two_fa_expires = NULL WHERE id = ?", userId);
    }
}
