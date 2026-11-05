
package com.example.Blogging_Platform_II.graphql;

import com.example.Blogging_Platform_II.api.Requests.UserCreate;
import com.example.Blogging_Platform_II.api.Requests.UserUpdate;
import com.example.Blogging_Platform_II.api.Requests.PostInput;
import com.example.Blogging_Platform_II.api.Requests.TagInput;
import com.example.Blogging_Platform_II.api.Requests.CommentInput;
import com.example.Blogging_Platform_II.api.Requests.ReviewInput;
import com.example.Blogging_Platform_II.model.Blog.User;
import com.example.Blogging_Platform_II.model.Blog.Post;
import com.example.Blogging_Platform_II.model.Blog.PostPage;
import com.example.Blogging_Platform_II.model.Blog.PostFilter;
import com.example.Blogging_Platform_II.model.Blog.Tag;
import com.example.Blogging_Platform_II.model.Blog.Comment;
import com.example.Blogging_Platform_II.model.Blog.Discussion;
import com.example.Blogging_Platform_II.model.Blog.Dashboard;
import com.example.Blogging_Platform_II.model.Blog.Review;
import com.example.Blogging_Platform_II.service.UserService;
import com.example.Blogging_Platform_II.service.PostService;
import com.example.Blogging_Platform_II.service.TagService;
import com.example.Blogging_Platform_II.service.DiscussionService;
import com.example.Blogging_Platform_II.service.AnalyticsService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

@Controller
public class BlogGraphQlController {
    private final UserService users;
    private final PostService posts;
    private final TagService tags;
    private final DiscussionService discussion;
    private final AnalyticsService analytics;

    public BlogGraphQlController(UserService users, PostService posts, TagService tags,
                                 DiscussionService discussion, AnalyticsService analytics) {
        this.users = users;
        this.posts = posts;
        this.tags = tags;
        this.discussion = discussion;
        this.analytics = analytics;
    }

    @QueryMapping
    public PostPage posts(@Argument PostFilter filter) {
        return posts.search(filter == null ? new PostFilter(null, null, null, null, null, null) : filter);
    }

    @QueryMapping
    public Post post(@Argument long id) {
        return posts.get(id);
    }

    @QueryMapping
    public List<User> users() {
        return users.list();
    }

    @QueryMapping
    public User user(@Argument long id) {
        return users.get(id);
    }

    @QueryMapping
    public List<Tag> tags() {
        return tags.list();
    }

    @QueryMapping
    public Tag tag(@Argument long id) {
        return tags.get(id);
    }

    @QueryMapping
    public List<Comment> comments(@Argument long postId) {
        return discussion.comments(postId);
    }

    @QueryMapping
    public Comment comment(@Argument long postId, @Argument String id) {
        return discussion.comment(postId, id);
    }

    @QueryMapping
    public Discussion reviews(@Argument long postId) {
        return discussion.reviews(postId);
    }

    @QueryMapping
    public Dashboard dashboard() {
        return analytics.dashboard();
    }

    @MutationMapping
    public User createUser(@Argument @Valid UserCreate input) {
        return users.create(input);
    }

    @MutationMapping
    public User updateUser(@Argument long id, @Argument @Valid UserUpdate input) {
        return users.update(id, input);
    }

    @MutationMapping
    public boolean deleteUser(@Argument long id) {
        users.delete(id);
        return true;
    }

    @MutationMapping
    public Post createPost(@Argument long authorId, @Argument @Valid PostInput input) {
        return posts.create(authorId, input);
    }

    @MutationMapping
    public Post updatePost(@Argument long id, @Argument @Valid PostInput input) {
        return posts.update(id, input);
    }

    @MutationMapping
    public boolean deletePost(@Argument long id) {
        posts.archive(id);
        return true;
    }

    @MutationMapping
    public Tag createTag(@Argument @Valid TagInput input) {
        return tags.create(input);
    }

    @MutationMapping
    public Tag updateTag(@Argument long id, @Argument @Valid TagInput input) {
        return tags.update(id, input);
    }

    @MutationMapping
    public boolean deleteTag(@Argument long id) {
        tags.archive(id);
        return true;
    }

    @MutationMapping
    public Comment addComment(@Argument long postId, @Argument long authorId, @Argument @Valid CommentInput input) {
        return discussion.addComment(postId, authorId, input);
    }

    @MutationMapping
    public Comment updateComment(@Argument long postId, @Argument String id, @Argument @Valid CommentInput input) {
        return discussion.editComment(postId, id, input);
    }

    @MutationMapping
    public boolean deleteComment(@Argument long postId, @Argument String id) {
        discussion.archiveComment(postId, id);
        return true;
    }

    @MutationMapping
    public Review addReview(@Argument long postId, @Argument long reviewerId, @Argument @Valid ReviewInput input) {
        return discussion.addReview(postId, reviewerId, input);
    }

}
