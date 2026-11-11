
package com.example.Blogging_Platform_II.service;

import com.example.Blogging_Platform_II.exception.BlogException;
import com.example.Blogging_Platform_II.model.Post;
import com.example.Blogging_Platform_II.model.PostFilter;
import com.example.Blogging_Platform_II.model.PostPage;
import com.example.Blogging_Platform_II.model.Sort;
import com.example.Blogging_Platform_II.repository.PostRepository;
import com.example.Blogging_Platform_II.repository.TagRepository;
import com.example.Blogging_Platform_II.repository.UserRepository;
import java.util.List;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PostService {
    private final PostRepository posts;
    private final TagRepository tags;
    private final UserRepository users;

    public PostService(PostRepository posts, TagRepository tags, UserRepository users) {
        this.posts = posts;
        this.tags = tags;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public PostPage search(PostFilter filter) {
        if (filter == null) {
            return posts.search(null, null, null, 0, 10, Sort.NEWEST);
        }
        int page = filter.getPage() == null ? 0 : filter.getPage();
        int size = filter.getSize() == null ? 10 : filter.getSize();
        if (page < 0 || size < 1 || size > 100) {
            throw BlogException.badRequest("Use a nonnegative page and a page size between 1 and 100.");
        }
        if (filter.getKeyword() != null && filter.getKeyword().length() > 200) {
            throw BlogException.badRequest("Search keywords must be at most 200 characters.");
        }
        if ((filter.getAuthorId() != null && filter.getAuthorId() <= 0)
                || (filter.getTagId() != null && filter.getTagId() <= 0)) {
            throw BlogException.badRequest("Author and tag filters must be positive IDs.");
        }
        return posts.search(filter.getKeyword(), filter.getAuthorId(), filter.getTagId(), page, size,
                filter.getSort() == null ? Sort.NEWEST : filter.getSort());
    }

    @Transactional(readOnly = true)
    public Post get(long id) {
        return posts.find(id).orElseThrow(() -> BlogException.notFound("Post"));
    }

    @Transactional
    public Post create(long authorId, String title, String body, List<Long> tagIds) {
        users.find(authorId).orElseThrow(() -> BlogException.notFound("Author"));
        List<Long> selected = visibleTags(tagIds);
        try {
            long id = posts.insert(authorId, title, body);
            posts.replaceTags(id, selected);
            return get(id);
        } catch (DuplicateKeyException exception) {
            throw BlogException.duplicateValue();
        }
    }

    @Transactional
    public Post update(long id, String title, String body, List<Long> tagIds) {
        requireVisible(id);
        List<Long> selected = visibleTags(tagIds);
        try {
            posts.update(id, title, body);
            posts.replaceTags(id, selected);
            return get(id);
        } catch (DuplicateKeyException exception) {
            throw BlogException.duplicateValue();
        }
    }

    @Transactional
    public void archive(long id) {
        requireVisible(id);
        posts.archive(id);
    }

    private void requireVisible(long id) {
        if (!posts.existsVisible(id)) {
            throw BlogException.notFound("Post");
        }
    }

    private List<Long> visibleTags(List<Long> tagIds) {
        List<Long> selected = tagIds == null ? List.of() : tagIds.stream().distinct().toList();
        if (!tags.allVisible(selected)) {
            throw BlogException.badRequest("Select tags that are available.");
        }
        return selected;
    }
}
