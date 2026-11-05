
package com.example.Blogging_Platform_II.service;

import com.example.Blogging_Platform_II.exception.BlogException;
import com.example.Blogging_Platform_II.model.Comment;
import com.example.Blogging_Platform_II.model.Discussion;
import com.example.Blogging_Platform_II.model.Review;
import com.example.Blogging_Platform_II.repository.DiscussionRepository;
import com.example.Blogging_Platform_II.repository.PostRepository;
import com.example.Blogging_Platform_II.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DiscussionService {
    private final DiscussionRepository discussion;
    private final PostRepository posts;
    private final UserRepository users;

    public DiscussionService(DiscussionRepository discussion, PostRepository posts, UserRepository users) {
        this.discussion = discussion;
        this.posts = posts;
        this.users = users;
    }

    public List<Comment> comments(long postId) {
        requireVisible(postId);
        return discussion.comments(postId);
    }

    public Comment comment(long postId, String id) {
        requireVisible(postId);
        return findComment(postId, id);
    }

    @Transactional
    public Comment addComment(long postId, long authorId, String body) {
        var author = users.findForContent(authorId).orElseThrow(() -> BlogException.notFound("Author"));
        lockVisible(postId);
        return discussion.insert(new Comment(null, postId, author.id(), author.displayName(),
                body, Instant.now(), null));
    }

    @Transactional
    public Comment editComment(long postId, String id, String body) {
        lockVisible(postId);
        return discussion.edit(postId, id, body).orElseThrow(() -> BlogException.notFound("Comment"));
    }

    @Transactional
    public void archiveComment(long postId, String id) {
        lockVisible(postId);
        if (!discussion.archive(postId, id)) {
            throw BlogException.notFound("Comment");
        }
    }

    public Discussion reviews(long postId) {
        requireVisible(postId);
        List<Review> reviews = discussion.reviews(postId);
        return new Discussion(reviews, reviews.stream().mapToInt(Review::rating).average().orElse(0));
    }

    @Transactional
    public Review addReview(long postId, long reviewerId, int rating, String text) {
        var reviewer = users.findForContent(reviewerId).orElseThrow(() -> BlogException.notFound("Reviewer"));
        lockVisible(postId);
        try {
            return discussion.insert(new Review(null, postId, reviewer.id(), reviewer.displayName(),
                    rating, text, Instant.now()));
        } catch (DuplicateKeyException exception) {
            throw BlogException.conflict("This user has already reviewed the post.");
        }
    }

    private Comment findComment(long postId, String id) {
        return discussion.comment(postId, id).orElseThrow(() -> BlogException.notFound("Comment"));
    }

    private void requireVisible(long postId) {
        if (!posts.existsVisible(postId)) {
            throw BlogException.notFound("Post");
        }
    }

    private void lockVisible(long postId) {
        if (!posts.lockVisible(postId)) {
            throw BlogException.notFound("Post");
        }
    }
}
