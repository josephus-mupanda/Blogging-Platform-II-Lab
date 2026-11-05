
package com.example.Blogging_Platform_II.model;

import java.time.Instant;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

public final class Blog {
    private Blog() {}

    public enum Role { ADMIN, AUTHOR, READER }
    public enum Sort { NEWEST, OLDEST, TITLE_ASC, TITLE_DESC }
    public enum Kind { POST, COMMENT, TAG }

    public record User(long id, String username, String displayName, Role role, boolean active) {}
    public record Account(User user, String passwordHash) {}
    public record Tag(long id, String name) {}
    public record Post(long id, long authorId, String title, String body, String createdAt,
                       String updatedAt, List<Tag> tags) {}
    public record PostPage(List<Post> items, int page, int size, long totalElements, long totalPages) {}
    public record PostFilter(String keyword, Long authorId, Long tagId, Integer page, Integer size, Sort sort) {}
    public record ArchivedItem(Kind kind, String id, String label, String deletedAt) {}
    public record TagUsage(String name, long posts) {}
    public record Dashboard(long users, long posts, long tags, long comments, long reviews,
                            List<Post> recentPosts, List<TagUsage> tagUsage) {}

    @Document("comments")
    @CompoundIndex(name = "postId_1_createdAt_1", def = "{'postId': 1, 'createdAt': 1}")
    public record Comment(@Id String id, long postId, long authorId, String authorName,
                          String body, Instant createdAt, Instant deletedAt) {}

    @Document("reviews")
    @CompoundIndex(name = "postId_1_reviewerId_1", def = "{'postId': 1, 'reviewerId': 1}", unique = true)
    public record Review(@Id String id, long postId, long reviewerId, String reviewerName,
                         int rating, String text, Instant createdAt) {}

    public record Discussion(List<Review> reviews, double averageRating) {}
}
