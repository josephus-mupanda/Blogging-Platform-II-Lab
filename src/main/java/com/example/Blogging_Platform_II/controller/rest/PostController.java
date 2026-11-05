package com.example.Blogging_Platform_II.controller.rest;

import static com.example.Blogging_Platform_II.controller.rest.ApiEnvelope.ok;

import com.example.Blogging_Platform_II.dto.PostInput;
import com.example.Blogging_Platform_II.model.Post;
import com.example.Blogging_Platform_II.model.PostPage;
import com.example.Blogging_Platform_II.model.PostFilter;
import com.example.Blogging_Platform_II.service.PostService;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/posts")
public class PostController {
    private final PostService posts;

    public PostController(PostService posts) {
        this.posts = posts;
    }

    @GetMapping
    ApiEnvelope<PostPage> list(@ParameterObject @ModelAttribute PostFilter filter) {
        return ok("Posts found.", posts.search(filter));
    }

    @GetMapping("/{id}")
    ApiEnvelope<Post> get(@PathVariable long id) {
        return ok("Post found.", posts.get(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ApiEnvelope<Post> create(@RequestParam long authorId, @Valid @RequestBody PostInput input) {
        return ok("Post created.", posts.create(authorId, input.title(), input.body(), input.tagIds()));
    }

    @PutMapping("/{id}")
    ApiEnvelope<Post> update(@PathVariable long id, @Valid @RequestBody PostInput input) {
        return ok("Post updated.", posts.update(id, input.title(), input.body(), input.tagIds()));
    }

    @DeleteMapping("/{id}")
    ApiEnvelope<Void> archive(@PathVariable long id) {
        posts.archive(id);
        return ok("Post deleted.", null);
    }
}
