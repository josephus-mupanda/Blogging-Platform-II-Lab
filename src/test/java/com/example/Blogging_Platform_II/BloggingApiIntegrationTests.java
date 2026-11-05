
package com.example.Blogging_Platform_II;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "RUN_BLOG_INTEGRATION_TESTS", matches = "true")
class BloggingApiIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired MongoTemplate mongo;
    @Autowired ObjectMapper json;

    @Test
    void restAndGraphQlShareDataAndPreserveLegacyRules() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String author = "author-" + suffix;
        String reader = "reader-" + suffix;
        String tag = "spring-" + suffix;
        String title = "Spring migration " + suffix;
        long authorId = data(request(post("/api/users"), user(author)), 201).path("id").asLong();
        String readerQuery = """
                mutation { createUser(input: {username: "%s", email: "%s@example.test", displayName: "Reader"}) { id } }
                """.formatted(reader, reader);
        long readerId = json.readTree(graph(readerQuery).andExpect(jsonPath("$.errors").doesNotExist()).andReturn()
                .getResponse().getContentAsString()).path("data").path("createUser").path("id").asLong();

        mvc.perform(request(post("/api/users"), user(author)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.data.username").exists());
        mvc.perform(get("/api/users")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.username == '" + author + "')]").isNotEmpty());
        mvc.perform(request(put("/api/users/" + readerId),
                Map.of("email", reader + "@example.test", "displayName", "Reader updated")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.displayName").value("Reader updated"));
        long tagId = data(request(post("/api/tags"), Map.of("name", tag.toUpperCase(java.util.Locale.ROOT))), 201)
                .path("id").asLong();
        mvc.perform(request(put("/api/tags/" + tagId), Map.of("name", tag)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.name").value(tag));
        graph("mutation { createTag(input: {name: \"" + tag + "\"}) { id } }")
                .andExpect(jsonPath("$.errors[0].extensions.code").value("CONFLICT"));

        var postBody = Map.of("title", title, "body", "Read about JDBC and Spring.", "tagIds", new long[]{tagId});
        String authorParam = String.valueOf(authorId);
        mvc.perform(request(post("/api/posts"), postBody)).andExpect(status().isBadRequest());
        mvc.perform(request(post("/api/posts").param("authorId", Long.toString(Long.MAX_VALUE)), postBody))
                .andExpect(status().isNotFound());
        mvc.perform(request(post("/api/posts").param("authorId", authorParam), Map.of("title", "", "body", "text")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.data.title").exists());
        long postId = data(request(post("/api/posts").param("authorId", authorParam), postBody), 201)
                .path("id").asLong();
        mvc.perform(request(post("/api/posts").param("authorId", authorParam),
                Map.of("title", title.toUpperCase(java.util.Locale.ROOT), "body", "Duplicate")))
                .andExpect(status().isConflict());
        mvc.perform(request(put("/api/posts/" + postId), postBody)).andExpect(status().isOk());
        mvc.perform(get("/api/posts").param("keyword", suffix).param("authorId", String.valueOf(authorId))
                .param("tagId", String.valueOf(tagId)).param("sort", "TITLE_ASC").param("size", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.items[0].title").value(title))
                .andExpect(jsonPath("$.data.items[0].tags[0].name").value(tag));
        mvc.perform(get("/api/posts").param("keyword", "%")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[?(@.id == " + postId + ")]").isEmpty());
        mvc.perform(get("/api/posts").param("size", "101")).andExpect(status().isBadRequest());

        graph("query { posts(filter: {tagId: \"" + tagId + "\"}) { totalElements items { id title } } }")
                .andExpect(jsonPath("$.data.posts.totalElements").value(1))
                .andExpect(jsonPath("$.data.posts.items[0].id").value(String.valueOf(postId)));
        graph("""
                mutation { addComment(postId: "%d", authorId: "%d", input: {body: "GraphQL comment"}) { id authorId } }
                """.formatted(postId, readerId)).andExpect(jsonPath("$.errors").doesNotExist())
                .andExpect(jsonPath("$.data.addComment.authorId").value(Long.toString(readerId)));
        graph("""
                mutation { addReview(postId: "%d", reviewerId: "%d", input: {rating: 6}) { id } }
                """.formatted(postId, readerId))
                .andExpect(jsonPath("$.errors[0].extensions.code").value("BAD_REQUEST"));
        graph("query { post(id: \"invalid\") { id } }")
                .andExpect(jsonPath("$.errors[0].extensions.code").value("BAD_REQUEST"));

        String path = "/api/posts/" + postId;
        String commentId = data(get(path + "/comments"), 200).get(0).path("id").asText();
        mvc.perform(request(put(path + "/comments/" + commentId), Map.of("body", "Edited")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.body").value("Edited"));
        mvc.perform(delete(path + "/comments/" + commentId)).andExpect(status().isOk());
        mongo.getCollection("comments").insertOne(new Document("postId", postId).append("authorId", authorId)
                .append("authorName", "Legacy author").append("body", "Legacy comment")
                .append("_class", "com.example.Blogging_Platform_II.model.Blog$Comment")
                .append("createdAt", java.util.Date.from(java.time.Instant.now())));
        mvc.perform(get(path + "/comments")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].body").value("Legacy comment"));

        String reviewerParam = Long.toString(readerId);
        mvc.perform(request(post(path + "/reviews").param("reviewerId", reviewerParam), Map.of("rating", 4)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.data.reviewerId").value(readerId));
        mvc.perform(request(post(path + "/reviews").param("reviewerId", reviewerParam), Map.of("rating", 5)))
                .andExpect(status().isConflict());
        mvc.perform(get(path + "/reviews")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.averageRating").value(4.0));

        mvc.perform(delete("/api/tags/" + tagId)).andExpect(status().isOk());
        mvc.perform(get("/api/tags/" + tagId)).andExpect(status().isNotFound());
        mvc.perform(get(path)).andExpect(status().isOk()).andExpect(jsonPath("$.data.tags").isEmpty());

        mvc.perform(get("/api/users/" + readerId)).andExpect(status().isOk());
        mvc.perform(delete("/api/users/" + readerId)).andExpect(status().isOk());
        mvc.perform(get("/api/users/" + readerId)).andExpect(status().isNotFound());
        mvc.perform(request(post(path + "/comments").param("authorId", reviewerParam), Map.of("body", "Hello")))
                .andExpect(status().isNotFound());
        graph("mutation { deleteUser(id: \"" + authorId + "\") }")
                .andExpect(jsonPath("$.data.deleteUser").value(true));
        mvc.perform(get(path)).andExpect(status().isOk()).andExpect(jsonPath("$.data.authorId").value(authorId));

        var countsBeforeDelete = data(get("/api/dashboard"), 200);
        mvc.perform(delete(path)).andExpect(status().isOk());
        mvc.perform(get(path)).andExpect(status().isNotFound());
        mvc.perform(get(path + "/comments")).andExpect(status().isNotFound());
        mvc.perform(get(path + "/reviews")).andExpect(status().isNotFound());
        graph("query { dashboard { posts comments reviews } }")
                .andExpect(jsonPath("$.data.dashboard.posts").value(countsBeforeDelete.path("posts").asInt() - 1))
                .andExpect(jsonPath("$.data.dashboard.comments").value(countsBeforeDelete.path("comments").asInt() - 1))
                .andExpect(jsonPath("$.data.dashboard.reviews").value(countsBeforeDelete.path("reviews").asInt() - 1));

        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/posts']").exists())
                .andExpect(jsonPath("$.components.securitySchemes").doesNotExist());
        mvc.perform(get("/graphiql")).andExpect(status().isTemporaryRedirect());
        mvc.perform(get("/graphiql").param("path", "/graphql")).andExpect(status().isOk());

    }

    private Map<String, String> user(String username) {
        return Map.of("username", username, "email", username + "@example.test", "displayName", username);
    }

    private ResultActions graph(String query) throws Exception {
        return mvc.perform(request(post("/graphql"), Map.of("query", query))).andExpect(status().isOk());
    }

    private JsonNode data(MockHttpServletRequestBuilder request, int expectedStatus) throws Exception {
        return json.readTree(mvc.perform(request).andExpect(status().is(expectedStatus)).andReturn()
                .getResponse().getContentAsString()).path("data");
    }

    private MockHttpServletRequestBuilder request(MockHttpServletRequestBuilder request, Object body)
            throws Exception {
        return request.contentType("application/json").content(json.writeValueAsBytes(body));
    }
}
