package com.example.Blogging_Platform_II.controller.rest;

import static com.example.Blogging_Platform_II.controller.rest.ApiResponse.ok;

import com.example.Blogging_Platform_II.dto.CommentInput;
import com.example.Blogging_Platform_II.dto.ReviewInput;
import com.example.Blogging_Platform_II.model.Comment;
import com.example.Blogging_Platform_II.model.Discussion;
import com.example.Blogging_Platform_II.model.Review;
import com.example.Blogging_Platform_II.service.DiscussionService;
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
import org.springframework.web.bind.annotation.RequestParam;
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
    ApiResponse<List<Comment>> comments(@PathVariable long postId) {
        return ok("Comments found.", discussion.comments(postId));
    }

    @GetMapping("/comments/{id}")
    ApiResponse<Comment> comment(@PathVariable long postId, @PathVariable String id) {
        return ok("Comment found.", discussion.comment(postId, id));
    }

    @PostMapping("/comments")
    @ResponseStatus(HttpStatus.CREATED)
    ApiResponse<Comment> createComment(@PathVariable long postId, @RequestParam long authorId,
                                       @Valid @RequestBody CommentInput input) {
        return ok("Comment created.", discussion.addComment(postId, authorId, input.getBody()));
    }

    @PutMapping("/comments/{id}")
    ApiResponse<Comment> updateComment(@PathVariable long postId, @PathVariable String id,
                                       @Valid @RequestBody CommentInput input) {
        return ok("Comment updated.", discussion.editComment(postId, id, input.getBody()));
    }

    @DeleteMapping("/comments/{id}")
    ApiResponse<Void> archiveComment(@PathVariable long postId, @PathVariable String id) {
        discussion.archiveComment(postId, id);
        return ok("Comment deleted.", null);
    }

    @GetMapping("/reviews")
    ApiResponse<Discussion> reviews(@PathVariable long postId) {
        return ok("Reviews found.", discussion.reviews(postId));
    }

    @PostMapping("/reviews")
    @ResponseStatus(HttpStatus.CREATED)
    ApiResponse<Review> createReview(@PathVariable long postId, @RequestParam long reviewerId,
                                     @Valid @RequestBody ReviewInput input) {
        return ok("Review created.", discussion.addReview(postId, reviewerId, input.getRating(), input.getText()));
    }
}
