
package com.example.Blogging_Platform_II.api;

import static com.example.Blogging_Platform_II.api.ApiEnvelope.ok;

import com.example.Blogging_Platform_II.model.Blog.ArchivedItem;
import com.example.Blogging_Platform_II.model.Blog.Dashboard;
import com.example.Blogging_Platform_II.model.Blog.Kind;
import com.example.Blogging_Platform_II.service.AdminService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AdminController {
    private final AdminService admin;

    public AdminController(AdminService admin) {
        this.admin = admin;
    }

    @GetMapping("/dashboard")
    @SecurityRequirement(name = "basicAuth")
    ApiEnvelope<Dashboard> dashboard() {
        return ok("Dashboard calculated.", admin.dashboard());
    }

    @GetMapping("/trash/{kind}")
    @SecurityRequirement(name = "basicAuth")
    ApiEnvelope<List<ArchivedItem>> trash(@PathVariable Kind kind) {
        return ok("Archived items found.", admin.trash(kind));
    }

    @PostMapping(path = "/trash/{kind}/{id}/restore", consumes = MediaType.APPLICATION_JSON_VALUE)
    @SecurityRequirement(name = "basicAuth")
    ApiEnvelope<Void> restore(@PathVariable Kind kind, @PathVariable String id,
                              @RequestBody Map<String, Object> body) {
        if (!body.isEmpty()) {
            throw ApiException.badRequest("Send an empty JSON object to restore this item.");
        }
        admin.restore(kind, id);
        return ok("Item restored.", null);
    }
}
