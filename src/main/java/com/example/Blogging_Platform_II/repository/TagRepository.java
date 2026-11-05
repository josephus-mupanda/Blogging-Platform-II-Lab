
package com.example.Blogging_Platform_II.repository;

import com.example.Blogging_Platform_II.model.Blog.Tag;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class TagRepository {
    private static final RowMapper<Tag> TAG = (row, number) -> new Tag(row.getLong("tag_id"), row.getString("name"));
    private final JdbcClient jdbc;

    public TagRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public List<Tag> list() {
        return jdbc.sql("SELECT tag_id, name FROM tags WHERE deleted_at IS NULL ORDER BY name, tag_id").query(TAG).list();
    }

    public Optional<Tag> find(long id) {
        return jdbc.sql("SELECT tag_id, name FROM tags WHERE tag_id = :id AND deleted_at IS NULL")
                .param("id", id).query(TAG).optional();
    }

    public Tag insert(String name) {
        return jdbc.sql("INSERT INTO tags (name) VALUES (:name) RETURNING tag_id, name")
                .param("name", name).query(TAG).single();
    }

    public Optional<Tag> update(long id, String name) {
        return jdbc.sql("""
                UPDATE tags SET name = :name WHERE tag_id = :id AND deleted_at IS NULL RETURNING tag_id, name
                """).param("name", name).param("id", id).query(TAG).optional();
    }

    public boolean archive(long id) {
        return jdbc.sql("UPDATE tags SET deleted_at = now() WHERE tag_id = :id AND deleted_at IS NULL")
                .param("id", id).update() == 1;
    }

    public boolean allVisible(List<Long> ids) {
        if (ids.isEmpty()) {
            return true;
        }
        return jdbc.sql("SELECT tag_id FROM tags WHERE tag_id IN (:ids) AND deleted_at IS NULL ORDER BY tag_id FOR SHARE")
                .param("ids", ids).query(Long.class).list().size() == ids.size();
    }

    public long countVisible() {
        return jdbc.sql("SELECT count(*) FROM tags WHERE deleted_at IS NULL").query(Long.class).single();
    }
}
