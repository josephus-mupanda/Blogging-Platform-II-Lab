
package com.example.Blogging_Platform_II.api;

import static com.example.Blogging_Platform_II.api.ApiEnvelope.ok;

import com.example.Blogging_Platform_II.api.Requests.TagInput;
import com.example.Blogging_Platform_II.model.Blog.Tag;
import com.example.Blogging_Platform_II.service.TagService;
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
    @SecurityRequirement(name = "basicAuth")
    ApiEnvelope<Tag> create(@Valid @RequestBody TagInput input) {
        return ok("Tag created.", tags.create(input));
    }

    @PutMapping("/{id}")
    @SecurityRequirement(name = "basicAuth")
    ApiEnvelope<Tag> update(@PathVariable long id, @Valid @RequestBody TagInput input) {
        return ok("Tag updated.", tags.update(id, input));
    }

    @DeleteMapping("/{id}")
    @SecurityRequirement(name = "basicAuth")
    ApiEnvelope<Void> archive(@PathVariable long id) {
        tags.archive(id);
        return ok("Tag moved to Trash.", null);
    }
}
