
package com.example.Blogging_Platform_II.repository;

import com.example.Blogging_Platform_II.model.Blog.Account;
import com.example.Blogging_Platform_II.model.Blog.Role;
import com.example.Blogging_Platform_II.model.Blog.User;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepository {
    private static final String COLUMNS = "user_id, username, display_name, role, active";
    private static final RowMapper<User> USER = (row, number) -> new User(row.getLong("user_id"),
            row.getString("username"), row.getString("display_name"), Role.valueOf(row.getString("role")),
            row.getBoolean("active"));
    private final JdbcClient jdbc;

    public UserRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<Account> account(String username) {
        return jdbc.sql("SELECT " + COLUMNS + ", password_hash FROM users WHERE username = :username")
                .param("username", username)
                .query((row, number) -> new Account(USER.mapRow(row, number),
                        row.getString("password_hash"))).optional();
    }

    public Optional<User> find(long id) {
        return jdbc.sql("SELECT " + COLUMNS + " FROM users WHERE user_id = :id")
                .param("id", id).query(USER).optional();
    }

    public List<User> list() {
        return jdbc.sql("SELECT " + COLUMNS + " FROM users ORDER BY username, user_id").query(USER).list();
    }

    public boolean usernameExists(String username) {
        return jdbc.sql("SELECT EXISTS(SELECT 1 FROM users WHERE username = :value)")
                .param("value", username).query(Boolean.class).single();
    }

    public boolean emailExists(String email, long excludedId) {
        return jdbc.sql("SELECT EXISTS(SELECT 1 FROM users WHERE email = :value AND user_id <> :id)")
                .param("value", email).param("id", excludedId).query(Boolean.class).single();
    }

    public User insert(String username, String email, String hash, String name, Role role) {
        return jdbc.sql("""
                INSERT INTO users (username, email, password_hash, display_name, role)
                VALUES (:username, :email, :hash, :name, :role) RETURNING
                """ + COLUMNS).param("username", username).param("email", email).param("hash", hash)
                .param("name", name).param("role", role.name()).query(USER).single();
    }

    public Optional<User> update(long id, String email, String name) {
        return jdbc.sql("UPDATE users SET email = :email, display_name = :name WHERE user_id = :id RETURNING " + COLUMNS)
                .param("id", id).param("email", email).param("name", name).query(USER).optional();
    }

    public Optional<User> deactivate(long id) {
        return jdbc.sql("""
                UPDATE users SET active = false WHERE user_id = :id AND role <> 'ADMIN' RETURNING
                """ + COLUMNS).param("id", id).query(USER).optional();
    }

    public long count() {
        return jdbc.sql("SELECT count(*) FROM users").query(Long.class).single();
    }
}
