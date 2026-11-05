package com.example.Blogging_Platform_II.model;

import java.util.List;

public record Post(long id, long authorId, String title, String body, String createdAt,
                   String updatedAt, List<Tag> tags) {}
