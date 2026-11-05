package com.example.Blogging_Platform_II.model;

import java.util.List;

public record Discussion(List<Review> reviews, double averageRating) {}
