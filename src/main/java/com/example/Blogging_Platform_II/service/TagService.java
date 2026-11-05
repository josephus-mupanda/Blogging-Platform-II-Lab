
package com.example.Blogging_Platform_II.service;

import com.example.Blogging_Platform_II.api.ApiException;
import com.example.Blogging_Platform_II.api.Requests.TagInput;
import com.example.Blogging_Platform_II.model.Blog.Tag;
import com.example.Blogging_Platform_II.repository.TagRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class TagService {
    private final TagRepository tags;
    private final Actors actors;

    public TagService(TagRepository tags, Actors actors) {
        this.tags = tags;
        this.actors = actors;
    }

    public List<Tag> list() {
        return tags.list();
    }

    public Tag get(long id) {
        return tags.find(id).orElseThrow(() -> ApiException.notFound("Tag"));
    }

    public Tag create(TagInput input) {
        actors.admin();
        return tags.insert(input.name());
    }

    public Tag update(long id, TagInput input) {
        actors.admin();
        return tags.update(id, input.name()).orElseThrow(() -> ApiException.notFound("Tag"));
    }

    public void archive(long id) {
        actors.admin();
        if (!tags.archive(id)) {
            throw ApiException.notFound("Tag");
        }
    }
}
