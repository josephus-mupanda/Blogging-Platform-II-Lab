package com.example.Blogging_Platform_II.model;

import java.util.List;

public record PostPage(List<Post> items, int page, int size, long totalElements, long totalPages) {}
