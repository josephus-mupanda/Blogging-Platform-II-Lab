
package com.example.Blogging_Platform_II.service;

import com.example.Blogging_Platform_II.exception.BlogException;
import com.example.Blogging_Platform_II.model.Comment;
import com.example.Blogging_Platform_II.model.Discussion;
import com.example.Blogging_Platform_II.model.Review;
import com.example.Blogging_Platform_II.model.User;
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
        User author = users.find(authorId).orElseThrow(() -> BlogException.notFound("Author"));
        requireVisible(postId);
        return discussion.insert(new Comment(null, postId, author.getId(), author.getDisplayName(),
                body, Instant.now(), null));
    }

    @Transactional
    public Comment editComment(long postId, String id, String body) {
        requireVisible(postId);
        return discussion.edit(postId, id, body).orElseThrow(() -> BlogException.notFound("Comment"));
    }

    @Transactional
    public void archiveComment(long postId, String id) {
        requireVisible(postId);
        if (!discussion.archive(postId, id)) {
            throw BlogException.notFound("Comment");
        }
    }

    public Discussion reviews(long postId) {
        requireVisible(postId);
        List<Review> reviews = discussion.reviews(postId);
        return new Discussion(reviews, reviews.stream().mapToInt(Review::getRating).average().orElse(0));
    }

    @Transactional
    public Review addReview(long postId, long reviewerId, int rating, String text) {
        User reviewer = users.find(reviewerId).orElseThrow(() -> BlogException.notFound("Reviewer"));
        requireVisible(postId);
        try {
            return discussion.insert(new Review(null, postId, reviewer.getId(), reviewer.getDisplayName(),
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
}
