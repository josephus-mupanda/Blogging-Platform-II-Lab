package com.example.Blogging_Platform_II.controller.graphql;

import com.example.Blogging_Platform_II.dto.PostInput;
import com.example.Blogging_Platform_II.model.Post;
import com.example.Blogging_Platform_II.model.PostFilter;
import com.example.Blogging_Platform_II.model.PostPage;
import com.example.Blogging_Platform_II.service.PostService;
import jakarta.validation.Valid;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

@Controller
public class PostGraphQlController {
    private final PostService posts;

    public PostGraphQlController(PostService posts) {
        this.posts = posts;
    }

    @QueryMapping
    public PostPage posts(@Argument PostFilter filter) {
        return posts.search(filter);
    }

    @QueryMapping
    public Post post(@Argument long id) {
        return posts.get(id);
    }

    @MutationMapping
    public Post createPost(@Argument long authorId, @Argument @Valid PostInput input) {
        return posts.create(authorId, input.title(), input.body(), input.tagIds());
    }

    @MutationMapping
    public Post updatePost(@Argument long id, @Argument @Valid PostInput input) {
        return posts.update(id, input.title(), input.body(), input.tagIds());
    }

    @MutationMapping
    public boolean deletePost(@Argument long id) {
        posts.archive(id);
        return true;
    }
}
