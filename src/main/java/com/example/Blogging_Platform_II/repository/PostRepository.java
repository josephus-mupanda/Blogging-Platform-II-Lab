
package com.example.Blogging_Platform_II.repository;

import com.example.Blogging_Platform_II.model.Blog.Post;
import com.example.Blogging_Platform_II.model.Blog.PostPage;
import com.example.Blogging_Platform_II.model.Blog.Sort;
import com.example.Blogging_Platform_II.model.Blog.Tag;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class PostRepository {
    private static final RowMapper<Post> POST = (row, number) -> new Post(row.getLong("post_id"),
            row.getLong("author_id"), row.getString("title"), row.getString("body"),
            row.getObject("created_at", LocalDateTime.class).toString(),
            row.getObject("updated_at", LocalDateTime.class).toString(), List.of());

    private final JdbcClient jdbc;

    public PostRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public PostPage search(String keyword, Long authorId, Long tagId, int page, int size, Sort sort) {
        StringBuilder where = new StringBuilder(" FROM posts p WHERE p.deleted_at IS NULL");
        Map<String, Object> parameters = new HashMap<>();
        if (keyword != null && !keyword.isBlank()) {
            where.append(" AND (p.title ILIKE :keyword OR p.body ILIKE :keyword)");
            parameters.put("keyword", "%" + keyword.strip().replace("\\", "\\\\")
                    .replace("%", "\\%").replace("_", "\\_") + "%");
        }
        if (authorId != null) {
            where.append(" AND p.author_id = :authorId");
            parameters.put("authorId", authorId);
        }
        if (tagId != null) {
            where.append("""
                     AND EXISTS (SELECT 1 FROM post_tags pt JOIN tags t USING (tag_id)
                                 WHERE pt.post_id = p.post_id AND t.tag_id = :tagId AND t.deleted_at IS NULL)
                    """);
            parameters.put("tagId", tagId);
        }
        String order = switch (sort) {
            case NEWEST -> "p.created_at DESC, p.post_id DESC";
            case OLDEST -> "p.created_at, p.post_id";
            case TITLE_ASC -> "lower(btrim(p.title)), p.post_id";
            case TITLE_DESC -> "lower(btrim(p.title)) DESC, p.post_id DESC";
        };
        long total = jdbc.sql("SELECT count(*)" + where).params(parameters).query(Long.class).single();
        List<Post> posts = jdbc.sql("SELECT p.*" + where + " ORDER BY " + order + " LIMIT :limit OFFSET :offset")
                .params(parameters).param("limit", size).param("offset", (long) page * size).query(POST).list();
        return new PostPage(withTags(posts), page, size, total, (total + size - 1) / size);
    }

    public Optional<Post> find(long id) {
        List<Post> rows = jdbc.sql("SELECT * FROM posts WHERE post_id = :id AND deleted_at IS NULL")
                .param("id", id).query(POST).list();
        return withTags(rows).stream().findFirst();
    }

    public boolean existsVisible(long id) {
        return jdbc.sql("SELECT EXISTS(SELECT 1 FROM posts WHERE post_id = :id AND deleted_at IS NULL)")
                .param("id", id).query(Boolean.class).single();
    }

    public boolean lockVisible(long id) {
        return jdbc.sql("SELECT post_id FROM posts WHERE post_id = :id AND deleted_at IS NULL FOR UPDATE")
                .param("id", id).query(Long.class).optional().isPresent();
    }

    public long insert(long authorId, String title, String body) {
        return jdbc.sql("INSERT INTO posts (author_id, title, body) VALUES (:author, :title, :body) RETURNING post_id")
                .param("author", authorId).param("title", title).param("body", body).query(Long.class).single();
    }

    public void update(long id, String title, String body) {
        jdbc.sql("UPDATE posts SET title = :title, body = :body, updated_at = now() WHERE post_id = :id")
                .param("id", id).param("title", title).param("body", body).update();
    }

    public void replaceTags(long id, List<Long> tags) {
        jdbc.sql("DELETE FROM post_tags WHERE post_id = :id").param("id", id).update();
        for (long tagId : tags) {
            jdbc.sql("INSERT INTO post_tags (post_id, tag_id) VALUES (:id, :tag)")
                    .param("id", id).param("tag", tagId).update();
        }
    }

    public void archive(long id) {
        jdbc.sql("UPDATE posts SET deleted_at = now() WHERE post_id = :id").param("id", id).update();
    }

    public long countVisible() {
        return jdbc.sql("SELECT count(*) FROM posts WHERE deleted_at IS NULL").query(Long.class).single();
    }

    public List<Long> archivedIds() {
        return jdbc.sql("SELECT post_id FROM posts WHERE deleted_at IS NOT NULL").query(Long.class).list();
    }

    private List<Post> withTags(List<Post> posts) {
        if (posts.isEmpty()) {
            return posts;
        }
        Map<Long, List<Tag>> tagsByPost = new HashMap<>();
        jdbc.sql("""
                SELECT pt.post_id, t.tag_id, t.name FROM post_tags pt JOIN tags t USING (tag_id)
                WHERE pt.post_id IN (:ids) AND t.deleted_at IS NULL ORDER BY t.name, t.tag_id
                """).param("ids", posts.stream().map(Post::id).toList()).query(row -> {
                    tagsByPost.computeIfAbsent(row.getLong("post_id"), ignored -> new ArrayList<>())
                            .add(new Tag(row.getLong("tag_id"), row.getString("name")));
                });
        return posts.stream().map(post -> new Post(post.id(), post.authorId(), post.title(), post.body(),
                post.createdAt(), post.updatedAt(), List.copyOf(tagsByPost.getOrDefault(post.id(), List.of())))).toList();
    }
}
