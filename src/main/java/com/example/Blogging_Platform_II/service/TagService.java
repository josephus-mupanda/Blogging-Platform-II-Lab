
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

    public TagService(TagRepository tags) {
        this.tags = tags;
    }

    public List<Tag> list() {
        return tags.list();
    }

    public Tag get(long id) {
        return tags.find(id).orElseThrow(() -> ApiException.notFound("Tag"));
    }

    public Tag create(TagInput input) {
        return tags.insert(input.name());
    }

    public Tag update(long id, TagInput input) {
        return tags.update(id, input.name()).orElseThrow(() -> ApiException.notFound("Tag"));
    }

    public void archive(long id) {
        if (!tags.archive(id)) {
            throw ApiException.notFound("Tag");
        }
    }
}
