package com.example.Blogging_Platform_II.model;

public record PostFilter(String keyword, Long authorId, Long tagId, Integer page, Integer size, Sort sort) {}
