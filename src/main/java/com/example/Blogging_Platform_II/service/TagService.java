
package com.example.Blogging_Platform_II.service;

import com.example.Blogging_Platform_II.exception.BlogException;
import com.example.Blogging_Platform_II.model.Tag;
import com.example.Blogging_Platform_II.repository.TagRepository;
import java.util.List;
import org.springframework.dao.DuplicateKeyException;
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
        return tags.find(id).orElseThrow(() -> BlogException.notFound("Tag"));
    }

    public Tag create(String name) {
        try {
            return tags.insert(name);
        } catch (DuplicateKeyException exception) {
            throw BlogException.duplicateValue();
        }
    }

    public Tag update(long id, String name) {
        try {
            return tags.update(id, name).orElseThrow(() -> BlogException.notFound("Tag"));
        } catch (DuplicateKeyException exception) {
            throw BlogException.duplicateValue();
        }
    }

    public void archive(long id) {
        if (!tags.archive(id)) {
            throw BlogException.notFound("Tag");
        }
    }
}
