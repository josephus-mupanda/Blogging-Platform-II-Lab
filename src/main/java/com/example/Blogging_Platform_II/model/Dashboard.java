package com.example.Blogging_Platform_II.model;

public class Dashboard {
    private long users;
    private long posts;
    private long tags;
    private long comments;
    private long reviews;

    public Dashboard() {
    }

    public Dashboard(long users, long posts, long tags, long comments, long reviews) {
        this.users = users;
        this.posts = posts;
        this.tags = tags;
        this.comments = comments;
        this.reviews = reviews;
    }

    public long getUsers() {
        return users;
    }

    public void setUsers(long users) {
        this.users = users;
    }

    public long getPosts() {
        return posts;
    }

    public void setPosts(long posts) {
        this.posts = posts;
    }

    public long getTags() {
        return tags;
    }

    public void setTags(long tags) {
        this.tags = tags;
    }

    public long getComments() {
        return comments;
    }

    public void setComments(long comments) {
        this.comments = comments;
    }

    public long getReviews() {
        return reviews;
    }

    public void setReviews(long reviews) {
        this.reviews = reviews;
    }
}
