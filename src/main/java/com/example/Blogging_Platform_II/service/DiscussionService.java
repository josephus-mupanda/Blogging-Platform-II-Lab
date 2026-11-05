
package com.example.Blogging_Platform_II.service;

import com.example.Blogging_Platform_II.api.ApiException;
import com.example.Blogging_Platform_II.api.Requests.CommentInput;
import com.example.Blogging_Platform_II.api.Requests.ReviewInput;
import com.example.Blogging_Platform_II.model.Blog.Comment;
import com.example.Blogging_Platform_II.model.Blog.Discussion;
import com.example.Blogging_Platform_II.model.Blog.Review;
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
    public Comment addComment(long postId, long authorId, CommentInput input) {
        var author = users.findForContent(authorId).orElseThrow(() -> ApiException.notFound("Author"));
        lockVisible(postId);
        return discussion.insert(new Comment(null, postId, author.id(), author.displayName(),
                input.body(), Instant.now(), null));
    }

    @Transactional
    public Comment editComment(long postId, String id, CommentInput input) {
        lockVisible(postId);
        return discussion.edit(postId, id, input.body()).orElseThrow(() -> ApiException.notFound("Comment"));
    }

    @Transactional
    public void archiveComment(long postId, String id) {
        lockVisible(postId);
        if (!discussion.archive(postId, id)) {
            throw ApiException.notFound("Comment");
        }
    }

    public Discussion reviews(long postId) {
        requireVisible(postId);
        List<Review> reviews = discussion.reviews(postId);
        return new Discussion(reviews, reviews.stream().mapToInt(Review::rating).average().orElse(0));
    }

    @Transactional
    public Review addReview(long postId, long reviewerId, ReviewInput input) {
        var reviewer = users.findForContent(reviewerId).orElseThrow(() -> ApiException.notFound("Reviewer"));
        lockVisible(postId);
        try {
            return discussion.insert(new Review(null, postId, reviewer.id(), reviewer.displayName(),
                    input.rating(), input.text(), Instant.now()));
        } catch (DuplicateKeyException exception) {
            throw ApiException.conflict("This user has already reviewed the post.");
        }
    }

    private Comment findComment(long postId, String id) {
        return discussion.comment(postId, id).orElseThrow(() -> ApiException.notFound("Comment"));
    }

    private void requireVisible(long postId) {
        if (!posts.existsVisible(postId)) {
            throw ApiException.notFound("Post");
        }
    }

    private void lockVisible(long postId) {
        if (!posts.lockVisible(postId)) {
            throw ApiException.notFound("Post");
        }
    }
}
