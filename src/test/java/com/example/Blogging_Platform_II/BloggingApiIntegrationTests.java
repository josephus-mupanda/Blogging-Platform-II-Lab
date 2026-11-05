
package com.example.Blogging_Platform_II;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "RUN_BLOG_INTEGRATION_TESTS", matches = "true")
class BloggingApiIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired JdbcClient jdbc;
    @Autowired MongoTemplate mongo;
    @Autowired ObjectMapper json;

    @Test
    void restAndGraphQlShareDataAndPreserveLegacyRules() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String admin = "admin-" + suffix;
        String author = "author-" + suffix;
        String reader = "reader-" + suffix;
        String tag = "spring-" + suffix;
        String title = "Spring migration " + suffix;
        jdbc.sql("""
                INSERT INTO users (username, email, password_hash, display_name, role)
                VALUES (:name, :email, :hash, 'Legacy admin', 'ADMIN')
                """).param("name", admin).param("email", admin + "@example.test")
                .param("hash", "bcb15f821479b4d5772bd0ca866c00ad5f926e3580720659cc80d39c9d09802a")
                .update();

        JsonNode authorAccount = register(author, "AUTHOR");
        JsonNode readerAccount = register(reader, "READER");
        long authorId = authorAccount.path("id").asLong();
        long readerId = readerAccount.path("id").asLong();

        mvc.perform(request(post("/api/users"), null, registration(author, "AUTHOR")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.data.username").exists());
        mvc.perform(get("/api/users")).andExpect(status().isUnauthorized());
        mvc.perform(auth(get("/api/users"), admin)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].passwordHash").doesNotExist());
        mvc.perform(request(put("/api/users/" + readerId), reader,
                Map.of("email", reader + "@example.test", "displayName", "Reader updated")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.displayName").value("Reader updated"));
        mvc.perform(request(post("/api/tags"), author, Map.of("name", tag))).andExpect(status().isForbidden());
        long tagId = data(request(post("/api/tags"), admin, Map.of("name", tag.toUpperCase(java.util.Locale.ROOT))), 201)
                .path("id").asLong();
        mvc.perform(request(put("/api/tags/" + tagId), admin, Map.of("name", tag)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.name").value(tag));

        var postBody = Map.of("title", title, "body", "Read about JDBC and Spring.", "tagIds", new long[]{tagId});
        mvc.perform(request(post("/api/posts"), reader, postBody)).andExpect(status().isForbidden());
        mvc.perform(request(post("/api/posts"), author, Map.of("title", "", "body", "text")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.data.title").exists());
        long postId = data(request(post("/api/posts"), author, postBody), 201).path("id").asLong();
        mvc.perform(request(post("/api/posts"), author,
                Map.of("title", title.toUpperCase(java.util.Locale.ROOT), "body", "Duplicate")))
                .andExpect(status().isConflict());
        mvc.perform(request(put("/api/posts/" + postId), author, postBody)).andExpect(status().isOk());
        mvc.perform(get("/api/posts").param("keyword", suffix).param("authorId", String.valueOf(authorId))
                .param("tagId", String.valueOf(tagId)).param("sort", "TITLE_ASC").param("size", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.items[0].title").value(title))
                .andExpect(jsonPath("$.data.items[0].tags[0].name").value(tag));
        mvc.perform(get("/api/posts").param("keyword", "%")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[?(@.id == " + postId + ")]").isEmpty());
        mvc.perform(get("/api/posts").param("size", "101")).andExpect(status().isBadRequest());

        graph(null, "query { posts(filter: {tagId: \"" + tagId + "\"}) { totalElements items { id title } } }")
                .andExpect(jsonPath("$.data.posts.totalElements").value(1))
                .andExpect(jsonPath("$.data.posts.items[0].id").value(String.valueOf(postId)));
        graph(reader, "mutation { addComment(postId: \"" + postId + "\", input: {body: \"GraphQL comment\"})"
                + " { id createdAt } }").andExpect(jsonPath("$.errors").doesNotExist())
                .andExpect(jsonPath("$.data.addComment.createdAt").isString());
        graph(reader, "mutation { addReview(postId: \"" + postId + "\", input: {rating: 6}) { id } }")
                .andExpect(jsonPath("$.errors[0].extensions.code").value("BAD_REQUEST"));
        graph(null, "mutation { createTag(input: {name: \"unauthorized\"}) { id } }")
                .andExpect(jsonPath("$.errors[0].extensions.code").value("UNAUTHORIZED"));
        graph(null, "query { post(id: \"invalid\") { id } }")
                .andExpect(jsonPath("$.errors[0].extensions.code").value("BAD_REQUEST"));

        String path = "/api/posts/" + postId;
        String commentId = data(get(path + "/comments"), 200).get(0).path("id").asText();
        mvc.perform(request(put(path + "/comments/" + commentId), author, Map.of("body", "Not mine")))
                .andExpect(status().isForbidden());
        mvc.perform(request(put(path + "/comments/" + commentId), reader, Map.of("body", "Edited")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.body").value("Edited"));
        mvc.perform(auth(delete(path + "/comments/" + commentId), reader)).andExpect(status().isOk());
        mongo.getCollection("comments").insertOne(new Document("postId", postId).append("authorId", authorId)
                .append("authorName", "Legacy author").append("body", "Legacy comment")
                .append("createdAt", java.util.Date.from(java.time.Instant.now())));
        mvc.perform(get(path + "/comments")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].body").value("Legacy comment"));

        mvc.perform(request(post(path + "/reviews"), reader, Map.of("rating", 4))).andExpect(status().isCreated());
        mvc.perform(request(post(path + "/reviews"), reader, Map.of("rating", 5))).andExpect(status().isConflict());
        mvc.perform(get(path + "/reviews")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.averageRating").value(4.0));

        mvc.perform(auth(delete(path), author)).andExpect(status().isOk());
        mvc.perform(get(path + "/comments")).andExpect(status().isNotFound());
        mvc.perform(get(path + "/reviews")).andExpect(status().isNotFound());
        mvc.perform(request(post("/api/trash/COMMENT/" + commentId + "/restore"), admin, Map.of()))
                .andExpect(status().isConflict());
        mvc.perform(request(post("/api/trash/POST/" + postId + "/restore"), admin, Map.of()))
                .andExpect(status().isOk());
        mvc.perform(request(post("/api/trash/COMMENT/" + commentId + "/restore"), admin, Map.of()))
                .andExpect(status().isOk());
        mvc.perform(get(path + "/comments")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));
        mvc.perform(auth(delete("/api/tags/" + tagId), admin)).andExpect(status().isOk());
        mvc.perform(get(path)).andExpect(status().isOk()).andExpect(jsonPath("$.data.tags").isEmpty());
        mvc.perform(request(post("/api/trash/TAG/" + tagId + "/restore"), admin, Map.of()))
                .andExpect(status().isOk());
        mvc.perform(auth(get("/api/dashboard"), admin)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tagUsage").isArray());

        mvc.perform(auth(delete("/api/users/" + readerId), admin)).andExpect(status().isOk());
        mvc.perform(auth(get("/api/users/me"), reader)).andExpect(status().isUnauthorized());
        mvc.perform(request(patch("/api/users/" + readerId + "/status"), admin, Map.of("active", true)))
                .andExpect(status().isOk());
        mvc.perform(auth(get("/api/users/me"), reader)).andExpect(status().isOk());
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/posts']").exists());
        mvc.perform(get("/graphiql")).andExpect(status().isTemporaryRedirect());
        mvc.perform(get("/graphiql").param("path", "/graphql")).andExpect(status().isOk());
        mvc.perform(auth(get("/actuator/metrics/blog.service.duration"), admin)).andExpect(status().isOk())
                .andExpect(jsonPath("$.measurements").isArray());

        assertThat(authorAccount.has("email")).isFalse();
    }

    private Map<String, String> registration(String user, String role) {
        return Map.of("username", user, "email", user + "@example.test", "password", "111111",
                "displayName", user, "role", role);
    }

    private JsonNode register(String user, String role) throws Exception {
        return data(request(post("/api/users"), null, registration(user, role)), 201);
    }

    private org.springframework.test.web.servlet.ResultActions graph(String user, String query) throws Exception {
        return mvc.perform(request(post("/graphql"), user, Map.of("query", query))).andExpect(status().isOk());
    }

    private JsonNode data(MockHttpServletRequestBuilder request, int expectedStatus) throws Exception {
        return json.readTree(mvc.perform(request).andExpect(status().is(expectedStatus)).andReturn()
                .getResponse().getContentAsString()).path("data");
    }

    private MockHttpServletRequestBuilder request(MockHttpServletRequestBuilder request, String user, Object body)
            throws Exception {
        return auth(request, user).contentType("application/json").content(json.writeValueAsBytes(body));
    }

    private MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder request, String user) {
        if (user != null) {
            String token = Base64.getEncoder().encodeToString((user + ":111111").getBytes(StandardCharsets.UTF_8));
            request.header("Authorization", "Basic " + token);
        }
        return request;
    }
}
