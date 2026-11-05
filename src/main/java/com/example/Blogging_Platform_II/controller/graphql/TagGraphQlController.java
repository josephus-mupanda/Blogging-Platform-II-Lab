package com.example.Blogging_Platform_II.controller.graphql;

import com.example.Blogging_Platform_II.dto.TagInput;
import com.example.Blogging_Platform_II.model.Tag;
import com.example.Blogging_Platform_II.service.TagService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

@Controller
public class TagGraphQlController {
    private final TagService tags;

    public TagGraphQlController(TagService tags) {
        this.tags = tags;
    }

    @QueryMapping
    public List<Tag> tags() {
        return tags.list();
    }

    @QueryMapping
    public Tag tag(@Argument long id) {
        return tags.get(id);
    }

    @MutationMapping
    public Tag createTag(@Argument @Valid TagInput input) {
        return tags.create(input.name());
    }

    @MutationMapping
    public Tag updateTag(@Argument long id, @Argument @Valid TagInput input) {
        return tags.update(id, input.name());
    }

    @MutationMapping
    public boolean deleteTag(@Argument long id) {
        tags.archive(id);
        return true;
    }
}
