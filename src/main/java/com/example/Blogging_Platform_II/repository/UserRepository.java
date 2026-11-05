
package com.example.Blogging_Platform_II.repository;

import com.example.Blogging_Platform_II.model.User;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepository {
    private static final String COLUMNS = "user_id, username, display_name";
    private static final String BY_ID = "SELECT " + COLUMNS + " FROM users WHERE user_id = :id AND active";
    private static final RowMapper<User> USER = (row, number) -> new User(row.getLong("user_id"),
            row.getString("username"), row.getString("display_name"));
    private final JdbcClient jdbc;

    public UserRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<User> find(long id) {
        return jdbc.sql(BY_ID).param("id", id).query(USER).optional();
    }

    public Optional<User> findForContent(long id) {
        return jdbc.sql(BY_ID + " FOR SHARE").param("id", id).query(USER).optional();
    }

    public List<User> list() {
        return jdbc.sql("SELECT " + COLUMNS + " FROM users WHERE active ORDER BY username, user_id").query(USER).list();
    }

    public boolean usernameExists(String username) {
        return jdbc.sql("SELECT EXISTS(SELECT 1 FROM users WHERE username = :value)")
                .param("value", username).query(Boolean.class).single();
    }

    public boolean emailExists(String email, long excludedId) {
        return jdbc.sql("SELECT EXISTS(SELECT 1 FROM users WHERE email = :value AND user_id <> :id)")
                .param("value", email).param("id", excludedId).query(Boolean.class).single();
    }

    public User insert(String username, String email, String name) {
        return jdbc.sql("""
                INSERT INTO users (username, email, display_name)
                VALUES (:username, :email, :name) RETURNING
                """ + COLUMNS).param("username", username).param("email", email)
                .param("name", name).query(USER).single();
    }

    public Optional<User> update(long id, String email, String name) {
        return jdbc.sql("UPDATE users SET email = :email, display_name = :name WHERE user_id = :id AND active RETURNING "
                + COLUMNS)
                .param("id", id).param("email", email).param("name", name).query(USER).optional();
    }

    public boolean delete(long id) {
        return jdbc.sql("UPDATE users SET active = false WHERE user_id = :id AND active")
                .param("id", id).update() == 1;
    }

    public long count() {
        return jdbc.sql("SELECT count(*) FROM users WHERE active").query(Long.class).single();
    }
}
