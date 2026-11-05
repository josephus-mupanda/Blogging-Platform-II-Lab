package com.example.Blogging_Platform_II.controller.rest;

import static com.example.Blogging_Platform_II.controller.rest.ApiEnvelope.ok;

import com.example.Blogging_Platform_II.dto.TagInput;
import com.example.Blogging_Platform_II.model.Tag;
import com.example.Blogging_Platform_II.service.TagService;
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
@RequestMapping("/api/tags")
public class TagController {
    private final TagService tags;

    public TagController(TagService tags) {
        this.tags = tags;
    }

    @GetMapping
    ApiEnvelope<List<Tag>> list() {
        return ok("Tags found.", tags.list());
    }

    @GetMapping("/{id}")
    ApiEnvelope<Tag> get(@PathVariable long id) {
        return ok("Tag found.", tags.get(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ApiEnvelope<Tag> create(@Valid @RequestBody TagInput input) {
        return ok("Tag created.", tags.create(input.name()));
    }

    @PutMapping("/{id}")
    ApiEnvelope<Tag> update(@PathVariable long id, @Valid @RequestBody TagInput input) {
        return ok("Tag updated.", tags.update(id, input.name()));
    }

    @DeleteMapping("/{id}")
    ApiEnvelope<Void> archive(@PathVariable long id) {
        tags.archive(id);
        return ok("Tag deleted.", null);
    }
}
