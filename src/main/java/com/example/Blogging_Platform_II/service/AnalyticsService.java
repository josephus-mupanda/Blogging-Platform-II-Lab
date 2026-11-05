
package com.example.Blogging_Platform_II.service;

import com.example.Blogging_Platform_II.model.Dashboard;
import com.example.Blogging_Platform_II.repository.DiscussionRepository;
import com.example.Blogging_Platform_II.repository.PostRepository;
import com.example.Blogging_Platform_II.repository.TagRepository;
import com.example.Blogging_Platform_II.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class AnalyticsService {
    private final UserRepository users;
    private final PostRepository posts;
    private final TagRepository tags;
    private final DiscussionRepository discussion;

    public AnalyticsService(UserRepository users, PostRepository posts, TagRepository tags,
                            DiscussionRepository discussion) {
        this.users = users;
        this.posts = posts;
        this.tags = tags;
        this.discussion = discussion;
    }

    public Dashboard dashboard() {
        var archived = posts.archivedIds();
        return new Dashboard(users.count(), posts.countVisible(), tags.countVisible(),
                discussion.countComments(archived), discussion.countReviews(archived));
    }
}
