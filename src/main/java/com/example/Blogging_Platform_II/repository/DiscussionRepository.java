
package com.example.Blogging_Platform_II.repository;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

import com.example.Blogging_Platform_II.model.Comment;
import com.example.Blogging_Platform_II.model.Review;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

@Repository
public class DiscussionRepository {
    private static final Sort OLDEST = Sort.by("createdAt", "id");
    private final MongoTemplate mongo;

    public DiscussionRepository(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    public List<Comment> comments(long postId) {
        return mongo.find(query(where("postId").is(postId).and("deletedAt").isNull()).with(OLDEST), Comment.class);
    }

    public Optional<Comment> comment(long postId, String id) {
        return Optional.ofNullable(mongo.findOne(query(visibleComment(postId, id)), Comment.class));
    }

    public Comment insert(Comment comment) {
        return mongo.insert(comment);
    }

    public Optional<Comment> edit(long postId, String id, String body) {
        return Optional.ofNullable(mongo.findAndModify(query(visibleComment(postId, id)),
                Update.update("body", body), FindAndModifyOptions.options().returnNew(true), Comment.class));
    }

    public boolean archive(long postId, String id) {
        return mongo.updateFirst(query(visibleComment(postId, id)), Update.update("deletedAt", Instant.now()),
                Comment.class).getModifiedCount() == 1;
    }

    public List<Review> reviews(long postId) {
        return mongo.find(query(where("postId").is(postId)).with(OLDEST), Review.class);
    }

    public Review insert(Review review) {
        return mongo.insert(review);
    }

    public long countComments(List<Long> archivedIds) {
        return mongo.count(query(where("postId").nin(archivedIds).and("deletedAt").isNull()), Comment.class);
    }

    public long countReviews(List<Long> archivedIds) {
        return mongo.count(query(where("postId").nin(archivedIds)), Review.class);
    }

    private Criteria visibleComment(long postId, String id) {
        return where("postId").is(postId).and("id").is(id).and("deletedAt").isNull();
    }
}
