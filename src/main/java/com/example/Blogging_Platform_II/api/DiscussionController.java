
package com.example.Blogging_Platform_II.api;

import static com.example.Blogging_Platform_II.api.ApiEnvelope.ok;

import com.example.Blogging_Platform_II.api.Requests.CommentInput;
import com.example.Blogging_Platform_II.api.Requests.ReviewInput;
import com.example.Blogging_Platform_II.model.Blog.Comment;
import com.example.Blogging_Platform_II.model.Blog.Discussion;
import com.example.Blogging_Platform_II.model.Blog.Review;
import com.example.Blogging_Platform_II.service.DiscussionService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/posts/{postId}")
public class DiscussionController {
    private final DiscussionService discussion;

    public DiscussionController(DiscussionService discussion) {
        this.discussion = discussion;
    }

    @GetMapping("/comments")
    ApiEnvelope<List<Comment>> comments(@PathVariable long postId) {
        return ok("Comments found.", discussion.comments(postId));
    }

    @GetMapping("/comments/{id}")
    ApiEnvelope<Comment> comment(@PathVariable long postId, @PathVariable String id) {
        return ok("Comment found.", discussion.comment(postId, id));
    }

    @PostMapping("/comments")
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirement(name = "basicAuth")
    ApiEnvelope<Comment> createComment(@PathVariable long postId, @Valid @RequestBody CommentInput input) {
        return ok("Comment created.", discussion.addComment(postId, input));
    }

    @PutMapping("/comments/{id}")
    @SecurityRequirement(name = "basicAuth")
    ApiEnvelope<Comment> updateComment(@PathVariable long postId, @PathVariable String id,
                                       @Valid @RequestBody CommentInput input) {
        return ok("Comment updated.", discussion.editComment(postId, id, input));
    }

    @DeleteMapping("/comments/{id}")
    @SecurityRequirement(name = "basicAuth")
    ApiEnvelope<Void> archiveComment(@PathVariable long postId, @PathVariable String id) {
        discussion.archiveComment(postId, id);
        return ok("Comment moved to Trash.", null);
    }

    @GetMapping("/reviews")
    ApiEnvelope<Discussion> reviews(@PathVariable long postId) {
        return ok("Reviews found.", discussion.reviews(postId));
    }

    @PostMapping("/reviews")
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirement(name = "basicAuth")
    ApiEnvelope<Review> createReview(@PathVariable long postId, @Valid @RequestBody ReviewInput input) {
        return ok("Review created.", discussion.addReview(postId, input));
    }
}
