
package com.example.Blogging_Platform_II.service;

import com.example.Blogging_Platform_II.api.ApiException;
import com.example.Blogging_Platform_II.model.Blog.ArchivedItem;
import com.example.Blogging_Platform_II.model.Blog.Dashboard;
import com.example.Blogging_Platform_II.model.Blog.Kind;
import com.example.Blogging_Platform_II.model.Blog.Sort;
import com.example.Blogging_Platform_II.repository.DiscussionRepository;
import com.example.Blogging_Platform_II.repository.PostRepository;
import com.example.Blogging_Platform_II.repository.TagRepository;
import com.example.Blogging_Platform_II.repository.UserRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminService {
    private final Actors actors;
    private final UserRepository users;
    private final PostRepository posts;
    private final TagRepository tags;
    private final DiscussionRepository discussion;

    public AdminService(Actors actors, UserRepository users, PostRepository posts, TagRepository tags,
                        DiscussionRepository discussion) {
        this.actors = actors;
        this.users = users;
        this.posts = posts;
        this.tags = tags;
        this.discussion = discussion;
    }

    public Dashboard dashboard() {
        actors.current();
        var recent = posts.search(null, null, null, 0, 5, Sort.NEWEST);
        var usage = tags.usage();
        var archived = posts.archivedIds();
        return new Dashboard(users.count(), recent.totalElements(), usage.size(), discussion.countComments(archived),
                discussion.countReviews(archived), recent.items(), usage);
    }

    public List<ArchivedItem> trash(Kind kind) {
        actors.admin();
        return switch (kind) {
            case POST -> posts.archived();
            case TAG -> tags.archived();
            case COMMENT -> discussion.archived().stream().map(comment ->
                    new ArchivedItem(Kind.COMMENT, comment.id(), comment.body(), comment.deletedAt().toString())).toList();
        };
    }

    @Transactional
    public void restore(Kind kind, String id) {
        actors.admin();
        boolean restored = switch (kind) {
            case POST -> posts.restore(numericId(id));
            case TAG -> tags.restore(numericId(id));
            case COMMENT -> restoreComment(id);
        };
        if (!restored) {
            throw ApiException.notFound("Archived item");
        }
    }

    private boolean restoreComment(String id) {
        var comment = discussion.archivedComment(id).orElseThrow(() -> ApiException.notFound("Archived comment"));
        if (!posts.lockVisible(comment.postId())) {
            throw ApiException.conflict("Restore the parent post before restoring this comment.");
        }
        return discussion.restore(comment.postId(), id);
    }

    private long numericId(String id) {
        try {
            return Long.parseLong(id);
        } catch (NumberFormatException exception) {
            throw ApiException.badRequest("Use a numeric post or tag ID.");
        }
    }
}
