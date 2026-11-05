
package com.example.Blogging_Platform_II.repository;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

import com.example.Blogging_Platform_II.model.Blog.Comment;
import com.example.Blogging_Platform_II.model.Blog.Review;
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

    public Optional<Comment> comment(long postId, String id, boolean archived) {
        return Optional.ofNullable(mongo.findOne(query(commentCriteria(postId, id, archived)), Comment.class));
    }

    public Optional<Comment> archivedComment(String id) {
        return Optional.ofNullable(mongo.findOne(query(where("id").is(id).and("deletedAt").ne(null)), Comment.class));
    }

    public List<Comment> archived() {
        return mongo.find(query(where("deletedAt").ne(null)).with(Sort.by(Sort.Direction.DESC, "deletedAt")),
                Comment.class);
    }

    public Comment insert(Comment comment) {
        return mongo.insert(comment);
    }

    public Optional<Comment> edit(long postId, String id, String body) {
        return Optional.ofNullable(mongo.findAndModify(query(commentCriteria(postId, id, false)),
                Update.update("body", body), FindAndModifyOptions.options().returnNew(true), Comment.class));
    }

    public boolean archive(long postId, String id) {
        return mongo.updateFirst(query(commentCriteria(postId, id, false)), Update.update("deletedAt", Instant.now()),
                Comment.class).getModifiedCount() == 1;
    }

    public boolean restore(long postId, String id) {
        return mongo.updateFirst(query(commentCriteria(postId, id, true)), new Update().unset("deletedAt"),
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

    private Criteria commentCriteria(long postId, String id, boolean archived) {
        Criteria criteria = where("postId").is(postId).and("id").is(id).and("deletedAt");
        return archived ? criteria.ne(null) : criteria.isNull();
    }
}
