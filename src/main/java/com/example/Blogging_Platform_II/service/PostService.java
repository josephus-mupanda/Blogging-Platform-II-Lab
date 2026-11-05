
package com.example.Blogging_Platform_II.service;

import com.example.Blogging_Platform_II.api.ApiException;
import com.example.Blogging_Platform_II.api.Requests.PostInput;
import com.example.Blogging_Platform_II.model.Blog.Post;
import com.example.Blogging_Platform_II.model.Blog.PostFilter;
import com.example.Blogging_Platform_II.model.Blog.PostPage;
import com.example.Blogging_Platform_II.model.Blog.Sort;
import com.example.Blogging_Platform_II.repository.PostRepository;
import com.example.Blogging_Platform_II.repository.TagRepository;
import com.example.Blogging_Platform_II.repository.UserRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
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

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public PostPage search(PostFilter filter) {
        int page = filter.page() == null ? 0 : filter.page();
        int size = filter.size() == null ? 10 : filter.size();
        if (page < 0 || size < 1 || size > 100) {
            throw ApiException.badRequest("Use a nonnegative page and a page size between 1 and 100.");
        }
        if (filter.keyword() != null && filter.keyword().length() > 200) {
            throw ApiException.badRequest("Search keywords must be at most 200 characters.");
        }
        if ((filter.authorId() != null && filter.authorId() <= 0) || (filter.tagId() != null && filter.tagId() <= 0)) {
            throw ApiException.badRequest("Author and tag filters must be positive IDs.");
        }
        return posts.search(filter.keyword(), filter.authorId(), filter.tagId(), page, size,
                filter.sort() == null ? Sort.NEWEST : filter.sort());
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public Post get(long id) {
        return posts.find(id).orElseThrow(() -> ApiException.notFound("Post"));
    }

    @Transactional
    public Post create(long authorId, PostInput input) {
        users.findForContent(authorId).orElseThrow(() -> ApiException.notFound("Author"));
        List<Long> selected = visibleTags(input);
        long id = posts.insert(authorId, input.title(), input.body());
        posts.replaceTags(id, selected);
        return get(id);
    }

    @Transactional
    public Post update(long id, PostInput input) {
        lockVisible(id);
        List<Long> selected = visibleTags(input);
        posts.update(id, input.title(), input.body());
        posts.replaceTags(id, selected);
        return get(id);
    }

    @Transactional
    public void archive(long id) {
        lockVisible(id);
        posts.archive(id);
    }

    private void lockVisible(long id) {
        if (!posts.lockVisible(id)) {
            throw ApiException.notFound("Post");
        }
    }

    private List<Long> visibleTags(PostInput input) {
        List<Long> selected = input.tagIds() == null ? List.of() : input.tagIds().stream().distinct().toList();
        if (!tags.allVisible(selected)) {
            throw ApiException.badRequest("Select tags that are available.");
        }
        return selected;
    }
}
